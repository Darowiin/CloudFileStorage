package ru.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import ru.docs.storage.DirectoryControllerDoc;
import ru.dto.ResourceResponse;
import ru.security.CustomUserDetails;
import ru.service.StorageService;

import java.util.List;

@RestController
@RequestMapping(value = "/api/directory")
@RequiredArgsConstructor(onConstructor_ = @Autowired)
public class DirectoryController implements DirectoryControllerDoc {
    private final StorageService storageService;

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public List<ResourceResponse> getDirectoryFilesInfo(@RequestParam(required = false, defaultValue = "") String path,
                                                        @AuthenticationPrincipal CustomUserDetails userDetails) {
        return storageService.getInfo(userDetails.getId(), path);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ResourceResponse createDirectory(@RequestParam String path,
                                            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return storageService.createDirectory(userDetails.getId(), path);
    }
}
