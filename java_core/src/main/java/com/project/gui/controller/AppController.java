package com.project.gui.controller;

import com.project.ai_client.FastApiRestClient;
import com.project.ai_client.IAiClient;
import com.project.datacollection.model.SocialMediaPost;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

public class AppController implements Initializable {

    // Sub-controllers injected by JavaFX FXMLLoader because of fx:id in fx:include
    @FXML private DataCollectionController dataCollectionController;
    @FXML private AnalysisController analysisController;
    @FXML private LogisticsController logisticsController;

    // Shared state
    private List<SocialMediaPost> collectedPosts = new ArrayList<>();
    private IAiClient aiClient;

    public AppController() {
        this.aiClient = new FastApiRestClient("http://127.0.0.1:8000");
    }

    public AppController(IAiClient injectedClient) {
        this.aiClient = injectedClient;
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Initialize shared state across sub-controllers
        if (dataCollectionController != null) {
            dataCollectionController.setSharedState(collectedPosts, aiClient);
            // Connect Data Collection events to Analysis
            dataCollectionController.setOnHistoricalDataLoadedCallback(() -> {
                if (analysisController != null) {
                    analysisController.renderHistoricalCharts(new ArrayList<>());
                }
            });
        }

        if (analysisController != null) {
            analysisController.setSharedState(collectedPosts, aiClient);
            // Connect Analysis events to Logistics
            analysisController.setOnDistressPointsDiscovered(points -> {
                if (logisticsController != null) {
                    logisticsController.updateDistressPoints(points);
                }
            });
        }
    }
}
