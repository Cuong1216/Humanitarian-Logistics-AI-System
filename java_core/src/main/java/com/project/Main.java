package main.java.com.project;

import main.java.com.project.ai_client.FastApiRestClient;
import main.java.com.project.ai_client.dto.AnalyzeReq;
import main.java.com.project.ai_client.dto.AnalyzeRes;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Main extends Application {
    @Override
    public void start(Stage primaryStage) throws Exception {
        FXMLLoader loader = new FXMLLoader(
            getClass().getResource("main/java/com/project/gui/resources/fxml/MainView.fxml")
        );
        Scene scene = new Scene(loader.load(), 1280, 800);
        scene.getStylesheets().add(
            getClass().getResource("main/java/com/project/gui/resources/css/style.css").toExternalForm()
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
