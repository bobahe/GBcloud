package ru.bobahe.gbcloud.server.auth;

import java.sql.SQLException;

public interface AuthService {
    String getFolderByUsernameAndPassword(String username, String password);
    void insertNewUser(String username, String password, String folder) throws SQLException;
}
