package ru.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import ru.docs.auth.AuthorizationControllerDoc;
import ru.dto.UserForm;
import ru.dto.UserResponse;
import ru.exception.ResourceAlreadyExistsException;
import ru.security.CustomUserDetails;
import ru.service.StorageService;
import ru.service.UserService;

import java.util.Map;

@RestController
@RequestMapping(value = "/api", produces = "application/json")
@RequiredArgsConstructor(onConstructor_ = @Autowired)
public class AuthorizationController implements AuthorizationControllerDoc {
    private final UserService userService;
    private final StorageService storageService;
    private final AuthenticationManager authenticationManager;

    @PostMapping("/auth/sign-up")
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse signUp(
            @Valid @RequestBody UserForm form) {
        UserResponse user = userService.createUser(form);

        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(form.username(), form.password());
        Authentication authentication = authenticationManager.authenticate(auth);

        SecurityContextHolder.getContext().setAuthentication(authentication);

        try {
            storageService.createDirectory(userService.findByUsername(user.username()).getId(), "");
        } catch (ResourceAlreadyExistsException ignored) {}

        return user;
    }

    @PostMapping("/auth/sign-out")
    public ResponseEntity<?> logout(HttpServletRequest request, HttpServletResponse response) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth != null && !(auth instanceof AnonymousAuthenticationToken)) {
            new SecurityContextLogoutHandler().logout(request, response, auth);

            var session = request.getSession(false);
            if (session != null) {
                session.invalidate();
            }
            return ResponseEntity.noContent().build();
        } else {
            return ResponseEntity.status(HttpServletResponse.SC_UNAUTHORIZED)
                    .body(Map.of("message", "User is not logged in"));
        }
    }

    @GetMapping("/user/me")
    public UserResponse getCurrentUser(@AuthenticationPrincipal CustomUserDetails userDetails) {
        return new UserResponse(userDetails.getUsername());
    }
}
