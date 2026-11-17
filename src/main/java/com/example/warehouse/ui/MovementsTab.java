package com.example.warehouse.ui;

import com.example.warehouse.db.DataAccessException;
import com.example.warehouse.db.WarehouseRepository;
import com.example.warehouse.model.Movement;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Tab;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;

public class MovementsTab extends Tab {

    private final WarehouseRepository repo;
    private final TableView<Movement> table = new TableView<>();

    public MovementsTab(WarehouseRepository repo, Events events) {
        super("Movements");
        this.repo = repo;
        setClosable(false);

        table.getColumns().add(col("Date", "createdAt", 160));
        table.getColumns().add(col("SKU", "sku", 130));
        table.getColumns().add(col("Type", "type", 80));
        table.getColumns().add(col("Change", "delta", 80));
        table.getColumns().add(col("Note", "note", 400));
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);

        Button reload = new Button("Reload");
        reload.setOnAction(e -> refresh());
        HBox bar = new HBox(8, reload);
        bar.setPadding(new Insets(10));

        BorderPane root = new BorderPane(table);
        root.setTop(bar);
        setContent(root);

        events.onChange(this::refresh);
    }

    private static <T> TableColumn<Movement, T> col(String title, String property, double width) {
        TableColumn<Movement, T> c = new TableColumn<>(title);
        c.setCellValueFactory(new PropertyValueFactory<>(property));
        c.setPrefWidth(width);
        return c;
    }

    public void refresh() {
        try {
            table.getItems().setAll(repo.recentMovements(1000));
        } catch (DataAccessException e) {
            Dialogs.error(e.getMessage());
        }
    }
}
