package ru.bobahe.gbcloud.client.guiutils;

import javafx.application.Platform;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import ru.bobahe.gbcloud.client.viewmodel.FileInfo;
import ru.bobahe.gbcloud.client.viewmodel.GlobalViewModel;

public class GuiUtils {
    @SuppressWarnings("unchecked")
    public static void prepareTableViews(TableView<FileInfo>... fileTableViews) {
        for (TableView<FileInfo> t : fileTableViews) {
            t.getColumns().addAll(
                    getNewColumn("Имя", "name"),
                    getNewColumn("Тип", "isFolder")
            );

            if (t.getId().startsWith("client")) {
                t.setItems(GlobalViewModel.getInstance().getClientFilesList());
            } else {
                t.setItems(GlobalViewModel.getInstance().getServerFilesList());
            }
        }

        setTableViewsColumnWidth(fileTableViews);
    }

    private static void setTableViewsColumnWidth(TableView<FileInfo>... fileTableViews) {
        Platform.runLater(() -> {
            for (TableView<FileInfo> t : fileTableViews) {
                t.getColumns().get(0).setPrefWidth(t.getWidth() * 85 / 100);
                t.getColumns().get(1).setPrefWidth(t.getWidth() * 14 / 100);
            }
        });
    }

    private static TableColumn<FileInfo, String> getNewColumn(String name, String propertyName) {
        TableColumn<FileInfo, String> newColumn = new TableColumn<>(name);
        newColumn.setCellValueFactory(new PropertyValueFactory<>(propertyName));
        return newColumn;
    }
}
