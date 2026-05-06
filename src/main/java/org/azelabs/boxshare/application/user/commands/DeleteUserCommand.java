package org.azelabs.boxshare.application.user.commands;

import org.azelabs.boxshare.application.interfaces.ICommand;
import org.azelabs.boxshare.services.interfaces.IUserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class DeleteUserCommand implements ICommand<Long, Boolean> {

    private static final Logger log = LoggerFactory.getLogger(DeleteUserCommand.class);
    private final IUserService service;

    @Autowired
    public DeleteUserCommand(IUserService service) {
        this.service = service;
    }

    @Override
    public Boolean handle(Long request) {
        return this.service.delete(request);
    }
}
