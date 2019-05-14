package ru.bobahe.gbcloud.common.fs;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

public class FSUtils {
    public static int readFileChunk(Path path, byte[] data, long offset) throws IOException {
        int length;

        RandomAccessFile raf = new RandomAccessFile(path.toString(), "r");
        raf.seek(offset);
        length = raf.read(data);
        raf.close();

        return length;
    }

    public static void writeFileChunk(Path path, byte[] data, long offset, int length) {
        if (path.getNameCount() > 1) {
            Path directory = path.subpath(0, path.getNameCount() - 1);

            try {
                Files.createDirectories(directory);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }


        try (RandomAccessFile raf = new RandomAccessFile(path.toString(), "rw")) {
            raf.seek(offset - data.length);
            raf.write(data, 0, length);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static Map<String, Boolean> getFileList(String path) throws IOException {
        Map<String, Boolean> result = new HashMap<>();
        Path pathToWalk = Paths.get(path);

        // check isExist and isDirectory
        if (!new File(pathToWalk.toString()).exists()) {
            throw new IOException("Путь не существует.");
        }

        if (!Files.isDirectory(pathToWalk)) {
            throw new IOException("Путь не является директорией.");
        }

        Files.walk(pathToWalk, 1)
                .forEach(p -> {
                    String filename = p.getFileName().toString();

                    if (!filename.startsWith(".") && !p.equals(pathToWalk)) {
                        result.put(filename, Files.isDirectory(p));
                    }
                });

        return result;
    }

    public static boolean delete(Path path) throws IOException {

        // Пишут, что плохо использовать лямбду и при этом изменять что-то извне.
        // Рекомендуют использовать обычный цикл. А коли сильно хочется, то обертку типа Atomic.

//        AtomicBoolean isDeleteFalse = new AtomicBoolean(false);
//        Files.walk(path).sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(file -> {
//            if (!file.delete()) {
//                isDeleteFalse.set(true);
//            }
//        });

        boolean isDeleteFalse = false;
        List<File> files = Files.walk(path).sorted(Comparator.reverseOrder()).map(Path::toFile).collect(Collectors.toList());

        for (File f : files) {
            if (!f.delete()) {
                isDeleteFalse = true;
            }
        }

        return isDeleteFalse;
    }

    public static boolean checkFolders(Path... paths) {
        return Arrays.stream(paths).allMatch(p -> Files.isDirectory(p));
    }

    public static void createDirectory(Path path) throws IOException {
        Files.createDirectories(path);
    }
}
