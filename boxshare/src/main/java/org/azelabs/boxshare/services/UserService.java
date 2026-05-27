package org.azelabs.boxshare.services;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.azelabs.boxshare.repositories.IUserRepository;
import org.azelabs.boxshare.dtos.UserRecord;
import org.azelabs.boxshare.models.UserModel;
import org.azelabs.boxshare.services.interfaces.IUserService;
import org.springframework.stereotype.Service;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService implements IUserService {

    private final IUserRepository repository;

    @Override
    public List<UserRecord> all() {
        return repository.findAll().stream().map(this::toDTO).collect(Collectors.toList());
    }

    @Override
    public Optional<UserRecord> getById(Long id) {
        return repository.findById(id).map(this::toDTO);
    }

    @Override
    public Optional<UserRecord> getByEmail(String email) {
        return repository.findByEmail(email).map(this::toDTO);
    }

    @Override
    public UserRecord save(UserRecord user) {
        return repository.findByEmail(user.email())
                .map(this::toDTO)
                .orElseGet(() -> toDTO(repository.save(toEntity(user))));
    }

    @Override
    public UserRecord update(UserRecord user) {
        UserModel dbUser = repository.findById(user.id())
                .orElseThrow(() -> new EntityNotFoundException("Could not update user " + user.email()));
        dbUser.setFirstName(user.firstName());
        dbUser.setLastName(user.lastName());
        dbUser.setEmail(user.email());
        dbUser.setUpdated_at(ZonedDateTime.now());
        UserModel saved = repository.save(dbUser);
        return toDTO(saved);
    }

    @Override
    public boolean delete(Long id) {
        if (!repository.existsById(id)) {
            return false;
        }
        repository.deleteById(id);
        return true;
    }

    private UserRecord toDTO(UserModel user) {
        return new UserRecord(user.getId(), user.getFirstName(), user.getLastName(), user.getEmail());
    }

    private UserModel toEntity(UserRecord user) {
        UserModel model = new UserModel();
        model.setFirstName(user.firstName());
        model.setLastName(user.lastName());
        model.setEmail(user.email());
        model.setIdentity(UUID.randomUUID());
        return model;
    }
}
