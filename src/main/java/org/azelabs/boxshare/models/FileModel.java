package org.azelabs.boxshare.models;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.azelabs.boxshare.application.enums.UploadStatus;

@Getter
@Setter
@Table(name = "files")
@Entity
public class FileModel extends BaseModel {

    @Column(unique = true)
    private String name;
    private Long size;
    private String mimeType;
    private String uploadedBy;
    @Enumerated(EnumType.ORDINAL)
    private UploadStatus uploadStatus;

}
