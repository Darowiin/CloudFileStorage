package ru.service;

import io.minio.GetObjectArgs;
import io.minio.ListObjectsArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectsArgs;
import io.minio.Result;
import io.minio.messages.DeleteObject;
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
public class DirectoryService {
    private final MinioClient minioClient;
    private final String bucketName = "user-files";

    @SneakyThrows
    public ResourceResponse createDirectory(int userId, String path) {
        PathUtils.checkPath(path);

        String normalizedPath = path.endsWith("/") ? path.substring(0, path.length() - 1) : path;

        String directoryName = PathUtils.getFileName(normalizedPath);
        String parentPath = PathUtils.getParentPath(normalizedPath);

        String pathWithSlash = normalizedPath + "/";

        String fullPath = PathUtils.getFullPath(userId, path);

        if (!parentPath.equals("/")) {
            String fullParentPath = PathUtils.getFullPath(userId, parentPath);
            if (!directoryExists(fullParentPath)) {
                throw new ResourceNotFoundException("Parent directory doesn't exists: " + parentPath);
            }
        }
        if (directoryExists(fullPath)) {
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
        List<DeleteObject> deleteObjects = new ArrayList<>();
        String fullPath = PathUtils.getFullPath(userId, path);
        Iterable<Result<Item>> results = minioClient.listObjects(
                ListObjectsArgs.builder().bucket(bucketName).prefix(fullPath).build()
        );

        for (Result<Item> result : results) {
            Item item = result.get();
            String objectName = item.objectName();
            deleteObjects.add(new DeleteObject(objectName));

        }
        minioClient.removeObjects(
                RemoveObjectsArgs.builder().bucket(bucketName).objects(deleteObjects).build());
    }

    @SneakyThrows
    private boolean directoryExists(String path) {
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

        path = path.endsWith("/") ? path : path + "/";

        for (Result<Item> result : items) {
            Item item = result.get();
            String objectName = item.objectName();

            if (objectName.equals(fullPath)) {
                continue;
            }

            String name = PathUtils.getFileName(objectName);

            if (item.isDir()) {
                resources.add(new ResourceResponse(path,
                        name,
                        null,
                        ResourceType.DIRECTORY));
            } else {
                try (InputStream input = minioClient.getObject(
                        GetObjectArgs.builder().bucket(bucketName).object(objectName).build())
                ) {
                    resources.add(new ResourceResponse(path,
                            name,
                            item.size(),
                            ResourceType.FILE));
                }
            }
        }
        return resources;
    }
}
