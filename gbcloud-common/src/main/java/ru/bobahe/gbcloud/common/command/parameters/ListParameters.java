package ru.bobahe.gbcloud.common.command.parameters;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Map;

@Getter
@AllArgsConstructor
public class ListParameters implements Parameters {
    private static final long serialVersionUID = -4470958279065168189L;

    private String path;
    protected Map<String, Boolean> fileList;
}
