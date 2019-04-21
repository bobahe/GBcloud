package ru.bobahe.gbcloud.common;

import java.io.Serializable;

public class Command implements Serializable {
    private static final long serialVersionUID = -6269805515535392423L;

    public enum Action {
        REGISTER,
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

    private String errorMessage;

    public Command setAction(Action action) {
        this.action = action;
        return this;
    }

    public Command addPath(String path) {
        this.path = path;
        return this;
    }

    public Command addFilename(String filename) {
        this.filename = filename;
        return this;
    }

    public Command setUsername(String username) {
        this.username = username;
        return this;
    }

    public Command setPassword(String password) {
        this.password = password;
        return this;
    }

    public Command setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
        return this;
    }

    public Action getAction() {
        return action;
    }

    public String getPath() {
        return path;
    }

    public String getFilename() {
        return filename;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public String getErrorMessage() {
        return errorMessage;
    }
}
