package com.project;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Main {
    public static class App extends Application {
        @Override
        public void start(Stage primaryStage) throws Exception {
            FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/com/project/gui/resources/fxml/MainView.fxml")
            );
            Scene scene = new Scene(loader.load(), 1280, 800);
            scene.getStylesheets().add(
                getClass().getResource("/com/project/gui/resources/css/style.css").toExternalForm()
            );
            primaryStage.setTitle("Hệ thống Phân tích Mạng xã hội & Điều phối Cứu trợ Thiên tai");
            primaryStage.setScene(scene);
            primaryStage.setMinWidth(1000);
            primaryStage.setMinHeight(700);
            primaryStage.show();
        }
    }

    public static void main(String[] args) {
        System.setProperty("jdk.gtk.version", "2");
        Application.launch(App.class, args);
    }
}
