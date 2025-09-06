package ru.service;

import io.minio.GetObjectArgs;
import io.minio.ListObjectsArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.Result;
import io.minio.SnowballObject;
import io.minio.StatObjectArgs;
import io.minio.UploadSnowballObjectsArgs;
import io.minio.messages.Item;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import ru.exception.InvalidResourcePathException;
import ru.exception.ResourceAlreadyExistsException;
import ru.exception.ResourceNotFoundException;
import ru.entity.ResourceType;
import ru.dto.ResourceResponse;
import ru.util.PathUtils;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;


@Service
@RequiredArgsConstructor(onConstructor_ = @Autowired)
public class ResourceService {
    private final MinioClient minioClient;
    private final String bucketName = "user-files";

    @SneakyThrows
    public ResourceResponse getFileInfo(int userId, String path) {
        PathUtils.checkPath(path);

        String fullPath = PathUtils.getFullPath(userId, path);

        try {
            var response = minioClient.statObject(
                    StatObjectArgs.builder()
                            .bucket(bucketName)
                            .object(fullPath)
                            .build());
            return new ResourceResponse(PathUtils.getParentPath(path), PathUtils.getFileName(path), response.size(), ResourceType.FILE);
        } catch (Exception ex) {
            throw new ResourceNotFoundException("The resource was not found on: " + path);
        }
    }

    @SneakyThrows
    public void delete(int userId, String path) {
        getFileInfo(userId, path);

        String fullPath = PathUtils.getFullPath(userId, path);
        minioClient.removeObject(
                RemoveObjectArgs.builder()
                        .bucket(bucketName)
                        .object(fullPath)
                        .build());
    }

    @SneakyThrows
    public ResourceResponse move(int userId, String from, String to) {
        String fullFromPath = PathUtils.getFullPath(userId, from);
        String fullToPath = PathUtils.getFullPath(userId, to);

        ResourceResponse sourceFile;
        try {
            sourceFile = getFileInfo(userId, from);
        } catch (ResourceNotFoundException e) {
            throw new ResourceNotFoundException("The source file does not exist: " + from);
        }

        try  {
            getFileInfo(userId, to);
            throw new ResourceAlreadyExistsException("The file already exists by path: " + to);

        } catch (ResourceNotFoundException ignored) {}

        try (InputStream inputStream = minioClient.getObject(
                GetObjectArgs.builder()
                        .bucket(bucketName)
                        .object(fullFromPath)
                        .build())) {

            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(fullToPath)
                            .stream(inputStream, sourceFile.size(), -1)
                            .build());

            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(bucketName)
                            .object(fullFromPath)
                            .build());

            return new ResourceResponse(
                    PathUtils.getParentPath(to),
                    PathUtils.getFileName(to),
                    sourceFile.size(),
                    ResourceType.FILE
            );
        } catch (Exception e) {
            throw new RuntimeException("Can't move file from " + from + " to " + to + ": " + e.getMessage(), e);
        }
    }

    @SneakyThrows
    public List<ResourceResponse> search(int userId, String query) {
        if (query == null || query.isBlank()) {
            throw new InvalidResourcePathException("The query can't be null or empty");
        }

        if (query.contains("..") || query.contains("//")) {
            throw new InvalidResourcePathException("The query contains invalid characters: " + query);
        }

        String userDirectory = PathUtils.getUserDirectory(userId);
        Iterable<Result<Item>> items;
        try {
            items = minioClient.listObjects(
                    ListObjectsArgs.builder()
                            .bucket(bucketName)
                            .prefix(userDirectory)
                            .recursive(true)
                            .build());
        } catch (Exception ignored) {
            return List.of();
        }

        Set<String> seenDirs = new HashSet<>();
        List<ResourceResponse> resources = new ArrayList<>();

        for (Result<Item> result : items) {
            Item item = result.get();
            String objectName = item.objectName();

            String relativePath = objectName.replaceFirst("^" + Pattern.quote(userDirectory), "");
            if (relativePath.isEmpty()) continue;

            String[] parts = relativePath.split("/");

            StringBuilder prefix = new StringBuilder();
            for (int i = 0; i < parts.length - 1; i++) {
                prefix.append(parts[i]).append("/");
                String dirPath = prefix.toString();

                if (dirPath.contains(query) && seenDirs.add(dirPath)) {
                    String parent = PathUtils.getParentPath(dirPath);
                    String name = PathUtils.getFileName(dirPath);

                    resources.add(new ResourceResponse(
                            parent,
                            name + "/",
                            null,
                            ResourceType.DIRECTORY
                    ));
                }
            }

            String name = PathUtils.getFileName(relativePath);
            if (name.contains(query)) {
                boolean isDir = item.isDir() || objectName.endsWith("/");
                ResourceType resourceType = isDir ? ResourceType.DIRECTORY : ResourceType.FILE;
                if (isDir) {
                    name = name.endsWith("/") ? name : name + "/";
                }
                Long size = isDir ? null : item.size();
                String path = PathUtils.getParentPath(relativePath);

                String key = path + "/" + name;
                if (seenDirs.add(key)) {
                    resources.add(new ResourceResponse(path, name, size, resourceType));
                }
            }
        }

        return resources;
    }

    @SneakyThrows
    public byte[] downloadFile(int userId, String path) {
        String fullPath = PathUtils.getFullPath(userId, path);
        getFileInfo(userId, path);

        InputStream stream = minioClient.getObject(
                GetObjectArgs.builder()
                        .bucket(bucketName)
                        .object(fullPath)
                        .build());
        return IOUtils.toByteArray(stream);
    }

    @SneakyThrows
    public List<ResourceResponse> upload(List<MultipartFile> files, int userId, String basePath)  {
        if (files == null || files.isEmpty()) {
            throw new IllegalArgumentException("No files provided for upload");
        }

        List<ResourceResponse> resources = new ArrayList<>();
        List<SnowballObject> snowballObjects = new ArrayList<>();

        if (!basePath.endsWith("/")) {
            basePath += "/";
        }

        String fullPath = PathUtils.getFullPath(userId, basePath);

        for (MultipartFile file : files) {

            String objectName = fullPath + file.getOriginalFilename();
            boolean isFileExists = false;
            try {
                minioClient.statObject(
                        StatObjectArgs.builder()
                                .bucket(bucketName)
                                .object(objectName)
                                .build());
                isFileExists = true;
            } catch (Exception ignored) {}
            if (isFileExists) {
                throw new ResourceAlreadyExistsException("File already exists: " + basePath + file.getOriginalFilename());
            }

            snowballObjects.add(new SnowballObject(
                    objectName,
                    file.getInputStream(),
                    file.getSize(),
                    null
            ));
            resources.add(new ResourceResponse(basePath,
                    file.getOriginalFilename(),
                    file.getSize(),
                    ResourceType.FILE));
        }
        try {
            minioClient.uploadSnowballObjects(
                    UploadSnowballObjectsArgs.builder()
                            .bucket(bucketName)
                            .objects(snowballObjects)
                            .build());
        } catch (Exception e) {
            throw new RuntimeException("Can't upload objects: " + e.getMessage());
        }
        return resources;
    }
}
