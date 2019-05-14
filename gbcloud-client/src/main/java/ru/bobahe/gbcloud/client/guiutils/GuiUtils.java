package ru.bobahe.gbcloud.client.guiutils;

import javafx.application.Platform;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import ru.bobahe.gbcloud.client.viewmodel.FileInfo;
import ru.bobahe.gbcloud.client.viewmodel.GlobalViewModel;

import java.util.Optional;

public class GuiUtils {
    //region TableViews configuration
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
    //endregion

    //region New folder dialog code
    public static Optional<String> showNewFolderDialog() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Новая папка");
        dialog.setHeaderText("Введите имя папки");
        dialog.setContentText("Имя: ");

        return dialog.showAndWait();
    }
    //endregion

    //region Show alert
    public static void showAlert(Alert.AlertType alertType, String title, String header, String content) {
        Alert alert = new Alert(alertType, content, ButtonType.OK);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.showAndWait();
    }
    //endregion
}
