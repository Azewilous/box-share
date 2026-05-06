package org.azelabs.boxshare.application.file.queries;

import lombok.RequiredArgsConstructor;
import org.azelabs.boxshare.application.interfaces.ICommand;
import org.azelabs.boxshare.dtos.FileDTO;
import org.azelabs.boxshare.services.interfaces.IFileService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.http.SdkHttpMethod;

@Component
@RequiredArgsConstructor
public class GetFileCommand implements ICommand<String, FileDTO> {

    private static final Logger log = LoggerFactory.getLogger(GetFileCommand.class);
    private final IFileService service;

    @Override
    public FileDTO handle(String request) {
        return service.generatePreSignedUrl(request, SdkHttpMethod.GET);
    }
}
