package org.azelabs.boxshare.application.file.commands;

import lombok.RequiredArgsConstructor;
import org.azelabs.boxshare.application.interfaces.ICommand;
import org.azelabs.boxshare.services.interfaces.IFileService;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DeleteFileCommand implements ICommand<Long, Boolean> {

    private final IFileService service;

    @Override
    public Boolean handle(Long id) {
        return service.delete(id);
    }
}
