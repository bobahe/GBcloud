package ru.bobahe.gbcloud.common.fs;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

public class FileWorker {
    private long offset;

    public int readFileChunk(Path path, byte[] data) {
        int length = -1;
        try (RandomAccessFile raf = new RandomAccessFile(path.toString(), "r")) {
            raf.seek(offset);
            length = raf.read(data);
            offset += data.length;
        } catch (IOException e) {
            e.printStackTrace();
        }

        return length;
    }

    public void writeFileChunk(Path path, byte[] data, long offset, int length) {
        try (RandomAccessFile raf = new RandomAccessFile(path.toString(), "rw")) {
            raf.seek(offset - data.length);
            raf.write(data, 0, length);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public boolean checkFolders(Path... paths) {
        return Arrays.stream(paths).allMatch(p -> Files.isDirectory(p));
    }

    public void flush() {
        this.offset = 0;
    }

    public long getOffset() {
        return offset;
    }
}
