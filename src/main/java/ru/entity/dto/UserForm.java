package ru.entity.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UserForm(
    @NotBlank(message = "username must be not blank")
    @Size(min = 4, max = 20, message = "username size must be between 4 and 20")
    String username,
    @NotBlank(message = "password must be not blank")
    @Size(min = 6, max = 20, message = "password size must be between 6 and 20")
    String password
) {
}
