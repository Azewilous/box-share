package org.azelabs.boxshare.application.user.commands;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.azelabs.boxshare.application.interfaces.ICommand;
import org.azelabs.boxshare.dtos.UserDTO;
import org.azelabs.boxshare.services.interfaces.IUserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class UpdateUserCommand implements ICommand<UserDTO, Optional<UserDTO>> {

    private static final Logger log = LoggerFactory.getLogger(UpdateUserCommand.class);
    private final IUserService service;

    @Override
    public Optional<UserDTO> handle(UserDTO user) {
        try {
            return Optional.of(this.service.update(user));
        } catch (EntityNotFoundException e) {
            log.warn("User not found for update: {}", user.id());
            return Optional.empty();
        }
    }

}
