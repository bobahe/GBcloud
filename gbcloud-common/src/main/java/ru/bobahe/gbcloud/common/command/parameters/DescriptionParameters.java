package ru.bobahe.gbcloud.common.command.parameters;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class DescriptionParameters implements Parameters {
    private static final long serialVersionUID = -8635191547652247862L;

    private String description;
}
