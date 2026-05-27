package org.azelabs.boxshare.application.user.commands;

import lombok.RequiredArgsConstructor;
import org.azelabs.boxshare.application.interfaces.ICommand;
import org.azelabs.boxshare.dtos.UserRecord;
import org.azelabs.boxshare.services.interfaces.IUserService;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class VerifyUserEmailCommand implements ICommand<UUID, UserRecord> {
    private final IUserService service;

    @Override
    public UserRecord handle(UUID token) {
        return this.service.verifyUserEmail(token);
    }
}
