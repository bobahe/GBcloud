package ru.bobahe.gbcloud.client.controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import lombok.Setter;
import ru.bobahe.gbcloud.client.viewmodel.globalViewModel;

public class NewDirectoryController {
    globalViewModel model = globalViewModel.getInstance();

    @FXML
    TextField directoryName;

    @Setter
    private String path;

    @Setter
    private boolean isClient;

    public void createDirectory(ActionEvent actionEvent) {
        try {
            model.createDirectory(isClient, path + directoryName.getText());
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            ((Stage) directoryName.getScene().getWindow()).close();
        }
    }
}
