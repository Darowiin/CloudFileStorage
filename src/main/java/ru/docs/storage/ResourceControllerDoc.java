package ru.docs.storage;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;
import ru.dto.ResourceResponse;
import ru.security.CustomUserDetails;

import java.util.List;

/**
 * Documentation for the Resource Controller
 */
@Tag(name = "Resource Management", description = "API for managing files and resources")
public interface ResourceControllerDoc {

    @Operation(summary = "Get resource information",
               description = "Returns information about a file by its path")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Resource information retrieved successfully",
                    content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = ResourceResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid path",
                    content = @Content(mediaType = "application/json",
                    schema = @Schema(example = "{\"message\": \"Invalid path: must not contain '..' or '//'\"}"))),
        @ApiResponse(responseCode = "404", description = "Resource not found",
                    content = @Content(mediaType = "application/json",
                    schema = @Schema(example = "{\"message\": \"The resource was not found on: /path/to/file.txt\"}"))),
        @ApiResponse(responseCode = "500", description = "Internal server error",
                    content = @Content(mediaType = "application/json",
                    schema = @Schema(example = "{\"message\": \"Internal server error message\"}")))
    })
    List<ResourceResponse> getResourceInfo(
            @Parameter(description = "Path to the resource", required = true)
            @RequestParam String path,
            @AuthenticationPrincipal CustomUserDetails userDetails);

    @Operation(summary = "Delete resource",
               description = "Deletes a file at the specified path")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Resource successfully deleted",
                    content = @Content),
        @ApiResponse(responseCode = "400", description = "Invalid path",
                    content = @Content(mediaType = "application/json",
                    schema = @Schema(example = "{\"message\": \"Invalid path: must not contain '..' or '//'\"}"))),
        @ApiResponse(responseCode = "404", description = "Resource not found",
                    content = @Content(mediaType = "application/json",
                    schema = @Schema(example = "{\"message\": \"The resource was not found on: /path/to/file.txt\"}"))),
        @ApiResponse(responseCode = "500", description = "Internal server error",
                    content = @Content(mediaType = "application/json",
                    schema = @Schema(example = "{\"message\": \"Internal server error message\"}")))
    })
    void deleteResource(
            @Parameter(description = "Path to the resource to delete", required = true)
            @RequestParam String path,
            @AuthenticationPrincipal CustomUserDetails userDetails);

    @Operation(summary = "Move resource",
               description = "Moves a file from one location to another")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Resource successfully moved",
                    content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = ResourceResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid path",
                    content = @Content(mediaType = "application/json",
                    schema = @Schema(example = "{\"message\": \"Invalid path: must not contain '..' or '//'\"}"))),
        @ApiResponse(responseCode = "404", description = "Source resource not found",
                    content = @Content(mediaType = "application/json",
                    schema = @Schema(example = "{\"message\": \"The source file does not exist: /path/to/source.txt\"}"))),
        @ApiResponse(responseCode = "409", description = "Resource already exists at destination path",
                    content = @Content(mediaType = "application/json",
                    schema = @Schema(example = "{\"message\": \"The file already exists by path: /path/to/destination.txt\"}"))),
        @ApiResponse(responseCode = "500", description = "Internal server error",
                    content = @Content(mediaType = "application/json",
                    schema = @Schema(example = "{\"message\": \"Can't move file from /source.txt to /dest.txt: error details\"}")))
    })
    ResourceResponse moveResource(
            @Parameter(description = "Source path", required = true)
            @RequestParam String from,
            @Parameter(description = "Destination path", required = true)
            @RequestParam String to,
            @AuthenticationPrincipal CustomUserDetails userDetails);

    @Operation(summary = "Search resources",
               description = "Searches for resources by name")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Search completed successfully",
                    content = @Content(mediaType = "application/json",
                    array = @ArraySchema(schema = @Schema(implementation = ResourceResponse.class)))),
        @ApiResponse(responseCode = "400", description = "Invalid query",
                    content = @Content(mediaType = "application/json",
                    schema = @Schema(example = "{\"message\": \"The query can't be null or empty\"}"))),
        @ApiResponse(responseCode = "500", description = "Internal server error",
                    content = @Content(mediaType = "application/json",
                    schema = @Schema(example = "{\"message\": \"Internal server error message\"}")))
    })
    List<ResourceResponse> searchResource(
            @Parameter(description = "Search query", required = true)
            @RequestParam String query,
            @AuthenticationPrincipal CustomUserDetails userDetails);

    @Operation(summary = "Upload resources",
               description = "Uploads one or more files to the specified path")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Files uploaded successfully",
                    content = @Content(mediaType = "application/json",
                    array = @ArraySchema(schema = @Schema(implementation = ResourceResponse.class)))),
        @ApiResponse(responseCode = "400", description = "Invalid request or no files provided",
                    content = @Content(mediaType = "application/json",
                    schema = @Schema(example = "{\"message\": \"Cannot invoke \\\"org.springframework.web.multipart.MultipartFile.isEmpty()\\\" because \\\"file\\\" is null\"}"))),
        @ApiResponse(responseCode = "409", description = "File already exists",
                    content = @Content(mediaType = "application/json",
                    schema = @Schema(example = "{\"message\": \"File already exists: /path/to/existing-file.txt\"}"))),
        @ApiResponse(responseCode = "500", description = "Internal server error",
                    content = @Content(mediaType = "application/json",
                    schema = @Schema(example = "{\"message\": \"Can't upload objects: error details\"}")))
    })
    List<ResourceResponse> uploadResource(
            @Parameter(description = "Files to upload", required = true)
            @RequestPart(value = "object") List<MultipartFile> files,
            @Parameter(description = "Target path", required = false)
            @RequestParam(required = false, defaultValue = "") String path,
            @AuthenticationPrincipal CustomUserDetails userDetails);

    @Operation(summary = "Download resource",
               description = "Downloads a file or a directory as a zip archive")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Resource downloaded successfully",
                    content = @Content(mediaType = MediaType.APPLICATION_OCTET_STREAM_VALUE)),
        @ApiResponse(responseCode = "400", description = "Invalid path",
                    content = @Content(mediaType = "application/json",
                    schema = @Schema(example = "{\"message\": \"Invalid path: must not contain '..' or '//'\"}"))),
        @ApiResponse(responseCode = "404", description = "Resource not found",
                    content = @Content(mediaType = "application/json",
                    schema = @Schema(example = "{\"message\": \"The folder is empty or doesn't exist: /path/to/folder\"}"))),
        @ApiResponse(responseCode = "500", description = "Internal server error",
                    content = @Content(mediaType = "application/json",
                    schema = @Schema(example = "{\"message\": \"Object does not exist\"}")))
    })
    ResponseEntity<ByteArrayResource> downloadResource(
            @Parameter(description = "Path to the resource to download", required = false)
            @RequestParam(required = false, defaultValue = "") String path,
            @AuthenticationPrincipal CustomUserDetails userDetails);
}
