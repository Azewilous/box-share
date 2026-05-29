package org.azelabs.boxshare.models;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.azelabs.boxshare.application.enums.FileVisibility;
import org.azelabs.boxshare.application.enums.UploadStatus;

import java.util.UUID;

@Getter
@Setter
@Table(name = "files")
@Entity
public class FileModel extends BaseModel {

    @Column(unique = true)
    private String name;
    private Long size;
    private String mimeType;
    @ManyToOne(optional = false)
    @JoinColumn(name = "owner_id", nullable = false)
    private UserModel owner;
    @Enumerated(EnumType.ORDINAL)
    private UploadStatus uploadStatus;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "varchar(255) default 'PRIVATE' not null")
    private FileVisibility visibility = FileVisibility.PRIVATE;
    @Column(unique = true)
    private UUID shareToken;

}
