package org.azelabs.boxshare.services;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.azelabs.boxshare.repositories.IUserRepository;
import org.azelabs.boxshare.dtos.UserDTO;
import org.azelabs.boxshare.models.UserModel;
import org.azelabs.boxshare.services.interfaces.IUserService;
import org.springframework.stereotype.Service;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService implements IUserService {

    private final IUserRepository repository;

    @Override
    public List<UserDTO> all() {
        return repository.findAll().stream().map(this::toDTO).collect(Collectors.toList());
    }

    @Override
    public Optional<UserDTO> getById(Long id) {
        return repository.findById(id).map(this::toDTO);
    }

    @Override
    public Optional<UserDTO> getByEmail(String email) {
        return repository.findByEmail(email).map(this::toDTO);
    }

    @Override
    public UserDTO save(UserDTO user) {
        UserModel dbUser = toEntity(user);
        UserModel saved = repository.save(dbUser);
        return toDTO(saved);
    }

    @Override
    public UserDTO update(UserDTO user) {
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

    private UserDTO toDTO(UserModel user) {
        return new UserDTO(user.getId(), user.getFirstName(), user.getLastName(), user.getEmail());
    }

    private UserModel toEntity(UserDTO user) {
        UserModel model = new UserModel();
        model.setFirstName(user.firstName());
        model.setLastName(user.lastName());
        model.setEmail(user.email());
        return model;
    }
}
