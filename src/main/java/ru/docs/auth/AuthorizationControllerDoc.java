package ru.docs.auth;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.RequestBody;
import ru.dto.UserForm;
import ru.dto.UserResponse;
import ru.security.CustomUserDetails;

/**
 * Documentation for the Authorization Controller
 */
@Tag(name = "Authentication & Authorization", description = "API for user registration, login and logout")
public interface AuthorizationControllerDoc {

    @Operation(summary = "Register a new user",
               description = "Creates a new user in the system and establishes a session")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "User successfully registered",
                    content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = UserResponse.class,
                                    example = "{\"username\": \"user_1\"}"))),
        @ApiResponse(responseCode = "400", description = "Validation error",
                    content = @Content(mediaType = "application/json",
                    schema = @Schema(example = "{\"message\": \"username size must be between 4 and 20\"}"))),
        @ApiResponse(responseCode = "409", description = "Username already exists",
                    content = @Content(mediaType = "application/json",
                    schema = @Schema(example = "{\"message\": \"User with this username already exists\"}"))),
        @ApiResponse(responseCode = "500", description = "Internal server error",
                    content = @Content(mediaType = "application/json",
                    schema = @Schema(example = "{\"message\": \"Internal server error message\"}")))
    })
    UserResponse signUp(
            @Parameter(description = "User registration data")
            @Valid @RequestBody UserForm form,
            HttpServletRequest request,
            HttpServletResponse response);

    @Operation(summary = "Log in a user",
               description = "Authenticates an existing user and establishes a session")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "User successfully authenticated",
                    content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = UserResponse.class,
                                    example = "{\"username\": \"user_1\"}"))),
        @ApiResponse(responseCode = "400", description = "Validation error",
                    content = @Content(mediaType = "application/json",
                    schema = @Schema(example = "{\"message\": \"password must be not blank\"}"))),
        @ApiResponse(responseCode = "401", description = "Invalid credentials",
                    content = @Content(mediaType = "application/json",
                    schema = @Schema(example = "{\"message\": \"Bad credentials\"}"))),
        @ApiResponse(responseCode = "500", description = "Internal server error",
                    content = @Content(mediaType = "application/json",
                    schema = @Schema(example = "{\"message\": \"Internal server error message\"}")))
    })
    UserResponse signIn(
            @Parameter(description = "User login credentials")
            @Valid @RequestBody UserForm form,
            HttpServletRequest request,
            HttpServletResponse response);

    @Operation(summary = "Log out",
               description = "Terminates the user session")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "User successfully logged out",
                    content = @Content),
        @ApiResponse(responseCode = "401", description = "User not authenticated",
                    content = @Content(mediaType = "application/json",
                    schema = @Schema(example = "{\"message\": \"The user is not logged in\"}"))),
        @ApiResponse(responseCode = "500", description = "Internal server error",
                    content = @Content(mediaType = "application/json",
                    schema = @Schema(example = "{\"message\": \"Internal server error message\"}")))
    })
    void logout(HttpServletRequest request, HttpServletResponse response);

    @Operation(summary = "Get current user information",
               description = "Returns information about the currently authenticated user")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "User information retrieved successfully",
                    content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = UserResponse.class,
                                    example = "{\"username\": \"user_1\"}"))),
        @ApiResponse(responseCode = "401", description = "User not authenticated",
                    content = @Content(mediaType = "application/json",
                    schema = @Schema(example = "{\"message\": \"The user is not logged in\"}"))),
        @ApiResponse(responseCode = "500", description = "Internal server error",
                    content = @Content(mediaType = "application/json",
                    schema = @Schema(example = "{\"message\": \"Internal server error message\"}")))
    })
    UserResponse getCurrentUser(@AuthenticationPrincipal CustomUserDetails userDetails);
}
