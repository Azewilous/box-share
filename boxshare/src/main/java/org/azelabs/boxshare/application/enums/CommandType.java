package org.azelabs.boxshare.application.enums;

import lombok.Getter;

@Getter
public enum CommandType {
    CREATE_USER("CreateUserCommand"),
    DELETE_USER("DeleteUserCommand"),
    UPDATE_USER("UpdateUserCommand"),
    GET_USER("GetUserCommand"),
    VERIFY_USER_EMAIL("VerifyUserEmailCommand"),
    CREATE_FILE("CreateFileCommand"),
    GET_FILE("GetFileCommand"),
    DELETE_FILE("DeleteFileCommand"),
    LIST_MY_FILES("ListFilesCommand"),
    TOGGLE_FILE_VISIBILITY("ToggleFileVisibilityCommand"),
    SHARE_FILE("ShareFileCommand");

    private final String name;

    CommandType(String name) {
        this.name = name;
    }

}
