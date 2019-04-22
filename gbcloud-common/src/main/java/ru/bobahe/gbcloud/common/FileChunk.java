package ru.bobahe.gbcloud.common;

import ru.bobahe.gbcloud.common.fs.FileWorker;

import java.io.Serializable;
import java.nio.file.Paths;

public class FileChunk implements Serializable {
    private static final long serialVersionUID = 483955420908884631L;

    private Command command;
    private byte[] data = new byte[4096];
    private String filePath;
    private String destinationFilePath;
    private int length;
    private long offset;

    transient private FileWorker fileWorker = new FileWorker();

    public FileChunk(Command command) {
        this.command = command;
    }

    public boolean getNextChunk() {
        if (length != -1) {
            length = fileWorker.readFileChunk(Paths.get(filePath), data);
            offset = fileWorker.getOffset();
            return true;
        }

        fileWorker.flush();
        length = 0;

        return false;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setDestinationFilePath(String destinationFilePath) {
        this.destinationFilePath = destinationFilePath;
    }

    public String getDestinationFilePath() {
        return destinationFilePath;
    }

    public long getOffset() {
        return offset;
    }

    public int getLength() {
        return length;
    }

    public Command getCommand() {
        return command;
    }

    public byte[] getData() {
        return data;
    }
}
