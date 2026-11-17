package com.example.warehouse.ui;

import com.example.warehouse.model.Product;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;

import java.util.Optional;
import java.util.function.Supplier;

public final class ProductDialog {
    private ProductDialog() {
    }

    public static Optional<Product> show(Product existing) {
        boolean edit = existing != null;
        Dialog<Product> dlg = new Dialog<>();
        dlg.setTitle(edit ? "Edit product" : "New product");
        dlg.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        TextField sku = new TextField(edit ? existing.getSku() : "");
        TextField name = new TextField(edit ? existing.getName() : "");
        TextField location = new TextField(edit && existing.getLocation() != null ? existing.getLocation() : "");
        TextField qty = new TextField(edit ? String.valueOf(existing.getQuantity()) : "0");
        TextField min = new TextField(edit ? String.valueOf(existing.getMinStock()) : "0");
        TextField price = new TextField(edit ? String.valueOf(existing.getPrice()) : "0.00");
        location.setPromptText("e.g. A-03-2");
        qty.setDisable(edit); // quantity changes go through stock bookings
        Label error = new Label();
        error.setStyle("-fx-text-fill: red;");

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(8);
        grid.setPadding(new Insets(15));
        String[] labels = {"SKU", "Name", "Location", edit ? "Quantity (use Book stock)" : "Initial quantity", "Min. stock", "Price"};
        TextField[] fields = {sku, name, location, qty, min, price};
        for (int i = 0; i < labels.length; i++) {
            grid.add(new Label(labels[i]), 0, i);
            grid.add(fields[i], 1, i);
            GridPane.setHgrow(fields[i], Priority.ALWAYS);
        }
        grid.add(error, 0, labels.length, 2, 1);
        dlg.getDialogPane().setContent(grid);

        Supplier<Product> build = () -> {
            String s = sku.getText().trim().toUpperCase();
            String n = name.getText().trim();
            if (s.isEmpty()) throw new IllegalArgumentException("SKU is required.");
            if (n.isEmpty()) throw new IllegalArgumentException("Name is required.");
            int q;
            int m;
            double pr;
            try {
                q = Integer.parseInt(qty.getText().trim());
                m = Integer.parseInt(min.getText().trim());
                pr = Double.parseDouble(price.getText().trim().replace(',', '.'));
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Quantity, min. stock and price must be numbers.");
            }
            if (q < 0 || m < 0 || pr < 0) throw new IllegalArgumentException("Values must not be negative.");
            return new Product(edit ? existing.getId() : 0, s, n, location.getText().trim(), q, m, pr);
        };

        Button ok = (Button) dlg.getDialogPane().lookupButton(ButtonType.OK);
        ok.addEventFilter(ActionEvent.ACTION, ev -> {
            try {
                build.get();
            } catch (IllegalArgumentException e) {
                error.setText(e.getMessage());
                ev.consume();
            }
        });
        dlg.setResultConverter(bt -> bt == ButtonType.OK ? build.get() : null);
        return dlg.showAndWait();
    }
}
