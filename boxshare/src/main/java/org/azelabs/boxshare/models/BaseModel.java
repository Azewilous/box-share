package org.azelabs.boxshare.models;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.time.ZonedDateTime;

@MappedSuperclass
public class BaseModel implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Getter
    protected long id;

    @Getter
    @Setter
    protected ZonedDateTime created_at = ZonedDateTime.now();

    @Getter
    @Setter
    protected ZonedDateTime updated_at = ZonedDateTime.now();

}
