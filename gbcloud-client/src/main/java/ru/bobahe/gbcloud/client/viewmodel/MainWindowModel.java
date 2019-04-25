package ru.bobahe.gbcloud.client.viewmodel;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import lombok.Getter;
import lombok.Setter;

public class MainWindowModel {
    private static MainWindowModel ourInstance = new MainWindowModel();

    private MainWindowModel() {

    }

    public static MainWindowModel getInstance() {
        return ourInstance;
    }

    @Getter
    private ObservableList<Filec> clientFilesList = FXCollections.observableArrayList();

    @Getter
    private ObservableList<Filec> serverFilesList = FXCollections.observableArrayList();

    @Setter
    @Getter
    private StringProperty serverPath= new SimpleStringProperty();
}
