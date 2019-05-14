package ru.bobahe.gbcloud.common;

import lombok.Getter;
import lombok.Setter;
import ru.bobahe.gbcloud.common.fs.FSUtils;

import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Paths;

public class FileChunk implements Serializable {
    private static final long serialVersionUID = 483955420908884631L;

    @Getter
    private byte[] data = new byte[8192];

    @Getter
    @Setter
    private String filePath;

    @Getter
    @Setter
    private String destinationFilePath;

    @Getter
    private int length;

    @Getter
    private long offset;

    public boolean getNextChunk() throws IOException {
        if (length != -1) {
            length = FSUtils.readFileChunk(Paths.get(filePath), data, offset);
            offset += length;
            return true;
        }

        offset = 0;
        length = 0;

        return false;
    }
}
