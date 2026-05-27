package org.azelabs.boxshare.models;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.azelabs.boxshare.application.enums.RoleType;

@Setter
@Getter
@Table(name = "roles")
@Entity
public class RoleModel extends BaseModel {
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RoleType type;
}
