package ru.bobahe.gbcloud.server.auth;

public interface AuthService {
    String getFolderByUsernameAndPassword(String username, String password);
}
