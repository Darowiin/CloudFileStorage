package ru.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextHolderStrategy;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import ru.docs.auth.AuthorizationControllerDoc;
import ru.dto.UserForm;
import ru.dto.UserResponse;
import ru.security.CustomUserDetails;
import ru.service.UserService;

@RestController
@RequestMapping(value = "/api", produces = "application/json")
@RequiredArgsConstructor(onConstructor_ = @Autowired)
public class AuthorizationController implements AuthorizationControllerDoc {
    private final UserService userService;
    private final AuthenticationManager authenticationManager;
    private final SecurityContextRepository securityContextRepository;
    private final SecurityContextHolderStrategy securityContextHolderStrategy;

    @PostMapping("/auth/sign-up")
    public UserResponse signUp(
            @Valid @RequestBody UserForm form,
            HttpServletRequest request,
            HttpServletResponse response) {
        UserResponse user = userService.createUser(form);

        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(form.username(), form.password());
        Authentication authentication = authenticationManager.authenticate(auth);

        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
        securityContext.setAuthentication(authentication);
        securityContextHolderStrategy.setContext(securityContext);

        securityContextRepository.saveContext(securityContext, request, response);

        return user;
    }

    @PostMapping("/auth/sign-in")
    public UserResponse signIn(
            @Valid @RequestBody UserForm form,
            HttpServletRequest request,
            HttpServletResponse response) {
        UsernamePasswordAuthenticationToken usernamePasswordAuthenticationToken = new UsernamePasswordAuthenticationToken(
                form.username(),
                form.password()
        );
        Authentication authentication = authenticationManager.authenticate(usernamePasswordAuthenticationToken);
        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
        securityContextHolderStrategy.setContext(securityContext);
        securityContext.setAuthentication(authentication);

        securityContextRepository.saveContext(securityContext, request, response);
        CustomUserDetails principal = (CustomUserDetails) authentication.getPrincipal();
        return new UserResponse(principal.getUsername());
    }

    @PostMapping("/auth/sign-out")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(HttpServletRequest request, HttpServletResponse response) {
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        securityContextHolderStrategy.setContext(context);
        securityContextRepository.saveContext(context, request, response);
    }

    @GetMapping("/user/me")
    public UserResponse getCurrentUser(@AuthenticationPrincipal CustomUserDetails userDetails) {
        return new UserResponse(userDetails.getUsername());
    }
}
