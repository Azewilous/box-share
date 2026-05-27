package org.azelabs.boxshare.application.file.commands;

import lombok.RequiredArgsConstructor;
import org.azelabs.boxshare.application.interfaces.ICommand;
import org.azelabs.boxshare.dtos.FileRecord;
import org.azelabs.boxshare.services.interfaces.IFileService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.http.SdkHttpMethod;

@Component
@RequiredArgsConstructor
public class CreateFileCommand implements ICommand<FileRecord, FileRecord> {

    private static final Logger log = LoggerFactory.getLogger(CreateFileCommand.class);
    private final IFileService service;

    @Override
    public FileRecord handle(FileRecord request) {
        FileRecord file = this.service.save(request);
        log.info("Saved new file with name {}", file.name());
        return service.generatePreSignedUrl(file.name(), SdkHttpMethod.PUT);
    }
}
