package org.azelabs.boxshare.repositories;

import org.azelabs.boxshare.models.UserModel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface IUserRepository extends JpaRepository<UserModel, Long> {
    Optional<UserModel> findByEmail(String email);
    Optional<UserModel> findByUsername(String username);
    Optional<UserModel> findByIdentity(UUID identity);
    Optional<UserModel> findByVerificationToken(UUID verificationToken);
}
