package ru.bobahe.gbcloud.common;

import java.io.Serializable;

public class Command implements Serializable {
    public enum Action {
        UPLOAD,
        DELETE,
        DOWNLOAD,
        LIST
    }

    private Action command;
    private String path;
    private String filename;

    public Command(Action command) {
        this.command = command;
    }

    public Command addPath(String path) {
        this.path = path;
        return this;
    }

    public Command addFilename(String filename) {
        this.filename = filename;
        return this;
    }

    public Action getCommand() {
        return command;
    }

    public String getPath() {
        return path;
    }

    public String getFilename() {
        return filename;
    }
}
