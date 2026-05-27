package org.azelabs.boxshare.models;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.ZonedDateTime;
import java.util.UUID;

@Setter
@Getter
@Table(name = "users")
@Entity
public class UserModel extends BaseModel {

    private String firstName;
    private String lastName;
    @Column(unique = true, nullable = false)
    private String email;
    @Column(unique = true, nullable = false)
    private String username;
    @Column(nullable = false)
    private String password;
    @Column(unique = true, nullable = false)
    private UUID identity;
    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "role_id")
    private RoleModel role;
    private ZonedDateTime emailVerifiedOn;
    private UUID verificationToken;
    private ZonedDateTime verificationTokenExpiresAt;
    private ZonedDateTime lastVerificationSentAt;
}
