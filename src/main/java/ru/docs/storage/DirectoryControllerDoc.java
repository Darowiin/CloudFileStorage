package ru.docs.storage;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.RequestParam;
import ru.dto.ResourceResponse;
import ru.security.CustomUserDetails;

import java.util.List;

/**
 * Documentation for the Directory Controller
 */
@Tag(name = "Directory Management", description = "API for managing directories and folders")
public interface DirectoryControllerDoc {

    @Operation(summary = "List directory contents",
               description = "Returns information about files in the specified directory")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Directory contents retrieved successfully",
                    content = @Content(mediaType = "application/json",
                    array = @ArraySchema(schema = @Schema(implementation = ResourceResponse.class)))),
        @ApiResponse(responseCode = "400", description = "Invalid path",
                    content = @Content(mediaType = "application/json",
                    schema = @Schema(example = "{\"message\": \"Invalid path: must not contain '..' or '//'\"}"))),
        @ApiResponse(responseCode = "404", description = "Directory not found or empty",
                    content = @Content(mediaType = "application/json",
                    schema = @Schema(example = "{\"message\": \"The folder is empty or doesn't exists: /path/to/folder\"}"))),
        @ApiResponse(responseCode = "500", description = "Internal server error",
                    content = @Content(mediaType = "application/json",
                    schema = @Schema(example = "{\"message\": \"Internal server error message\"}")))
    })
    List<ResourceResponse> getDirectoryFilesInfo(
            @Parameter(description = "Path to the directory", required = true)
            @RequestParam String path,
            @AuthenticationPrincipal CustomUserDetails userDetails);

    @Operation(summary = "Create directory",
               description = "Creates a new directory at the specified path")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Directory created successfully",
                    content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = ResourceResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid path",
                    content = @Content(mediaType = "application/json",
                    schema = @Schema(example = "{\"message\": \"Invalid path: must not contain '..' or '//'\"}"))),
        @ApiResponse(responseCode = "404", description = "Parent directory not found",
                    content = @Content(mediaType = "application/json",
                    schema = @Schema(example = "{\"message\": \"Parent directory doesn't exists: /path/to/parent\"}"))),
        @ApiResponse(responseCode = "409", description = "Directory already exists",
                    content = @Content(mediaType = "application/json",
                    schema = @Schema(example = "{\"message\": \"Directory already exists: /path/to/directory\"}"))),
        @ApiResponse(responseCode = "500", description = "Internal server error",
                    content = @Content(mediaType = "application/json",
                    schema = @Schema(example = "{\"message\": \"Internal server error message\"}")))
    })
    ResourceResponse createDirectory(
            @Parameter(description = "Path for the new directory", required = true)
            @RequestParam String path,
            @AuthenticationPrincipal CustomUserDetails userDetails);
}
