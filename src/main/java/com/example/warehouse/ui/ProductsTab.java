package com.example.warehouse.ui;

import com.example.warehouse.db.DataAccessException;
import com.example.warehouse.db.WarehouseRepository;
import com.example.warehouse.model.Product;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;

import java.util.List;
import java.util.Locale;

public class ProductsTab extends Tab {

    private final WarehouseRepository repo;
    private final Events events;
    private final TableView<Product> table = new TableView<>();
    private final TextField search = new TextField();
    private final Label status = new Label();

    public ProductsTab(WarehouseRepository repo, Events events) {
        super("Stock");
        this.repo = repo;
        this.events = events;
        setClosable(false);

        table.getColumns().add(col("SKU", "sku", 130));
        table.getColumns().add(col("Name", "name", 300));
        table.getColumns().add(col("Location", "location", 110));
        table.getColumns().add(col("Quantity", "quantity", 90));
        table.getColumns().add(col("Min. stock", "minStock", 90));
        table.getColumns().add(col("Price", "price", 90));
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.setRowFactory(tv -> {
            TableRow<Product> row = new TableRow<>() {
                @Override
                protected void updateItem(Product p, boolean empty) {
                    super.updateItem(p, empty);
                    boolean low = !empty && p != null && p.getQuantity() <= p.getMinStock();
                    setStyle(low ? "-fx-background-color: #ffe0e0;" : "");
                }
            };
            row.setOnMouseClicked(e -> {
                if (e.getClickCount() == 2 && !row.isEmpty()) editSelected();
            });
            return row;
        });

        search.setPromptText("Search SKU, name or location...");
        search.setPrefWidth(300);
        search.textProperty().addListener((o, a, b) -> refresh());

        Button add = new Button("Add product");
        Button edit = new Button("Edit");
        Button delete = new Button("Delete");
        Button book = new Button("Book stock...");
        add.setOnAction(e -> addProduct());
        edit.setOnAction(e -> editSelected());
        delete.setOnAction(e -> deleteSelected());
        book.setOnAction(e -> bookSelected());

        HBox bar = new HBox(8, search, add, edit, delete, book);
        bar.setPadding(new Insets(10));
        HBox.setHgrow(search, Priority.NEVER);
        status.setPadding(new Insets(6, 10, 6, 10));

        BorderPane root = new BorderPane(table);
        root.setTop(bar);
        root.setBottom(status);
        setContent(root);

        events.onChange(this::refresh);
    }

    private static <T> TableColumn<Product, T> col(String title, String property, double width) {
        TableColumn<Product, T> c = new TableColumn<>(title);
        c.setCellValueFactory(new PropertyValueFactory<>(property));
        c.setPrefWidth(width);
        return c;
    }

    public void refresh() {
        try {
            String keep = selectedSku();
            List<Product> all = repo.findAll(search.getText());
            table.getItems().setAll(all);
            if (keep != null) {
                all.stream().filter(p -> p.getSku().equals(keep)).findFirst()
                        .ifPresent(p -> table.getSelectionModel().select(p));
            }
            long units = all.stream().mapToLong(Product::getQuantity).sum();
            double value = all.stream().mapToDouble(p -> p.getQuantity() * p.getPrice()).sum();
            long low = all.stream().filter(p -> p.getQuantity() <= p.getMinStock()).count();
            status.setText(String.format(Locale.ROOT,
                    "%d products | %d units | stock value %.2f | %d at or below minimum",
                    all.size(), units, value, low));
        } catch (DataAccessException e) {
            Dialogs.error(e.getMessage());
        }
    }

    private String selectedSku() {
        Product p = table.getSelectionModel().getSelectedItem();
        return p == null ? null : p.getSku();
    }

    private void addProduct() {
        ProductDialog.show(null).ifPresent(p -> {
            try {
                repo.insert(p);
                events.fire();
            } catch (DataAccessException e) {
                Dialogs.error(e.getMessage());
            }
        });
    }

    private void editSelected() {
        Product sel = table.getSelectionModel().getSelectedItem();
        if (sel == null) {
            Dialogs.info("Select a product first.");
            return;
        }
        ProductDialog.show(sel).ifPresent(p -> {
            try {
                repo.update(p);
                events.fire();
            } catch (DataAccessException e) {
                Dialogs.error(e.getMessage());
            }
        });
    }

    private void deleteSelected() {
        Product sel = table.getSelectionModel().getSelectedItem();
        if (sel == null) {
            Dialogs.info("Select a product first.");
            return;
        }
        if (Dialogs.confirm("Delete " + sel.getSku() + " - " + sel.getName() + "?")) {
            try {
                repo.delete(sel.getId());
                events.fire();
            } catch (DataAccessException e) {
                Dialogs.error(e.getMessage());
            }
        }
    }

    private void bookSelected() {
        Product sel = table.getSelectionModel().getSelectedItem();
        if (sel == null) {
            Dialogs.info("Select a product first.");
            return;
        }
        StockDialog.show(sel).ifPresent(b -> {
            try {
                repo.adjustStock(sel.getSku(), b.delta(), b.type(), b.note());
                events.fire();
            } catch (DataAccessException e) {
                Dialogs.error(e.getMessage());
            }
        });
    }
}
