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
import javafx.scene.paint.Paint;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import ru.bobahe.gbcloud.client.viewmodel.GlobalViewModel;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class RegistrationController implements Initializable {
    @FXML
    private VBox root;

    @FXML
    private TextField name, login;

    @FXML
    private PasswordField password, duplicate;

    @FXML
    private Label loginError, passwordError, duplicateError;

    private GlobalViewModel model = GlobalViewModel.getInstance();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        onCloseRequest();

        login.textProperty().addListener(this::loginTextChanged);
        password.textProperty().addListener(this::passwordTextChanged);
        model.getMessageFromServer().addListener(this::showMessageFromServer);
    }

    private void onCloseRequest() {
        Platform.runLater(() -> {
            root.getScene().getWindow().setOnCloseRequest(event -> {
                if (event.getEventType() == WindowEvent.WINDOW_CLOSE_REQUEST) {
                    while(!GlobalViewModel.getInstance().getClient().close()) {}
                    Platform.exit();
                }
            });
        });
    }

    private void showMessageFromServer(Observable observable) {
        Platform.runLater(() -> {
            duplicateError.setTextFill(Paint.valueOf("red"));
            if (model.getMessageFromServerType().get() == 0) {
                duplicateError.setTextFill(Paint.valueOf("#90b539"));
                login.setText("");
                password.setText("");
                duplicate.setText("");
            }

            StringProperty prop = (StringProperty) observable;
            duplicateError.setText(prop.get());
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

    public void register(ActionEvent actionEvent) {
        model.getMessageFromServer().set("");

        if (validate()) {
            return;
        }

        model.register(login.getText(), password.getText());
    }

    private boolean validate() {
        boolean stop = false;

        if (login.getText().equals("")) {
            loginError.setText("Поле логин не должно быть пустым");
            stop = true;
        }

        if (password.getText().equals("")) {
            passwordError.setText("Поле пароль не может быть пустым");
            stop = true;
        }

        if (!duplicate.getText().equals(password.getText())) {
            passwordError.setText("Пароли не совпадают");
            stop = true;
        }

        if (stop) {
            login.requestFocus();
        }

        return stop;
    }

    public void loginClicked(ActionEvent actionEvent) {
        Parent root;
        try {
            root = FXMLLoader.load(getClass().getResource("/fxml/Login.fxml"));
            Scene scene = new Scene(root);
            ((Stage) this.root.getScene().getWindow()).setScene(scene);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
