package ru.bobahe.gbcloud.client;

import javafx.application.Application;
import javafx.event.EventHandler;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import ru.bobahe.gbcloud.client.properties.ApplicationProperties;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

//todo Сделать форму для авторизации и регистрации

public class MainApp extends Application {
    public static void main(String[] args) {
        launch(args);
        checkRootFolder();
    }

    private static void checkRootFolder() {
        try {
            Files.createDirectories(Paths.get(ApplicationProperties.getInstance().getProperty("root.directory")));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/fxml/Login.fxml"));
        Parent root = fxmlLoader.load();
        //primaryStage.initStyle(StageStyle.TRANSPARENT);
        primaryStage.setTitle("GBCloud client");
        Scene scene = new Scene(root);
        scene.setFill(Color.TRANSPARENT);

        primaryStage.setScene(scene);
        primaryStage.setResizable(false);
        primaryStage.show();
    }
}
