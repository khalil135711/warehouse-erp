package com.example.warehouse;

import com.example.warehouse.db.Database;
import com.example.warehouse.db.WarehouseRepository;
import com.example.warehouse.ui.Events;
import com.example.warehouse.ui.LabelsTab;
import com.example.warehouse.ui.MovementsTab;
import com.example.warehouse.ui.ProductsTab;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.TabPane;
import javafx.stage.Stage;

public class App extends Application {

    @Override
    public void start(Stage stage) {
        Database.init();
        WarehouseRepository repo = new WarehouseRepository();
        Events events = new Events();

        TabPane tabs = new TabPane(
                new ProductsTab(repo, events),
                new MovementsTab(repo, events),
                new LabelsTab(repo, events));
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

        events.fire();

        stage.setTitle("Warehouse ERP");
        stage.setScene(new Scene(tabs, 1200, 760));
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
