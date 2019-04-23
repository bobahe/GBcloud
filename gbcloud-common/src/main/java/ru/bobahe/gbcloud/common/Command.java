package ru.bobahe.gbcloud.common;

import lombok.Builder;
import lombok.Getter;

import java.io.Serializable;

@Getter
@Builder
public class Command implements Serializable {
    private static final long serialVersionUID = -6269805515535392423L;

    public enum Action {
        REGISTER,
        SUCCESS,
        UPLOAD,
        DELETE,
        DOWNLOAD,
        LIST,
        AUTH,
        ERROR
    }

    private Action action;

    private String path;
    private String filename;

    private String username;
    private String password;

    private String description;
}
