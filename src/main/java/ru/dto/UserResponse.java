package ru.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Response with user information after registration or login")
public record UserResponse(
        @Schema(description = "Username of the authenticated user", example = "user_1")
        String username) {
}
