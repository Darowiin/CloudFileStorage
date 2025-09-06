package ru.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "User registration and authentication data form")
public record UserForm(
    @Schema(description = "Username", example = "user_1")
    @NotBlank(message = "username must be not blank")
    @Size(min = 4, max = 20, message = "username size must be between 4 and 20")
    String username,

    @Schema(description = "User password", example = "password")
    @NotBlank(message = "password must be not blank")
    @Size(min = 6, max = 20, message = "password size must be between 6 and 20")
    String password
) {
}
