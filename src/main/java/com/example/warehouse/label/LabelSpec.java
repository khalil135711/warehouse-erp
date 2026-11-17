package com.example.warehouse.label;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Properties;

/** All user-editable settings of a shipping label. Serialisable as text so it can be stored as a template. */
public class LabelSpec {
    public double widthMm = 100;
    public double heightMm = 150;
    public double fontSize = 11;

    public String title = "";
    public String sender = "";
    public String recipient = "";
    public String street = "";
    public String zipCity = "";
    public String country = "";
    public String reference = "";
    public String weight = "";
    public String contents = "";
    public String note = "";
    public String barcodeText = "";
    public String logoPath = "";

    public boolean showSender = true;
    public boolean showBarcode = true;
    public boolean showBorder = true;

    public String toText() {
        Properties p = new Properties();
        p.setProperty("widthMm", String.valueOf(widthMm));
        p.setProperty("heightMm", String.valueOf(heightMm));
        p.setProperty("fontSize", String.valueOf(fontSize));
        p.setProperty("title", title);
        p.setProperty("sender", sender);
        p.setProperty("recipient", recipient);
        p.setProperty("street", street);
        p.setProperty("zipCity", zipCity);
        p.setProperty("country", country);
        p.setProperty("reference", reference);
        p.setProperty("weight", weight);
        p.setProperty("contents", contents);
        p.setProperty("note", note);
        p.setProperty("barcodeText", barcodeText);
        p.setProperty("logoPath", logoPath);
        p.setProperty("showSender", String.valueOf(showSender));
        p.setProperty("showBarcode", String.valueOf(showBarcode));
        p.setProperty("showBorder", String.valueOf(showBorder));
        StringWriter w = new StringWriter();
        try {
            p.store(w, null);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        return w.toString();
    }

    public static LabelSpec fromText(String text) {
        Properties p = new Properties();
        try {
            p.load(new StringReader(text));
        } catch (IOException e) {
            throw new IllegalArgumentException("Invalid template", e);
        }
        LabelSpec s = new LabelSpec();
        s.widthMm = d(p, "widthMm", s.widthMm);
        s.heightMm = d(p, "heightMm", s.heightMm);
        s.fontSize = d(p, "fontSize", s.fontSize);
        s.title = p.getProperty("title", "");
        s.sender = p.getProperty("sender", "");
        s.recipient = p.getProperty("recipient", "");
        s.street = p.getProperty("street", "");
        s.zipCity = p.getProperty("zipCity", "");
        s.country = p.getProperty("country", "");
        s.reference = p.getProperty("reference", "");
        s.weight = p.getProperty("weight", "");
        s.contents = p.getProperty("contents", "");
        s.note = p.getProperty("note", "");
        s.barcodeText = p.getProperty("barcodeText", "");
        s.logoPath = p.getProperty("logoPath", "");
        s.showSender = Boolean.parseBoolean(p.getProperty("showSender", "true"));
        s.showBarcode = Boolean.parseBoolean(p.getProperty("showBarcode", "true"));
        s.showBorder = Boolean.parseBoolean(p.getProperty("showBorder", "true"));
        return s;
    }

    private static double d(Properties p, String key, double def) {
        try {
            return Double.parseDouble(p.getProperty(key, String.valueOf(def)));
        } catch (NumberFormatException e) {
            return def;
        }
    }
}
