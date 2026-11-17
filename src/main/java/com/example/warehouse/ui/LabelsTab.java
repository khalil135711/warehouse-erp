package com.example.warehouse.ui;

import com.example.warehouse.db.DataAccessException;
import com.example.warehouse.db.WarehouseRepository;
import com.example.warehouse.label.LabelRenderer;
import com.example.warehouse.label.LabelSpec;
import com.example.warehouse.model.Product;
import javafx.embed.swing.SwingFXUtils;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.print.PageLayout;
import javafx.print.PrinterJob;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.SnapshotParameters;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.transform.Scale;
import javafx.scene.transform.Transform;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import javafx.util.StringConverter;

import javax.imageio.ImageIO;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Optional;

public class LabelsTab extends Tab {

    private static final String[] PRESET_NAMES = {
            "100 x 150 mm (shipping)", "100 x 100 mm", "100 x 50 mm", "62 x 29 mm (small)"};
    private static final double[][] PRESETS = {{100, 150, 11}, {100, 100, 10}, {100, 50, 9}, {62, 29, 7}};

    private final WarehouseRepository repo;
    private final Events events;

    private final ComboBox<String> preset = new ComboBox<>();
    private final TextField width = new TextField("100");
    private final TextField height = new TextField("150");
    private final TextField fontSize = new TextField("11");
    private final TextField title = new TextField();
    private final TextField sender = new TextField("Example GmbH, Sample Street 1, 12345 Sample City");
    private final TextField recipient = new TextField("Max Mustermann");
    private final TextField street = new TextField("Musterweg 12");
    private final TextField zipCity = new TextField("12345 Musterstadt");
    private final TextField country = new TextField("Germany");
    private final TextField reference = new TextField("ORD-0001");
    private final TextField weight = new TextField("1.2 kg");
    private final TextField contents = new TextField();
    private final TextField note = new TextField();
    private final TextField barcodeText = new TextField();
    private final CheckBox showSender = new CheckBox("Show sender");
    private final CheckBox showBarcode = new CheckBox("Show barcode");
    private final CheckBox showBorder = new CheckBox("Show border");
    private final Label logoLabel = new Label("no logo");
    private String logoPath = "";

    private final ComboBox<Product> productBox = new ComboBox<>();
    private final TextField qty = new TextField("1");
    private final CheckBox bookOut = new CheckBox("Book stock out after printing");
    private final ComboBox<String> templates = new ComboBox<>();

    private final StackPane previewHolder = new StackPane();
    private boolean updating = false;
    private int row = 0;

