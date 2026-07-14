package com.laweact.auth.application;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.laweact.auth.application.dto.AuthTokenResult;
import com.laweact.auth.application.dto.RegisterCommand;
import com.laweact.auth.domain.User;
import com.laweact.auth.domain.UserRepository;
import com.laweact.shared.exception.ApiException;
import com.laweact.shared.security.JwtService;

@Service
public class RegisterUserUseCase {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public RegisterUserUseCase(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional
    public AuthTokenResult execute(RegisterCommand command) {
        String email = command.email().toLowerCase().trim();
        if (userRepository.existsByEmail(email)) {
            throw new ApiException("Email already registered", HttpStatus.CONFLICT);
        }

        User user = User.create(email, passwordEncoder.encode(command.password()));
        User saved = userRepository.save(user);

        String token = jwtService.generateToken(saved.getId(), saved.getEmail());
        return new AuthTokenResult(token, "Bearer", jwtService.getExpirationMs() / 1000);
    }
}
