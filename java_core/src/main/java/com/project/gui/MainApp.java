package com.disaster;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

public class MainApp extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        FXMLLoader loader = new FXMLLoader(
            getClass().getResource("/com/disaster/gui/fxml/MainView.fxml")
        );
        Scene scene = new Scene(loader.load(), 1280, 800);
        scene.getStylesheets().add(
            getClass().getResource("/com/disaster/gui/css/style.css").toExternalForm()
        );
        primaryStage.setTitle("Disaster Relief System");
        primaryStage.setScene(scene);
        primaryStage.setMinWidth(1000);
        primaryStage.setMinHeight(700);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
