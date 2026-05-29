package org.azelabs.boxshare.models;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@Table(name = "shared_files", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"user_id", "file_id"})
})
@Entity
public class SharedFileModel extends BaseModel {
    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private UserModel user;

    @ManyToOne(optional = false)
    @JoinColumn(name = "file_id", nullable = false)
    private FileModel file;
}