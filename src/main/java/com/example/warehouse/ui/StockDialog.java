package com.example.warehouse.ui;

import com.example.warehouse.model.Product;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;

import java.util.Optional;
import java.util.function.Supplier;

public final class StockDialog {
    private StockDialog() {
    }

    public record Booking(String type, int quantity, String note) {
        public int delta() {
            return "OUT".equals(type) ? -quantity : quantity;
        }
    }

    public static Optional<Booking> show(Product p) {
        Dialog<Booking> dlg = new Dialog<>();
        dlg.setTitle("Book stock");
        dlg.setHeaderText(p.getSku() + " - " + p.getName() + "  (current: " + p.getQuantity() + ")");
        dlg.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        ChoiceBox<String> type = new ChoiceBox<>();
        type.getItems().addAll("IN", "OUT");
        type.setValue("IN");
        TextField qty = new TextField("1");
        TextField note = new TextField();
        note.setPromptText("e.g. delivery note, order number");
        Label error = new Label();
        error.setStyle("-fx-text-fill: red;");

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(8);
        grid.setPadding(new Insets(15));
        grid.add(new Label("Type"), 0, 0);
        grid.add(type, 1, 0);
        grid.add(new Label("Quantity"), 0, 1);
        grid.add(qty, 1, 1);
        grid.add(new Label("Note"), 0, 2);
        grid.add(note, 1, 2);
        grid.add(error, 0, 3, 2, 1);
        GridPane.setHgrow(note, Priority.ALWAYS);
        dlg.getDialogPane().setContent(grid);

        Supplier<Booking> build = () -> {
            int q;
            try {
                q = Integer.parseInt(qty.getText().trim());
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Quantity must be a whole number.");
            }
            if (q <= 0) throw new IllegalArgumentException("Quantity must be greater than 0.");
            if ("OUT".equals(type.getValue()) && q > p.getQuantity()) {
                throw new IllegalArgumentException("Only " + p.getQuantity() + " in stock.");
            }
            return new Booking(type.getValue(), q, note.getText().trim());
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
