package org.azelabs.boxshare.application.enums;

import lombok.Getter;

@Getter
public enum CommandType {
    CREATE_USER("CreateUserCommand"),
    DELETE_USER("DeleteUserCommand"),
    UPDATE_USER("UpdateUserCommand"),
    GET_USER("GetUserCommand"),
    CREATE_FILE("CreateFileCommand"),
    GET_FILE("GetFileCommand"),
    DELETE_FILE("DeleteFileCommand");

    private final String name;

    CommandType(String name) {
        this.name = name;
    }

}
