package org.azelabs.boxshare.application.user.queries;

import org.azelabs.boxshare.application.interfaces.ICommand;
import org.azelabs.boxshare.dtos.UserRecord;
import org.azelabs.boxshare.services.interfaces.IUserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class GetUserCommand implements ICommand<Long, Optional<UserRecord>> {

    private static final Logger log = LoggerFactory.getLogger(GetUserCommand.class);
    private final IUserService service;

    @Autowired
    public GetUserCommand(IUserService service) {
        this.service = service;
    }

    @Override
    public Optional<UserRecord> handle(Long id) {
        return this.service.getById(id);
    }

}
