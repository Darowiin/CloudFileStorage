package ru.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import ru.dto.ResourceResponse;
import ru.util.PathUtils;

import java.util.List;

@Service
@RequiredArgsConstructor(onConstructor_ = @Autowired)
@Slf4j
public class StorageService {
    private final DirectoryService directoryService;
    private final ResourceService resourceService;

    public List<ResourceResponse> getInfo(int userId, String path) {
        log.info("Getting info for path: {}", path);
        boolean isDir = PathUtils.isDirectory(path);
        log.info("Path {} is directory: {}", path, isDir);

        if (isDir) {
            return directoryService.getDirectoryFilesInfo(userId, path);
        } else {
            ResourceResponse fileInfo = resourceService.getFileInfo(userId, path);
            return List.of(fileInfo);
        }
    }

    public void delete(int userId, String path) {
        log.info("Deleting resource at path: {}", path);
        boolean isDir = PathUtils.isDirectory(path);
        log.info("Path {} is directory: {}", path, isDir);

        if (isDir) {
            log.info("Deleting directory: {}", path);
            directoryService.deleteDirectory(userId, path);
            log.info("Directory deleted successfully: {}", path);
        } else {
            log.info("Deleting file: {}", path);
            resourceService.delete(userId, path);
            log.info("File deleted successfully: {}", path);
        }
    }

    public ResourceResponse move(int userId, String from, String to) {
        return resourceService.move(userId, from, to);
    }


    public List<ResourceResponse> search(int userId, String query) {
        return resourceService.search(userId, query);
    }

    public List<ResourceResponse> upload(List<MultipartFile> files, int userId, String path) {
        return resourceService.upload(files, userId, path);
    }

    public byte[] download(int userId, String path) {
        String fullPath = PathUtils.getFullPath(userId, path);
        if (PathUtils.isDirectory(fullPath)) {
            return directoryService.downloadDirectory(userId, path);
        } else {
            return resourceService.downloadFile(userId, path);
        }
    }

    public ResourceResponse createDirectory(int userId, String path) {
        return directoryService.createDirectory(userId, path);
    }
}
