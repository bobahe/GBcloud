package ru.bobahe.gbcloud.common;

import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Paths;

public class Message implements Serializable {
    private Command command;
    private byte[] data;

    public Message(Command command) {
        this.command = command;

        readFile();
    }

    private void readFile() {
        try {
            data = Files.readAllBytes(Paths.get(command.getFilename()));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Command getCommand() {
        return command;
    }

    public byte[] getData() {
        return data;
    }
}
