package ru.service;

import io.minio.CopyObjectArgs;
import io.minio.CopySource;
import io.minio.GetObjectArgs;
import io.minio.ListObjectsArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.Result;
import io.minio.errors.ErrorResponseException;
import io.minio.messages.Item;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.entity.ResourceType;
import ru.dto.ResourceResponse;
import ru.exception.ResourceAlreadyExistsException;
import ru.exception.ResourceNotFoundException;
import ru.util.PathUtils;

import java.io.ByteArrayInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static java.nio.file.Files.createTempFile;

@Service
@RequiredArgsConstructor(onConstructor_ = @Autowired)
@Slf4j
public class DirectoryService {
    private final MinioClient minioClient;
    private final String bucketName = "user-files";

    @SneakyThrows
    public ResourceResponse createDirectory(int userId, String path) {
        PathUtils.checkPath(path);

        if (path == null || path.isEmpty()) {
            String fullUserDir = PathUtils.getFullPath(userId, "");
            if (isDirectoryExists(fullUserDir)) {
                throw new ResourceAlreadyExistsException("Directory already exists: " + path);
            }
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(fullUserDir)
                            .stream(new ByteArrayInputStream(new byte[]{}), 0, -1)
                            .contentType("application/x-directory")
                            .build());
            return new ResourceResponse("", "", null, ResourceType.DIRECTORY);
        }


        String normalizedPath = path.endsWith("/") ? path.substring(0, path.length() - 1) : path;

        String directoryName = PathUtils.getFileName(normalizedPath) + "/";
        String parentPath = PathUtils.getParentPath(normalizedPath);

        String pathWithSlash = normalizedPath + "/";

        String fullPath = PathUtils.getFullPath(userId, pathWithSlash);

        if (!parentPath.equals("/")) {
            String fullParentPath = PathUtils.getFullPath(userId, parentPath);
            if (!isDirectoryExists(fullParentPath)) {
                throw new ResourceNotFoundException("Parent directory doesn't exists: " + parentPath);
            }
        }
        if (isDirectoryExists(fullPath)) {
            throw new ResourceAlreadyExistsException("Directory already exists: " + path);
        }

        minioClient.putObject(
                PutObjectArgs.builder()
                        .bucket(bucketName)
                        .object(fullPath)
                        .stream(new ByteArrayInputStream(new byte[]{}), 0, -1)
                        .contentType("application/x-directory")
                        .build());

