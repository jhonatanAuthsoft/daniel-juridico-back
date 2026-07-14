package com.laweact.auth.application;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.laweact.auth.application.dto.UserResult;
import com.laweact.auth.domain.User;
import com.laweact.auth.domain.UserRepository;
import com.laweact.shared.exception.ApiException;

@Service
public class GetCurrentUserUseCase {

    private final UserRepository userRepository;

    public GetCurrentUserUseCase(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public UserResult execute(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException("User not found", HttpStatus.NOT_FOUND));
        return new UserResult(user.getId(), user.getEmail());
    }
}