    public LabelsTab(WarehouseRepository repo, Events events) {
        super("Labels");
        this.repo = repo;
        this.events = events;
        setClosable(false);

        showSender.setSelected(true);
        showBarcode.setSelected(true);
        showBorder.setSelected(true);
        preset.getItems().addAll(PRESET_NAMES);
        preset.setMaxWidth(Double.MAX_VALUE);
        productBox.setMaxWidth(Double.MAX_VALUE);
        templates.setMaxWidth(Double.MAX_VALUE);
        productBox.setPromptText("(none)");
        productBox.setConverter(new StringConverter<>() {
            @Override
            public String toString(Product p) {
                return p == null ? "" : p.getSku() + " - " + p.getName() + " (" + p.getQuantity() + ")";
            }

            @Override
            public Product fromString(String s) {
                return null;
            }
        });

        // placeholders
        recipient.setPromptText("Recipient name");
        contents.setPromptText("What is in the parcel");
        note.setPromptText("e.g. FRAGILE - THIS SIDE UP");
        barcodeText.setPromptText("empty = use reference");
        title.setPromptText("e.g. PRIORITY");

        for (TextField tf : List.of(width, height, fontSize, title, sender, recipient, street, zipCity,
                country, reference, weight, contents, note, barcodeText)) {
            tf.textProperty().addListener((o, a, b) -> updatePreview());
        }
        for (CheckBox cb : List.of(showSender, showBarcode, showBorder)) {
            cb.selectedProperty().addListener((o, a, b) -> updatePreview());
        }
        preset.getSelectionModel().selectedIndexProperty().addListener((o, a, b) -> {
            int i = b.intValue();
            if (i >= 0) {
                width.setText(fmt(PRESETS[i][0]));
                height.setText(fmt(PRESETS[i][1]));
                fontSize.setText(fmt(PRESETS[i][2]));
            }
        });
        productBox.valueProperty().addListener((o, a, b) -> fillContentsFromProduct());
        qty.textProperty().addListener((o, a, b) -> fillContentsFromProduct());

        // ---- form
        GridPane form = new GridPane();
        form.setHgap(10);
        form.setVgap(6);
        form.setPadding(new Insets(12));

        Button load = new Button("Load");
        Button save = new Button("Save as...");
        Button del = new Button("Delete");
        load.setOnAction(e -> loadTemplate());
        save.setOnAction(e -> saveTemplate());
        del.setOnAction(e -> deleteTemplate());
        HBox tplButtons = new HBox(6, load, save, del);

        Button logoBtn = new Button("Choose logo...");
        Button logoClear = new Button("Clear");
        logoBtn.setOnAction(e -> chooseLogo());
        logoClear.setOnAction(e -> {
            logoPath = "";
            logoLabel.setText("no logo");
            updatePreview();
        });
        HBox logoBox = new HBox(6, logoBtn, logoClear, logoLabel);
        logoBox.setAlignment(Pos.CENTER_LEFT);

        section(form, "Templates");
        addRow(form, "Template", templates);
        full(form, tplButtons);
        section(form, "Format");
        addRow(form, "Preset", preset);
        addRow(form, "Width (mm)", width);
        addRow(form, "Height (mm)", height);
        addRow(form, "Font size (pt)", fontSize);
        full(form, showBorder);
        section(form, "Header");
        addRow(form, "Title", title);
        addRow(form, "Sender", sender);
        full(form, showSender);
        full(form, logoBox);
        section(form, "Recipient");
        addRow(form, "Name", recipient);
        addRow(form, "Street", street);
        addRow(form, "ZIP / City", zipCity);
        addRow(form, "Country", country);
        section(form, "Details");
        addRow(form, "Reference", reference);
        addRow(form, "Weight", weight);
        addRow(form, "Contents", contents);
        addRow(form, "Note", note);
        full(form, showBarcode);
        addRow(form, "Barcode text", barcodeText);
        section(form, "Stock");
        addRow(form, "Product", productBox);
        addRow(form, "Quantity", qty);
        full(form, bookOut);

        ScrollPane formScroll = new ScrollPane(form);
        formScroll.setFitToWidth(true);
        formScroll.setPrefWidth(430);
        formScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);

        // ---- preview + actions
        previewHolder.setPadding(new Insets(24));
        previewHolder.setStyle("-fx-background-color: #d8d8d8;");
        ScrollPane previewScroll = new ScrollPane(previewHolder);
        previewScroll.setFitToWidth(true);
        previewScroll.setFitToHeight(true);

        Button print = new Button("Print...");
        Button png = new Button("Export PNG...");
        print.setDefaultButton(false);
        print.setOnAction(e -> print());
        png.setOnAction(e -> exportPng());
        HBox actions = new HBox(8, print, png);
        actions.setPadding(new Insets(10));
        actions.setAlignment(Pos.CENTER_RIGHT);

        BorderPane center = new BorderPane(previewScroll);
        center.setBottom(actions);

        BorderPane root = new BorderPane(center);
        root.setLeft(formScroll);
        setContent(root);

