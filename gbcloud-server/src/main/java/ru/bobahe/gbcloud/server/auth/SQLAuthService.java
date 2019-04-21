package ru.bobahe.gbcloud.server.auth;

import ru.bobahe.gbcloud.server.db.SQLHandler;

public class SQLAuthService implements AuthService {
    @Override
    public String getFolderByUsernameAndPassword(String username, String password) {
        return SQLHandler.getFolderByUsernameAndPassword(username, password);
    }
}
