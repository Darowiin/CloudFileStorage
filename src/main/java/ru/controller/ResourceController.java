package ru.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import ru.docs.storage.ResourceControllerDoc;
import ru.dto.ResourceResponse;
import ru.security.CustomUserDetails;
import ru.service.StorageService;
import ru.util.PathUtils;


import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping(value = "/api/resource")
@RequiredArgsConstructor(onConstructor_ = @Autowired)
public class ResourceController implements ResourceControllerDoc {
    private final StorageService storageService;

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public List<ResourceResponse> getResourceInfo(@RequestParam String path,
                                                  @AuthenticationPrincipal CustomUserDetails userDetails) {
        return storageService.getInfo(userDetails.getId(), path);
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteResource(@RequestParam String path,
                                            @AuthenticationPrincipal CustomUserDetails userDetails) {
        storageService.delete(userDetails.getId(), path);
    }

    @GetMapping("/move")
    @ResponseStatus(HttpStatus.OK)
    public ResourceResponse moveResource(@RequestParam String from,
                                         @RequestParam String to,
                                         @AuthenticationPrincipal CustomUserDetails userDetails) {
        return storageService.move(userDetails.getId(), from, to);
    }

    @GetMapping("/search")
    @ResponseStatus(HttpStatus.OK)
    public List<ResourceResponse> searchResource(@RequestParam String query,
                                                 @AuthenticationPrincipal CustomUserDetails userDetails) {
        return storageService.search(userDetails.getId(), query);
    }

    @PostMapping(consumes = {MediaType.MULTIPART_FORM_DATA_VALUE})
    @ResponseStatus(HttpStatus.CREATED)
    public List<ResourceResponse> uploadResource(@RequestPart(value = "object") List<MultipartFile> files,
                                            @RequestParam(required = false, defaultValue = "") String path,
                                            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return storageService.upload(files, userDetails.getId(), path);
    }

    @GetMapping("/download")
    public ResponseEntity<ByteArrayResource> downloadResource(@RequestParam(required = false, defaultValue = "") String path,
                                                              @AuthenticationPrincipal CustomUserDetails userDetails) {
        boolean isDirectory = PathUtils.isDirectory(path);

        byte[] content = storageService.download(userDetails.getId(), path);
        ByteArrayResource resource = new ByteArrayResource(content);

        String filename = PathUtils.getFileName(path);
        if (isDirectory) {
            filename = filename.isBlank() ? "files.zip" : filename + ".zip";
        }
        filename = URLEncoder.encode(filename, StandardCharsets.UTF_8);

        return ResponseEntity.status(HttpStatus.OK)
                .contentLength(content.length)
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header("Content-disposition", "attachment; filename=\"" + filename + "\"")
                .body(resource);
    }
}
