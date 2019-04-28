package ru.bobahe.gbcloud.common.command.parameters;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class CredentialParameters implements Parameters {
    private static final long serialVersionUID = 1082875166885140941L;

    private String username;
    private String password;
}
