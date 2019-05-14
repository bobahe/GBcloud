package ru.bobahe.gbcloud.client.viewmodel;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

@Getter
@Builder
@ToString
public class FileInfo {
    private String name;
    private String isFolder;
}
