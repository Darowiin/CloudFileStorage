package ru.controller;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.entity.dto.UserForm;
import ru.service.UserService;

@RestController
@RequestMapping(value = "/api", produces = "application/json")
@RequiredArgsConstructor(onConstructor_ = @Autowired)
public class AuthorizationController {
    private final UserService userService;

    @PostMapping("/auth/sign-up")
    public ResponseEntity<?> signUp(@Valid @RequestBody UserForm form, HttpSession session) {
        if (userService.findByUsername(form.username()) != null) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(java.util.Map.of("message","Username is already in use"));
        }
        userService.save(form);

        session.setAttribute("username", form.username());

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(java.util.Map.of("username", form.username()));
    }

    @PostMapping("/auth/sign-in")
    public ResponseEntity<?> signIn(@Valid @RequestBody UserForm form, HttpSession session) {
        if (userService.existsByUsernameAndPassword(form.username(), form.password())) {
            session.setAttribute("username", form.username());

            return ResponseEntity.ok().body(java.util.Map.of("username", form.username()));
        } else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(java.util.Map.of("message", "Username or password is invalid"));
        }
    }

    @PostMapping("/auth/sign-out")
    public ResponseEntity<?> signOut(HttpSession session, Authentication authentication) {
        if (authentication != null && authentication.isAuthenticated() &&
                !(authentication instanceof AnonymousAuthenticationToken)) {
            session.removeAttribute("username");
            session.invalidate();
            return ResponseEntity.noContent().build();
        } else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(java.util.Map.of("message", "You are not logged in"));
        }
    }
}
