package ru.bobahe.gbcloud.server.db;

import ru.bobahe.gbcloud.server.properties.ApplicationProperties;

import java.sql.*;

public class SQLHandler {
    private static Connection connection;
    private static PreparedStatement psGetFolderByLoginAndPassword;
    private static PreparedStatement psInsertNewUser;

    public static boolean connect() {
        try {
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection(ApplicationProperties.getInstance().getProperty("db.name"));
            //psChangeNick = connection.prepareStatement("UPDATE users SET nickname = ? WHERE nickname = ?;");
            psGetFolderByLoginAndPassword = connection.prepareStatement("SELECT folder FROM users WHERE username = ? AND password = ?;");
            psInsertNewUser = connection.prepareStatement("INSERT INTO users (username, password, folder) VALUES (?, ?, ?)");
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public static String getFolderByUsernameAndPassword(String username, String password) {
        String folder = null;

        try {
            psGetFolderByLoginAndPassword.setString(1, username);
            psGetFolderByLoginAndPassword.setString(2, password);
            ResultSet rs = psGetFolderByLoginAndPassword.executeQuery();
            if (rs.next()) {
                folder = rs.getString(1);
            }
            rs.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return folder;
    }

    public static void disconnect() {
        try {
            psGetFolderByLoginAndPassword.close();
            psInsertNewUser.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        try {
            connection.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void insertNewUser(String username, String password, String folder) throws SQLException {
        psInsertNewUser.setString(1, username);
        psInsertNewUser.setString(2, password);
        psInsertNewUser.setString(3, folder);

        psInsertNewUser.executeUpdate();
    }
}
