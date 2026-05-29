package org.azelabs.boxshare.application.file.commands;

import lombok.RequiredArgsConstructor;
import org.azelabs.boxshare.application.interfaces.ICommand;
import org.azelabs.boxshare.dtos.ShareFileRequest;
import org.azelabs.boxshare.services.interfaces.IFileService;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ShareFileCommand implements ICommand<ShareFileRequest, Void> {

    private final IFileService service;

    @Override
    public Void handle(ShareFileRequest request) {
        service.shareFile(request);
        return null;
    }
}
