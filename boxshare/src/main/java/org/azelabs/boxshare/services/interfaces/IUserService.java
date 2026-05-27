package org.azelabs.boxshare.services.interfaces;

import org.azelabs.boxshare.dtos.UserRecord;

import java.util.List;
import java.util.Optional;

public interface IUserService {
    List<UserRecord> all();
    Optional<UserRecord> getById(Long id);
    Optional<UserRecord> getByEmail(String email);
    UserRecord save(UserRecord user);
    UserRecord update(UserRecord user);
    boolean delete(Long id);
}