        return new ResourceResponse(pathWithSlash,
                directoryName,
                null,
                ResourceType.DIRECTORY);
    }

    @SneakyThrows
    public void deleteDirectory(int userId, String path) {

        String fullPath = PathUtils.getFullPath(userId, path);
        if (!fullPath.endsWith("/")) fullPath = fullPath + "/";

        if (!isDirectoryExists(fullPath)) {
            throw new ResourceNotFoundException("Directory doesn't exists: " + fullPath);
        }

        List<String> toDelete = new ArrayList<>();

        Iterable<Result<Item>> results = minioClient.listObjects(
                ListObjectsArgs.builder()
                        .bucket(bucketName)
                        .prefix(fullPath)
                        .recursive(true)
                        .build()
        );

        for (Result<Item> r : results) {
            Item item = r.get();
            toDelete.add(item.objectName());
        }

        for (String objectName : toDelete) {
            try {
                minioClient.removeObject(
                        RemoveObjectArgs.builder()
                                .bucket(bucketName)
                                .object(objectName)
                                .build()
                );
                log.info("User: {}, Deleted object: {}", userId, objectName);
            } catch (ErrorResponseException ere) {
                String code = ere.errorResponse() != null ? ere.errorResponse().code() : null;
                if ("NoSuchKey".equals(code) || "NoSuchBucket".equals(code)) {
                    log.info("User: {}, Object {} was not found when deleting (ignored).", userId, objectName);
                } else {
                    log.warn("User: {}, Error removing object {}: {}", userId, objectName, ere.toString());
                }
            } catch (Exception e) {
                log.warn("User: {}, Failed to remove object {}: {}", userId, objectName, e.toString());
            }
        }

        try {
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(bucketName)
                            .object(fullPath)
                            .build()
            );
        } catch (ErrorResponseException ere) {
            String code = ere.errorResponse() != null ? ere.errorResponse().code() : null;
            if ("NoSuchKey".equals(code) || "NoSuchBucket".equals(code)) {
                log.info("User: {}, Directory marker {} not found (ignored).", userId, fullPath);
            } else {
                log.warn("User: {}, Error removing directory marker {}: {}", userId, fullPath, ere.toString());
            }
        } catch (Exception e) {
            log.warn("User: {}, Failed to remove directory marker {}: {}", userId, fullPath, e.toString());
        }
    }

    @SneakyThrows
    private boolean isDirectoryExists(String path) {
        try {
            Iterable<Result<Item>> results = minioClient.listObjects(
                    ListObjectsArgs.builder()
                            .bucket(bucketName)
                            .prefix(path)
                            .maxKeys(1)
                            .build());
            return results.iterator().hasNext();
        } catch (Exception e) {
            return false;
        }
    }

    @SneakyThrows
    public ResourceResponse moveDirectory(int userId, String from, String to) {
        String fromFull = PathUtils.getFullPath(userId, from);
        String toFull = PathUtils.getFullPath(userId, to);

        if (!isDirectoryExists(fromFull)) {
            throw new ResourceNotFoundException("Source directory does not exist: " + from);
        }

        if (isDirectoryExists(toFull)) {
            throw new ResourceAlreadyExistsException("Destination directory already exists: " + to);
        }

        boolean movedAny = false;
        boolean wasEmpty = true;

        Iterable<Result<Item>> results = minioClient.listObjects(
                ListObjectsArgs.builder()
                        .bucket(bucketName)
                        .prefix(fromFull)
                        .recursive(true)
                        .build()
        );

        for (Result<Item> r : results) {
            Item item = r.get();
            String src = item.objectName();

            if (src.equals(fromFull)) {
                continue;
            }

            wasEmpty = false;
            String dst = toFull + src.substring(fromFull.length());

            minioClient.copyObject(
                    CopyObjectArgs.builder()
                            .bucket(bucketName)
                            .object(dst)
                            .source(CopySource.builder().bucket(bucketName).object(src).build())
                            .build()
            );

            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(bucketName)
                            .object(src)
                            .build()
            );

            movedAny = true;
        }

        try {
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(bucketName)
                            .object(fromFull)
                            .build()
            );
        } catch (Exception ignored) {}

        if (!movedAny && wasEmpty) {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(toFull)
                            .stream(new ByteArrayInputStream(new byte[]{}), 0, -1)
                            .contentType("application/x-directory")
                            .build()
            );
        }

        return new ResourceResponse(
                PathUtils.getParentPath(to),
                PathUtils.getFileName(to),
                null,
                ResourceType.DIRECTORY
        );
    }

    @SneakyThrows
    public byte[] downloadDirectory(int userId, String path) {
        String fullPath = PathUtils.getFullPath(userId, path);
        Path tempFile = null;

        try {
            tempFile = createTempFile("download_", ".zip");
            boolean hasFiles = false;

            try (FileOutputStream fileOutputStream = new FileOutputStream(tempFile.toFile());
                 ZipOutputStream zipOutputStream = new ZipOutputStream(fileOutputStream)) {

                Iterable<Result<Item>> items = minioClient.listObjects(
                        ListObjectsArgs.builder()
                                .bucket(bucketName)
                                .prefix(fullPath)
                                .recursive(true)
                                .build());

                for (Result<Item> result : items) {
                    Item item = result.get();
                    String objectName = item.objectName();

                    if (item.isDir() || objectName.equals(fullPath)) {
                        continue;
                    }

                    hasFiles = true;

                    String relativePath = objectName.substring(fullPath.length());
                    if (relativePath.startsWith("/")) {
                        relativePath = relativePath.substring(1);
                    }

                    try (InputStream input = minioClient.getObject(
                            GetObjectArgs.builder().bucket(bucketName).object(objectName).build())
                    ) {
                        zipOutputStream.putNextEntry(new ZipEntry(relativePath));
                        input.transferTo(zipOutputStream);
                        zipOutputStream.closeEntry();
                    }
                }

                if (!hasFiles) {
                    throw new ResourceNotFoundException("The folder is empty or doesn't exists: " + path);
                }

                zipOutputStream.flush();
            }
            return java.nio.file.Files.readAllBytes(tempFile);
        } finally {
            if (tempFile != null) {
                try {
                    java.nio.file.Files.deleteIfExists(tempFile);
                } catch (Exception ignored) {}
            }
        }
    }

    @SneakyThrows
    public List<ResourceResponse> getDirectoryFilesInfo(int userId, String path) {
        String fullPath = PathUtils.getFullPath(userId, path);

        PathUtils.checkPath(fullPath);

        if (!fullPath.endsWith("/")) {
            fullPath = fullPath + "/";
        }

        Iterable<Result<Item>> items = minioClient.listObjects(
        ListObjectsArgs.builder()
                .bucket(bucketName)
                .prefix(fullPath)
                .build());

        List<ResourceResponse> resources = new ArrayList<>();

        path = !path.endsWith("/") && !path.isEmpty() ? path + "/" : path;

        for (Result<Item> result : items) {
            Item item = result.get();
            String objectName = item.objectName();

            if (objectName.equals(fullPath)) {
                continue;
            }

            String name = PathUtils.getFileName(objectName);

            if (item.isDir()) {
                resources.add(new ResourceResponse(path,
                        name + "/",
                        null,
                        ResourceType.DIRECTORY));
            } else {
                resources.add(new ResourceResponse(path,
                        name,
                        item.size(),
                        ResourceType.FILE));
            }
        }
        return resources;
    }
}
