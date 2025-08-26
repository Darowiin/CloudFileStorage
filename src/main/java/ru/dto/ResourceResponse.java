package ru.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import ru.entity.ResourceType;

@Schema(description = "Information about a file or directory resource")
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ResourceResponse(
        @Schema(description = "Path to the parent directory", example = "/folder/")
        String path,

        @Schema(description = "Name of the resource", example = "document.pdf")
        String name,

        @Schema(description = "Size of the resource in bytes", example = "1024")
        Long Size,

        @Schema(description = "Type of resource - FILE or DIRECTORY", example = "FILE")
        ResourceType resourceType
) {
}
