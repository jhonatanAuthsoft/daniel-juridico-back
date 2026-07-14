package com.laweact.auth.presentation;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.laweact.auth.application.GetCurrentUserUseCase;
import com.laweact.auth.application.LoginUseCase;
import com.laweact.auth.application.RegisterUserUseCase;
import com.laweact.auth.application.dto.AuthTokenResult;
import com.laweact.auth.application.dto.LoginCommand;
import com.laweact.auth.application.dto.RegisterCommand;
import com.laweact.auth.application.dto.UserResult;
import com.laweact.auth.presentation.dto.LoginRequest;
import com.laweact.auth.presentation.dto.RegisterRequest;
import com.laweact.auth.presentation.dto.TokenResponse;
import com.laweact.auth.presentation.dto.UserResponse;
import com.laweact.shared.security.AuthenticatedUser;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Auth", description = "Registro e login JWT (exemplo)")
public class AuthController {

    private final RegisterUserUseCase registerUserUseCase;
    private final LoginUseCase loginUseCase;
    private final GetCurrentUserUseCase getCurrentUserUseCase;

    public AuthController(
            RegisterUserUseCase registerUserUseCase,
            LoginUseCase loginUseCase,
            GetCurrentUserUseCase getCurrentUserUseCase) {
        this.registerUserUseCase = registerUserUseCase;
        this.loginUseCase = loginUseCase;
        this.getCurrentUserUseCase = getCurrentUserUseCase;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Registrar usuário", security = {})
    public TokenResponse register(@Valid @RequestBody RegisterRequest request) {
        AuthTokenResult result = registerUserUseCase.execute(
                new RegisterCommand(request.email(), request.password()));
        return toTokenResponse(result);
    }

    @PostMapping("/login")
    @Operation(summary = "Login", security = {})
    public TokenResponse login(@Valid @RequestBody LoginRequest request) {
        AuthTokenResult result = loginUseCase.execute(
                new LoginCommand(request.email(), request.password()));
        return toTokenResponse(result);
    }

    @GetMapping("/me")
    @Operation(summary = "Usuário autenticado", security = @SecurityRequirement(name = "bearerAuth"))
    public UserResponse me(@AuthenticationPrincipal AuthenticatedUser user) {
        UserResult result = getCurrentUserUseCase.execute(user.getId());
        return new UserResponse(result.id(), result.email());
    }

    private TokenResponse toTokenResponse(AuthTokenResult result) {
        return new TokenResponse(result.accessToken(), result.tokenType(), result.expiresIn());
    }
}
