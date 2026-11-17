package com.example.warehouse.label;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.io.File;

/** Builds the JavaFX node tree of a label. 1 mm = 72/25.4 points, matching the printer coordinate system. */
public final class LabelRenderer {

    public static final double MM = 72.0 / 25.4;

    private LabelRenderer() {
    }

    public static Pane render(LabelSpec s) {
        double w = s.widthMm * MM;
        double h = s.heightMm * MM;
        double pad = Math.max(3, Math.min(w, h) * 0.04);
        double inner = w - 2 * pad;
        double fs = s.fontSize;

        VBox box = new VBox(pad * 0.5);
        box.setAlignment(Pos.TOP_LEFT);
        box.setPadding(new Insets(pad));
        box.setPrefSize(w, h);
        box.setMinSize(w, h);
        box.setMaxSize(w, h);
        box.setClip(new Rectangle(w, h));
        box.setStyle("-fx-background-color: white;"
                + (s.showBorder ? "-fx-border-color: black; -fx-border-width: 1.5;" : ""));

        if (!s.title.isBlank()) {
            box.getChildren().add(text(s.title, fs * 1.4, true, inner));
        }

        // sender + logo
        Image logo = null;
        if (!s.logoPath.isBlank()) {
            File f = new File(s.logoPath);
            if (f.isFile()) {
                Image img = new Image(f.toURI().toString(), inner * 0.3, h * 0.12, true, true);
                if (!img.isError()) {
                    logo = img;
                }
            }
        }
        boolean hasSender = s.showSender && !s.sender.isBlank();
        if (hasSender || logo != null) {
            HBox row = new HBox(pad);
            row.setAlignment(Pos.CENTER_LEFT);
            if (logo != null) {
                row.getChildren().add(new ImageView(logo));
            }
            if (hasSender) {
                row.getChildren().add(text("FROM: " + s.sender, fs * 0.8, false, logo != null ? inner * 0.65 : inner));
            }
            box.getChildren().add(row);
            box.getChildren().add(line(inner));
        }

        // recipient
        box.getChildren().add(text("TO:", fs * 0.8, false, inner));
        if (!s.recipient.isBlank()) box.getChildren().add(text(s.recipient, fs * 1.7, true, inner));
        if (!s.street.isBlank()) box.getChildren().add(text(s.street, fs * 1.2, false, inner));
        if (!s.zipCity.isBlank()) box.getChildren().add(text(s.zipCity, fs * 1.4, true, inner));
        if (!s.country.isBlank()) box.getChildren().add(text(s.country.toUpperCase(), fs * 1.2, true, inner));

        // details
        boolean details = !s.reference.isBlank() || !s.weight.isBlank() || !s.contents.isBlank() || !s.note.isBlank();
        if (details) {
            box.getChildren().add(line(inner));
        }
        if (!s.reference.isBlank()) box.getChildren().add(text("Ref: " + s.reference, fs, false, inner));
        if (!s.weight.isBlank()) box.getChildren().add(text("Weight: " + s.weight, fs, false, inner));
        if (!s.contents.isBlank()) box.getChildren().add(text("Contents: " + s.contents, fs, false, inner));
        if (!s.note.isBlank()) box.getChildren().add(text(s.note, fs, true, inner));

        // spacer + barcode
        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);
        box.getChildren().add(spacer);

        String code = s.barcodeText.isBlank() ? s.reference : s.barcodeText;
        if (s.showBarcode && !code.isBlank()) {
            Canvas bc = Code39.draw(code, inner, Math.min(h * 0.18, 20 * MM));
            Label caption = text(Code39.sanitize(code), fs * 0.9, false, inner);
            caption.setAlignment(Pos.CENTER);
            caption.setMaxWidth(inner);
            caption.setMinWidth(inner);
            box.getChildren().addAll(bc, caption);
        }
        return box;
    }

    /** A label inside a Scene with CSS and layout applied, ready for printing or snapshots. */
    public static Group renderForOutput(LabelSpec s) {
        Pane pane = render(s);
        Group g = new Group(pane);
        new Scene(g);
        pane.applyCss();
        pane.layout();
        return g;
    }

    private static Label text(String value, double size, boolean bold, double maxWidth) {
        Label l = new Label(value);
        l.setWrapText(true);
        l.setMaxWidth(maxWidth);
        l.setMinHeight(Region.USE_PREF_SIZE);
        l.setFont(Font.font("Arial", bold ? FontWeight.BOLD : FontWeight.NORMAL, size));
        l.setTextFill(Color.BLACK);
        return l;
    }

    private static Region line(double width) {
        Region r = new Region();
        r.setPrefSize(width, 1);
        r.setMinHeight(1);
        r.setMaxHeight(1);
        r.setStyle("-fx-background-color: black;");
        return r;
    }
}
