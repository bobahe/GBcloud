package ru.bobahe.gbcloud.server.properties;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

public class ApplicationProperties {
    private static ApplicationProperties ourInstance = new ApplicationProperties();
    private Properties properties;

    public static ApplicationProperties getInstance() {
        return ourInstance;
    }

    private ApplicationProperties() {
        properties = new Properties();
        try (FileInputStream propFileStream = new FileInputStream("server.properties")) {
            properties.load(propFileStream);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String getProperty(String key) {
        return properties.getProperty(key);
    }
}
