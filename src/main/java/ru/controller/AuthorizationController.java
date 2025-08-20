package ru.controller;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
    public ResponseEntity<?> signUp(@Valid @RequestBody UserForm form, HttpSession session, HttpServletResponse response) {
        try {
            if (userService.findByUsername(form.username()) != null) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(java.util.Map.of("message","Username is already in use"));
            }
            userService.save(form);

            session.setAttribute("username", form.username());
            response.addHeader("Set-Cookie", "SESSIONID=" + session.getId() + "; Path=/; HttpOnly");

            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(java.util.Map.of("username", form.username()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(java.util.Map.of("message","Something went wrong. Please try again later."));
        }
    }

    @PostMapping("/auth/sign-in")
    public ResponseEntity<?> signIn(@Valid @RequestBody UserForm form) {
        try {
            if (userService.existsByUsernameAndPassword(form.username(), form.password())) {
                return ResponseEntity.ok().body(java.util.Map.of("message", "success sign-in"));
            } else {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(java.util.Map.of("message", "Username or password is invalid"));
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(java.util.Map.of("message","Something went wrong. Please try again later."));
        }
    }

    @PostMapping("/auth/sign-out")
    public ResponseEntity<?> signOut(HttpSession session) {
        try {
            if (session.getAttribute("username") != null) {
                session.removeAttribute("username");
                session.invalidate();
                return ResponseEntity.noContent().build();
            } else {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(java.util.Map.of("message", "You are not logged in"));
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(java.util.Map.of("message","Something went wrong. Please try again later."));
        }
    }

}
