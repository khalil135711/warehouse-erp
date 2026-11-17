package com.example.warehouse.label;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Minimal Code 39 barcode generator (uppercase letters, digits and - . space $ / + %). */
public final class Code39 {

    private static final String CHARS = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ-. $/+%*";

    // n = narrow, w = wide; elements alternate bar, space, bar, ... (9 per character)
    private static final String[] PATTERNS = {
            "nnnwwnwnn", "wnnwnnnnw", "nnwwnnnnw", "wnwwnnnnn", "nnnwwnnnw",   // 0-4
            "wnnwwnnnn", "nnwwwnnnn", "nnnwnnwnw", "wnnwnnwnn", "nnwwnnwnn",   // 5-9
            "wnnnnwnnw", "nnwnnwnnw", "wnwnnwnnn", "nnnnwwnnw", "wnnnwwnnn",   // A-E
            "nnwnwwnnn", "nnnnnwwnw", "wnnnnwwnn", "nnwnnwwnn", "nnnnwwwnn",   // F-J
            "wnnnnnnww", "nnwnnnnww", "wnwnnnnwn", "nnnnwnnww", "wnnnwnnwn",   // K-O
            "nnwnwnnwn", "nnnnnnwww", "wnnnnnwwn", "nnwnnnwwn", "nnnnwnwwn",   // P-T
            "wwnnnnnnw", "nwwnnnnnw", "wwwnnnnnn", "nwnnwnnnw", "wwnnwnnnn",   // U-Y
            "nwwnwnnnn",                                                       // Z
            "nwnnnnwnw", "wwnnnnwnn", "nwwnnnwnn", "nwnwnwnnn",                // - . space $
            "nwnwnnnwn", "nwnnnwnwn", "nnnwnwnwn", "nwnnwnwnn"                 // / + % *
    };

    private Code39() {
    }

    /** Uppercases the text and replaces characters Code 39 cannot encode by '-'. */
    public static String sanitize(String text) {
        StringBuilder sb = new StringBuilder();
        for (char ch : text.toUpperCase(Locale.ROOT).toCharArray()) {
            sb.append(CHARS.indexOf(ch) >= 0 && ch != '*' ? ch : '-');
        }
        return sb.toString();
    }

    public static Canvas draw(String text, double width, double height) {
        String data = "*" + sanitize(text) + "*";
        List<int[]> segments = new ArrayList<>(); // {isBar, units}
        int total = 0;
        for (int ci = 0; ci < data.length(); ci++) {
            String pattern = PATTERNS[CHARS.indexOf(data.charAt(ci))];
            for (int i = 0; i < 9; i++) {
                int units = pattern.charAt(i) == 'w' ? 3 : 1;
                segments.add(new int[]{i % 2 == 0 ? 1 : 0, units});
                total += units;
            }
            if (ci < data.length() - 1) { // inter-character gap
                segments.add(new int[]{0, 1});
                total += 1;
            }
        }
        double unit = width / total;
        Canvas canvas = new Canvas(width, height);
        GraphicsContext g = canvas.getGraphicsContext2D();
        g.setFill(Color.WHITE);
        g.fillRect(0, 0, width, height);
        g.setFill(Color.BLACK);
        double x = 0;
        for (int[] s : segments) {
            double w = s[1] * unit;
            if (s[0] == 1) {
                g.fillRect(x, 0, w, height);
            }
            x += w;
        }
        return canvas;
    }
}
