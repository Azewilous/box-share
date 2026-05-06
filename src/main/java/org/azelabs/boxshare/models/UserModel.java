package org.azelabs.boxshare.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@Table(name = "users")
@Entity
public class UserModel extends BaseModel {

    private String firstName;
    private String lastName;
    @Column(unique = true)
    private String email;

}
