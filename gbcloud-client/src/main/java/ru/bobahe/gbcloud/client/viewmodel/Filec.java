package ru.bobahe.gbcloud.client.viewmodel;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

@Getter
@Builder
@ToString
public class Filec {
    private String name;
    private String isFolder;
}
