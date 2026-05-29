package org.azelabs.boxshare.application.file.commands;

import lombok.RequiredArgsConstructor;
import org.azelabs.boxshare.application.interfaces.ICommand;
import org.azelabs.boxshare.dtos.FileRecord;
import org.azelabs.boxshare.dtos.VisibilityRequest;
import org.azelabs.boxshare.services.interfaces.IFileService;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ToggleFileVisibilityCommand implements ICommand<VisibilityRequest, FileRecord> {

    private final IFileService service;

    @Override
    public FileRecord handle(VisibilityRequest request) {
        return service.updateVisibility(request.id(), request.visibility());
    }
}
