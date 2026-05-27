package org.azelabs.boxshare.application.user.commands;

import lombok.RequiredArgsConstructor;
import org.azelabs.boxshare.application.interfaces.ICommand;
import org.azelabs.boxshare.dtos.UserRecord;
import org.azelabs.boxshare.services.interfaces.IUserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CreateUserCommand implements ICommand<UserRecord, UserRecord> {

    private static final Logger log = LoggerFactory.getLogger(CreateUserCommand.class);
    private final IUserService service;

    @Override
    public UserRecord handle(UserRecord user) {
        return this.service.save(user);
    }
}
