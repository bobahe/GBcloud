package ru.bobahe.gbcloud.common;

import lombok.Builder;
import lombok.Getter;

import java.io.Serializable;
import java.util.Map;

@Getter
@Builder
public class Command implements Serializable {
    private static final long serialVersionUID = 3987679474598702876L;

    public enum Action {
        REGISTER,
        SUCCESS,
        UPLOAD,
        DELETE,
        CREATE,
        DOWNLOAD,
        LIST,
        AUTH,
        ERROR
    }

    private Action action;

    private String path;
    private String filename;
    private String destinationPath;

    private String username;
    private String password;

    private String description;

    protected Map<String, Boolean> childFiles;
}
