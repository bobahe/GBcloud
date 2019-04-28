package ru.bobahe.gbcloud.common.command;

import lombok.Builder;
import lombok.Getter;
import ru.bobahe.gbcloud.common.command.parameters.Parameters;

import java.io.Serializable;

@Getter
@Builder
public class Command implements Serializable {
    private static final long serialVersionUID = 3987679474598702876L;

    private Action action;

    private Parameters parameters;
}
