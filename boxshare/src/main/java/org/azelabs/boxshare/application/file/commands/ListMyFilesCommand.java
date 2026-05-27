package org.azelabs.boxshare.application.file.commands;

import lombok.RequiredArgsConstructor;
import org.azelabs.boxshare.application.interfaces.ICommand;
import org.azelabs.boxshare.dtos.FileRecord;
import org.azelabs.boxshare.services.interfaces.IFileService;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class ListMyFilesCommand implements ICommand<String, List<FileRecord>> {

    private final IFileService service;

    @Override
    public List<FileRecord> handle(String identity) {
        return service.allByOwner(identity);
    }
}
