package com.laweact.auth.application;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.laweact.auth.application.dto.AuthTokenResult;
import com.laweact.auth.application.dto.LoginCommand;
import com.laweact.auth.domain.User;
import com.laweact.auth.domain.UserRepository;
import com.laweact.shared.exception.ApiException;
import com.laweact.shared.security.JwtService;

@Service
public class LoginUseCase {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public LoginUseCase(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional(readOnly = true)
    public AuthTokenResult execute(LoginCommand command) {
        User user = userRepository.findByEmail(command.email().toLowerCase().trim())
                .orElseThrow(() -> new ApiException("Invalid email or password", HttpStatus.UNAUTHORIZED));

        if (!passwordEncoder.matches(command.password(), user.getPasswordHash())) {
            throw new ApiException("Invalid email or password", HttpStatus.UNAUTHORIZED);
        }

        String token = jwtService.generateToken(user.getId(), user.getEmail());
        return new AuthTokenResult(token, "Bearer", jwtService.getExpirationMs() / 1000);
    }
}
