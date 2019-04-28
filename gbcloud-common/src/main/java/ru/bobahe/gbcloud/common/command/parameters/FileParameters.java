package ru.bobahe.gbcloud.common.command.parameters;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class FileParameters implements Parameters {
    private static final long serialVersionUID = 5877402157268041640L;

    private String path;
    private String destinationPath;
}
