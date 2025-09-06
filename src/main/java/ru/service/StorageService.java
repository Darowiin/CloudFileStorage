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
        log.info("User: {}, Getting info for path: {}", userId, path);
        boolean isDir = PathUtils.isDirectory(path);
        log.info("User: {}, Path {} is directory: {}",userId, path, isDir);

        if (isDir) {
            return directoryService.getDirectoryFilesInfo(userId, path);
        } else {
            ResourceResponse fileInfo = resourceService.getFileInfo(userId, path);
            return List.of(fileInfo);
        }
    }

    public void delete(int userId, String path) {
        log.info("User: {}, Deleting resource at path: {}",userId, path);
        boolean isDir = PathUtils.isDirectory(path);
        log.info("User: {}, Path {} is directory: {}", userId, path, isDir);

        if (isDir) {
            log.info("User: {}, Deleting directory: {}", userId, path);
            directoryService.deleteDirectory(userId, path);
            log.info("User: {}, Directory deleted successfully: {}", userId, path);
        } else {
            log.info("User: {}, Deleting file: {}", userId, path);
            resourceService.delete(userId, path);
            log.info("User: {}, File deleted successfully: {}", userId, path);
        }
    }

    public ResourceResponse move(int userId, String from, String to) {
        log.info("User: {}, Moving resource from: {}, to: {}", userId, from, to);

        boolean isDirFrom = PathUtils.isDirectory(from);
        boolean isDirTo = PathUtils.isDirectory(to);

        log.info("User: {}, From path {} is directory: {}", userId, from, isDirFrom);
        log.info("User: {}, To path {} is directory: {}", userId, to, isDirTo);

        if (isDirFrom && isDirTo) {
            return directoryService.moveDirectory(userId, from, to);
        }
        return resourceService.move(userId, from, to);
    }


    public List<ResourceResponse> search(int userId, String query) {
        log.info("User: {}, Searching with query: {}", userId, query);
        return resourceService.search(userId, query);
    }

    public List<ResourceResponse> upload(List<MultipartFile> files, int userId, String path) {
        log.info("User: {}, Uploading files to: {}", userId, path);
        return resourceService.upload(files, userId, path);
    }

    public byte[] download(int userId, String path) {
        String fullPath = PathUtils.getFullPath(userId, path);
        if (PathUtils.isDirectory(fullPath)) {
            log.info("User: {}, Downloading directory from: {}", userId, path);
            return directoryService.downloadDirectory(userId, path);
        } else {
            log.info("User: {}, Downloading file from: {}", userId, path);
            return resourceService.downloadFile(userId, path);
        }
    }

    public ResourceResponse createDirectory(int userId, String path) {
        log.info("User: {}, Creating directory: {}", userId, path);
        return directoryService.createDirectory(userId, path);
    }
}
