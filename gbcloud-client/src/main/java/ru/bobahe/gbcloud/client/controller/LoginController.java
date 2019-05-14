package ru.bobahe.gbcloud.client.controller;

import javafx.application.Platform;
import javafx.beans.Observable;
import javafx.beans.property.StringProperty;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import lombok.extern.java.Log;
import ru.bobahe.gbcloud.client.viewmodel.GlobalViewModel;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

@Log
public class LoginController implements Initializable {
    @FXML
    private VBox root;

    @FXML
    private TextField login;

    @FXML
    private PasswordField password;

    @FXML
    Label loginError, passwordError;

    private GlobalViewModel model = GlobalViewModel.getInstance();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        onCloseRequest();
        connect();

        login.textProperty().addListener(this::loginTextChanged);
        password.textProperty().addListener(this::passwordTextChanged);
        model.getMessageFromServer().addListener(this::showMessageFromServer);
    }

    private void connect() {
        if (model.getIsConnected().get()) {
            return;
        }

        new Thread(() -> {
            try {
                model.getClient().connect();
            } catch (Exception e) {
                Platform.runLater(() -> passwordError.setText("Возникла ошибка при подключении"));
                log.info(e.getMessage());
            }
        }).start();
    }

    private void onCloseRequest() {
        Platform.runLater(() -> {
            root.getScene().getWindow().setOnCloseRequest(event -> {
                if (event.getEventType() == WindowEvent.WINDOW_CLOSE_REQUEST) {
                    while (!model.getClient().close()) {
                    }
                    Platform.exit();
                }
            });
        });
    }

    private void showMessageFromServer(Observable observable) {
        Platform.runLater(() -> {
            StringProperty prop = model.getMessageFromServer();

            if (!prop.get().startsWith("OK")) {
                passwordError.setText(prop.get());
                return;
            }

            showFileManager();
        });
    }

    private void showFileManager() {
        Platform.runLater(() -> {
            try {
                Stage window = (Stage) this.root.getScene().getWindow();
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/Main.fxml"));
                window.setResizable(true);
                window.setScene(new Scene(loader.load(), 800, 600));
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    private void passwordTextChanged(Observable observable) {
        if (password.getText().length() > 0) {
            passwordError.setText("");
        }
    }

    private void loginTextChanged(Observable observable) {
        if (login.getText().length() > 0) {
            loginError.setText("");
        }
    }

    public void registrationClicked(ActionEvent actionEvent) {
        Parent root;
        try {
            root = FXMLLoader.load(getClass().getResource("/fxml/Registration.fxml"));
            Scene scene = new Scene(root);
            ((Stage) this.root.getScene().getWindow()).setScene(scene);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void authenticate(ActionEvent actionEvent) {
        model.getMessageFromServer().set("");
        boolean stop = false;

        if (login.getText().equals("")) {
            loginError.setText("Поле логин не должно быть пустым");
            stop = true;
        }

        if (password.getText().equals("")) {
            passwordError.setText("Поле пароль не может быть пустым");
            stop = true;
        }

        if (stop) {
            login.requestFocus();
            return;
        }

        model.authenticate(login.getText(), password.getText());
    }
}
