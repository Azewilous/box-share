package org.azelabs.boxshare.services.interfaces;

import org.azelabs.boxshare.dtos.UserDTO;

import java.util.List;
import java.util.Optional;

public interface IUserService {
    List<UserDTO> all();
    Optional<UserDTO> getById(Long id);
    Optional<UserDTO> getByEmail(String email);
    UserDTO save(UserDTO user);
    UserDTO update(UserDTO user);
    boolean delete(Long id);
}
