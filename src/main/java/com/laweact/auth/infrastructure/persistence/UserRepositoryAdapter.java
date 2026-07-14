package com.laweact.auth.infrastructure.persistence;

import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.laweact.auth.domain.User;
import com.laweact.auth.domain.UserRepository;

@Repository
public class UserRepositoryAdapter implements UserRepository {

    private final SpringDataUserRepository springDataUserRepository;

    public UserRepositoryAdapter(SpringDataUserRepository springDataUserRepository) {
        this.springDataUserRepository = springDataUserRepository;
    }

    @Override
    public User save(User user) {
        UserJpaEntity entity = new UserJpaEntity(
                user.getId(),
                user.getEmail(),
                user.getPasswordHash(),
                user.getCreatedAt());
        UserJpaEntity saved = springDataUserRepository.save(entity);
        return toDomain(saved);
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return springDataUserRepository.findByEmail(email.toLowerCase().trim()).map(this::toDomain);
    }

    @Override
    public Optional<User> findById(UUID id) {
        return springDataUserRepository.findById(id).map(this::toDomain);
    }

    @Override
    public boolean existsByEmail(String email) {
        return springDataUserRepository.existsByEmail(email.toLowerCase().trim());
    }

    private User toDomain(UserJpaEntity entity) {
        return new User(entity.getId(), entity.getEmail(), entity.getPasswordHash(), entity.getCreatedAt());
    }
}
