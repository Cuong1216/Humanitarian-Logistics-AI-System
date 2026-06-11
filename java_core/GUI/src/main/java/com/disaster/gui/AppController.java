package com.disaster.gui;

import com.disaster.ai_client.AiClient;
import com.disaster.analysis.*;
import com.disaster.datacollection.model.SocialMediaPost;
import com.disaster.datacollection.platform.*;
import com.disaster.logistics.entities.*;
import com.disaster.logistics.utils.RouteFinder;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;
import java.net.URL;
import java.util.*;

public class AppController implements Initializable {

    // ── Data Collection Tab ──
    @FXML private TextField keywordField;
    @FXML private DatePicker startDatePicker;
    @FXML private DatePicker endDatePicker;
    @FXML private CheckBox facebookCheck;
    @FXML private CheckBox twitterCheck;
    @FXML private TableView<SocialMediaPost> postsTable;
    @FXML private TableColumn<SocialMediaPost, String> colPostId;
    @FXML private TableColumn<SocialMediaPost, String> colContent;
    @FXML private TableColumn<SocialMediaPost, String> colTimestamp;
    @FXML private Label statusLabel;

    // ── Analysis Tab ──
    @FXML private CheckBox sentimentCheck;
    @FXML private CheckBox damageCheck;
    @FXML private CheckBox reliefCheck;
    @FXML private VBox resultsBox;

    // ── Logistics Tab ──
    @FXML private TextField startLocField;
    @FXML private TextField destLocField;
    @FXML private TableView<Vehicle> vehicleTable;
    @FXML private TableColumn<Vehicle, String> colVehicleId;
    @FXML private TableColumn<Vehicle, Integer> colCapacity;
    @FXML private TableColumn<Vehicle, String> colStatus;
    @FXML private TextArea routeOutput;

    private List<SocialMediaPost> collectedPosts = new ArrayList<>();
    private AiClient aiClient = new AiClient("https://api.example.com", "demo-key");

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupPostsTable();
        setupVehicleTable();
        loadMockVehicles();
    }

    private void setupPostsTable() {
        colPostId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colContent.setCellValueFactory(new PropertyValueFactory<>("content"));
        colTimestamp.setCellValueFactory(new PropertyValueFactory<>("timestamp"));
    }

    private void setupVehicleTable() {
        colVehicleId.setCellValueFactory(new PropertyValueFactory<>("vehicleId"));
        colCapacity.setCellValueFactory(new PropertyValueFactory<>("loadCapacity"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
    }

    private void loadMockVehicles() {
        ObservableList<Vehicle> vehicles = FXCollections.observableArrayList(
            new Vehicle("VH-001", 500, "AVAILABLE"),
            new Vehicle("VH-002", 1000, "DISPATCHED"),
            new Vehicle("VH-003", 300, "AVAILABLE"),
            new Vehicle("VH-004", 750, "MAINTENANCE")
        );
        vehicleTable.setItems(vehicles);
    }

    @FXML
    private void onFetchPosts() {
        String keyword = keywordField.getText().trim();
        if (keyword.isEmpty()) {
            showAlert("Validation", "Please enter a keyword.");
            return;
        }

        collectedPosts.clear();
        PlatformSetting setting = new PlatformSetting();
        Date start = new Date();
        Date end = new Date();

        if (facebookCheck.isSelected()) {
            collectedPosts.addAll(setting.getPlatform("facebook").fetchPost(keyword, start, end));
        }
        if (twitterCheck.isSelected()) {
            collectedPosts.addAll(setting.getPlatform("twitter").fetchPost(keyword, start, end));
        }

        postsTable.setItems(FXCollections.observableArrayList(collectedPosts));
        statusLabel.setText("Fetched " + collectedPosts.size() + " posts.");
    }

    @FXML
    private void onRunAnalysis() {
        if (collectedPosts.isEmpty()) {
            showAlert("No Data", "Please fetch posts first.");
            return;
        }

        AnalysisManager manager = new AnalysisManager();
        if (sentimentCheck.isSelected()) manager.addAnalyzer(new SentimentAnalyzeOverTime());
        if (damageCheck.isSelected())   manager.addAnalyzer(new DamageCategorizer());
        if (reliefCheck.isSelected())   manager.addAnalyzer(new ReliefSentimentAnalyzer());

        if (manager.getAnalyzers().isEmpty()) {
            showAlert("No Analyzer", "Please select at least one analyzer.");
            return;
        }

        List<AnalysisResult> results = manager.runAll(collectedPosts, aiClient);
        resultsBox.getChildren().clear();

        for (AnalysisResult res : results) {
            Label lbl = new Label("▶ " + res.toString());
            lbl.setStyle("-fx-font-size: 13px; -fx-padding: 6 0 6 0;");
            resultsBox.getChildren().add(lbl);
            Separator sep = new Separator();
            resultsBox.getChildren().add(sep);
        }
    }

    @FXML
    private void onFindRoute() {
        String startAddr = startLocField.getText().trim();
        String destAddr  = destLocField.getText().trim();

        if (startAddr.isEmpty() || destAddr.isEmpty()) {
            showAlert("Validation", "Please enter start and destination.");
            return;
        }

        Location start = new Location(10.762622, 106.660172, startAddr);
        Location dest  = new Location(10.800000, 106.700000, destAddr);

        RouteFinder finder = new RouteFinder();
        List<Location> route = finder.AStarRouteFinder(start, dest);

        StringBuilder sb = new StringBuilder("Route found (" + route.size() + " waypoints):\n\n");
        for (int i = 0; i < route.size(); i++) {
            sb.append(String.format("  %d. %s%n", i + 1, route.get(i).toString()));
        }
        routeOutput.setText(sb.toString());
    }

    private void showAlert(String title, String msg) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION, msg, ButtonType.OK);
        alert.setTitle(title);
        alert.showAndWait();
    }
}