        events.onChange(this::refreshProducts);
        refreshTemplates();
        updatePreview();
    }

    // ------------------------------------------------------------ form helpers

    private void section(GridPane g, String text) {
        Label l = new Label(text);
        l.setStyle("-fx-font-weight: bold; -fx-padding: 8 0 0 0;");
        g.add(l, 0, row++, 2, 1);
    }

    private void addRow(GridPane g, String label, Node n) {
        g.add(new Label(label), 0, row);
        g.add(n, 1, row++);
        GridPane.setHgrow(n, Priority.ALWAYS);
    }

    private void full(GridPane g, Node n) {
        g.add(n, 0, row++, 2, 1);
    }

    private static String fmt(double d) {
        return d == Math.rint(d) ? String.valueOf((long) d) : String.valueOf(d);
    }

    private static double num(TextField tf, double def, double min, double max) {
        try {
            double v = Double.parseDouble(tf.getText().trim().replace(',', '.'));
            return Math.max(min, Math.min(max, v));
        } catch (NumberFormatException e) {
            return def;
        }
    }

    private static int parseQty(TextField tf) {
        try {
            return Integer.parseInt(tf.getText().trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    // --------------------------------------------------------------- spec <-> UI

    private LabelSpec readSpec() {
        LabelSpec s = new LabelSpec();
        s.widthMm = num(width, 100, 20, 300);
        s.heightMm = num(height, 150, 20, 400);
        s.fontSize = num(fontSize, 11, 4, 40);
        s.title = title.getText().trim();
        s.sender = sender.getText().trim();
        s.recipient = recipient.getText().trim();
        s.street = street.getText().trim();
        s.zipCity = zipCity.getText().trim();
        s.country = country.getText().trim();
        s.reference = reference.getText().trim();
        s.weight = weight.getText().trim();
        s.contents = contents.getText().trim();
        s.note = note.getText().trim();
        s.barcodeText = barcodeText.getText().trim();
        s.logoPath = logoPath;
        s.showSender = showSender.isSelected();
        s.showBarcode = showBarcode.isSelected();
        s.showBorder = showBorder.isSelected();
        return s;
    }

    private void applySpec(LabelSpec s) {
        updating = true;
        try {
            preset.getSelectionModel().clearSelection();
            width.setText(fmt(s.widthMm));
            height.setText(fmt(s.heightMm));
            fontSize.setText(fmt(s.fontSize));
            title.setText(s.title);
            sender.setText(s.sender);
            recipient.setText(s.recipient);
            street.setText(s.street);
            zipCity.setText(s.zipCity);
            country.setText(s.country);
            reference.setText(s.reference);
            weight.setText(s.weight);
            contents.setText(s.contents);
            note.setText(s.note);
            barcodeText.setText(s.barcodeText);
            logoPath = s.logoPath;
            logoLabel.setText(logoPath.isBlank() ? "no logo" : new File(logoPath).getName());
            showSender.setSelected(s.showSender);
            showBarcode.setSelected(s.showBarcode);
            showBorder.setSelected(s.showBorder);
        } finally {
            updating = false;
        }
        updatePreview();
    }

    private void updatePreview() {
        if (updating) return;
        Pane label = LabelRenderer.render(readSpec());
        label.setEffect(new DropShadow(8, Color.gray(0, 0.4)));
        previewHolder.getChildren().setAll(new Group(label));
    }

    // ---------------------------------------------------------------- products

    public void refreshProducts() {
        try {
            Product current = productBox.getValue();
            String keep = current == null ? null : current.getSku();
            List<Product> all = repo.findAll("");
            productBox.getItems().setAll(all);
            if (keep != null) {
                all.stream().filter(p -> p.getSku().equals(keep)).findFirst().ifPresent(productBox::setValue);
            }
        } catch (DataAccessException e) {
            Dialogs.error(e.getMessage());
        }
    }

    private void fillContentsFromProduct() {
        Product p = productBox.getValue();
        if (p != null) {
            int q = Math.max(1, parseQty(qty));
            contents.setText(q + " x " + p.getName());
        }
    }

    // --------------------------------------------------------------- templates

    private void refreshTemplates() {
        try {
            String keep = templates.getValue();
            templates.getItems().setAll(repo.templateNames());
            if (keep != null && templates.getItems().contains(keep)) {
                templates.setValue(keep);
            }
        } catch (DataAccessException e) {
            Dialogs.error(e.getMessage());
        }
    }

    private void loadTemplate() {
        String name = templates.getValue();
        if (name == null) {
            Dialogs.info("Choose a template first.");
            return;
        }
        try {
            repo.loadTemplate(name).ifPresent(text -> applySpec(LabelSpec.fromText(text)));
        } catch (DataAccessException e) {
            Dialogs.error(e.getMessage());
        }
    }

    private void saveTemplate() {
        TextInputDialog dlg = new TextInputDialog(templates.getValue() == null ? "" : templates.getValue());
        dlg.setTitle("Save template");
        dlg.setHeaderText(null);
        dlg.setContentText("Template name:");
        Optional<String> name = dlg.showAndWait().map(String::trim).filter(s -> !s.isEmpty());
        name.ifPresent(n -> {
            try {
                repo.saveTemplate(n, readSpec().toText());
                refreshTemplates();
                templates.setValue(n);
            } catch (DataAccessException e) {
                Dialogs.error(e.getMessage());
            }
        });
    }

    private void deleteTemplate() {
        String name = templates.getValue();
        if (name == null) return;
        if (Dialogs.confirm("Delete template \"" + name + "\"?")) {
            try {
                repo.deleteTemplate(name);
                templates.setValue(null);
                refreshTemplates();
            } catch (DataAccessException e) {
                Dialogs.error(e.getMessage());
            }
        }
    }

    private void chooseLogo() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Choose logo");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.gif"));
        File f = fc.showOpenDialog(window());
        if (f != null) {
            logoPath = f.getAbsolutePath();
            logoLabel.setText(f.getName());
            updatePreview();
        }
    }

    // ------------------------------------------------------------ print/export

    private Window window() {
        return getTabPane().getScene().getWindow();
    }

    private void print() {
        LabelSpec spec = readSpec();
        Product product = productBox.getValue();
        int q = parseQty(qty);
        boolean book = bookOut.isSelected();
        if (book) {
            if (product == null) {
                Dialogs.error("Choose a product to book stock out, or untick the option.");
                return;
            }
            if (q <= 0 || q > product.getQuantity()) {
                Dialogs.error("Quantity must be between 1 and " + product.getQuantity() + ".");
                return;
            }
        }

        PrinterJob job = PrinterJob.createPrinterJob();
        if (job == null) {
            Dialogs.error("No printer available.");
            return;
        }
        if (!job.showPrintDialog(window())) {
            return;
        }

        Group out = LabelRenderer.renderForOutput(spec);
        Pane pane = (Pane) out.getChildren().get(0);
        PageLayout layout = job.getJobSettings().getPageLayout();
        double scale = Math.min(1.0, Math.min(
                layout.getPrintableWidth() / pane.getPrefWidth(),
                layout.getPrintableHeight() / pane.getPrefHeight()));
        if (scale < 1.0) {
            pane.getTransforms().add(new Scale(scale, scale));
        }

        if (job.printPage(out)) {
            job.endJob();
            if (book) {
                try {
                    String ref = spec.reference.isBlank() ? "label" : spec.reference;
                    repo.adjustStock(product.getSku(), -q, "OUT", "Shipment " + ref);
                    events.fire();
                } catch (DataAccessException e) {
                    Dialogs.error(e.getMessage());
                }
            }
        } else {
            job.cancelJob();
            Dialogs.error("Printing failed.");
        }
    }

    private void exportPng() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Export label as PNG");
        fc.setInitialFileName("label.png");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("PNG image", "*.png"));
        File f = fc.showSaveDialog(window());
        if (f == null) return;

        Group out = LabelRenderer.renderForOutput(readSpec());
        Pane pane = (Pane) out.getChildren().get(0);
        SnapshotParameters params = new SnapshotParameters();
        params.setTransform(Transform.scale(4, 4));
        params.setFill(Color.WHITE);
        WritableImage img = pane.snapshot(params, null);
        try {
            ImageIO.write(SwingFXUtils.fromFXImage(img, null), "png", f);
        } catch (IOException e) {
            Dialogs.error("Could not write file: " + e.getMessage());
        }
    }
}
