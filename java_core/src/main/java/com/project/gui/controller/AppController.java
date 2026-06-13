package com.project.gui.controller;

import com.project.ai_client.*;
import com.project.analysis.*;
import com.project.datacollection.model.SocialMediaPost;
import com.project.datacollection.platform.*;
import com.project.logistics.entities.*;
import com.project.logistics.utils.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.scene.web.WebView;
import javafx.scene.web.WebEngine;
import com.google.gson.*;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.net.URL;
import java.util.*;

public class AppController implements Initializable {

    // ── Data Collection Tab ──
    @FXML private TextField keywordField;
    @FXML private ListView<String> pagesListView;
    @FXML private TextField newPageField;
    @FXML private DatePicker startDatePicker;
    @FXML private DatePicker endDatePicker;
    @FXML private CheckBox facebookCheck;
    @FXML private CheckBox twitterCheck;
    @FXML private Button fetchButton;
    @FXML private Button cancelButton;
    @FXML private HBox loadingBox;
    @FXML private TableView<SocialMediaPost> postsTable;
    @FXML private TableColumn<SocialMediaPost, String> colPostId;
    @FXML private TableColumn<SocialMediaPost, String> colContent;
    @FXML private TableColumn<SocialMediaPost, String> colTimestamp;
    @FXML private Label statusLabel;

    // ── Analysis Tab ──
    @FXML private CheckBox sentimentCheck;
    @FXML private CheckBox damageCheck;
    @FXML private CheckBox reliefCheck;
    @FXML private Button runAnalysisButton;
    @FXML private VBox resultsBox;

    // ── Logistics Tab ──
    @FXML private TextField startLocField;
    @FXML private TextField destLocField;
    @FXML private TableView<Vehicle> vehicleTable;
    @FXML private TableColumn<Vehicle, String> colVehicleId;
    @FXML private TableColumn<Vehicle, Integer> colCapacity;
    @FXML private TableColumn<Vehicle, String> colStatus;
    @FXML private TextArea routeOutput;
    @FXML private WebView mapWebView;

    private List<SocialMediaPost> collectedPosts = new ArrayList<>();
    private IAiClient aiClient; 
    private Thread scrapingThread;

    public AppController() {
        this.aiClient = new FastApiRestClient("http://127.0.0.1:8000");
    }

    /**
     * @param injectedClient choose specific ai_client for the app
     * exp: IAiClient myFastApiClient = new FastApiRestClient("http://127.0.0.1:8000");
     */
    public AppController(IAiClient injectedClient) {
        this.aiClient = injectedClient;
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupPostsTable();
        setupVehicleTable();
        loadMockVehicles();
        if (pagesListView != null) {
            pagesListView.setItems(FXCollections.observableArrayList(
                "https://www.facebook.com/phongchongthientaivn"
            ));
        }
        if (mapWebView != null) {
            WebEngine webEngine = mapWebView.getEngine();
            URL url = getClass().getResource("/com/project/gui/resources/html/map.html");
            if (url != null) {
                webEngine.load(url.toExternalForm());
            } else {
                System.err.println("[!] Could not find map.html resource!");
            }
        }
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
    private void onAddPage() {
        String newPage = newPageField.getText().trim();
        if (!newPage.isEmpty()) {
            if (!pagesListView.getItems().contains(newPage)) {
                pagesListView.getItems().add(newPage);
            }
            newPageField.clear();
        }
    }

    @FXML
    private void onRemovePage() {
        String selected = pagesListView.getSelectionModel().getSelectedItem();
        if (selected != null) {
            pagesListView.getItems().remove(selected);
        }
    }

    @FXML
    private void onCancelScraping() {
        if (scrapingThread != null && scrapingThread.isAlive()) {
            statusLabel.setText("Cancelling scraping operation... Please wait.");
            FacebookScraper.quitActiveDriver();
            scrapingThread.interrupt();
            
            fetchButton.setDisable(false);
            loadingBox.setVisible(false);
            loadingBox.setManaged(false);
            cancelButton.setVisible(false);
            cancelButton.setManaged(false);
            statusLabel.setText("Scraping cancelled by user.");
        }
    }

    @FXML
    private void onFetchPosts() {
        String keyword = keywordField.getText().trim();
        List<String> pagesList = (pagesListView != null) ? pagesListView.getItems() : new ArrayList<>();

        if (pagesList.isEmpty()) {
            showAlert("Validation", "Please add at least one target page/link.");
            return;
        }

        String customLinks = String.join(",", pagesList);
        System.setProperty("CUSTOM_CRAWL_PAGES", customLinks);

        // Disable fetch button and show loading indicator & cancel button
        fetchButton.setDisable(true);
        loadingBox.setVisible(true);
        loadingBox.setManaged(true);
        cancelButton.setVisible(true);
        cancelButton.setManaged(true);
        statusLabel.setText("Scraping live posts in progress... Please wait.");

        Thread t = new Thread(() -> {
            List<SocialMediaPost> tempPosts = new ArrayList<>();
            PlatformSetting setting = new PlatformSetting();
            Date start = new Date();
            Date end = new Date();
            String errorMsg = null;

            try {
                if (facebookCheck.isSelected()) {
                    tempPosts.addAll(setting.getPlatform("facebook").fetchPost(keyword, start, end));
                }
                if (twitterCheck.isSelected()) {
                    tempPosts.addAll(setting.getPlatform("twitter").fetchPost(keyword, start, end));
                }
            } catch (Exception e) {
                e.printStackTrace();
                errorMsg = e.getMessage();
            }

            final List<SocialMediaPost> finalPosts = tempPosts;
            final String finalError = errorMsg;

            javafx.application.Platform.runLater(() -> {
                collectedPosts.clear();
                collectedPosts.addAll(finalPosts);
                postsTable.setItems(FXCollections.observableArrayList(collectedPosts));

                // Reset loading UI
                fetchButton.setDisable(false);
                loadingBox.setVisible(false);
                loadingBox.setManaged(false);
                cancelButton.setVisible(false);
                cancelButton.setManaged(false);

                if (finalError != null) {
                    if (finalError.contains("InterruptedException") || finalError.contains("sleep interrupted") || finalError.contains("session deleted")) {
                        statusLabel.setText("Scraping cancelled.");
                    } else {
                        statusLabel.setText("Error: " + finalError);
                        showAlert("Scraping Error", "Live scraping failed: " + finalError);
                    }
                } else {
                    statusLabel.setText("Fetched " + collectedPosts.size() + " posts.");
                }
            });
        });
        
        t.setContextClassLoader(getClass().getClassLoader());
        scrapingThread = t;
        t.start();
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

        // Disable run button and show dynamic progress indicator
        runAnalysisButton.setDisable(true);
        resultsBox.getChildren().clear();
        
        VBox loadingContainer = new VBox(12);
        loadingContainer.setAlignment(Pos.CENTER);
        loadingContainer.setPadding(new Insets(40));
        ProgressIndicator progressIndicator = new ProgressIndicator();
        Label progressLabel = new Label("Analyzing posts using AI engine... Please wait.");
        progressLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: -color-secondary; -fx-font-weight: bold;");
        loadingContainer.getChildren().addAll(progressIndicator, progressLabel);
        resultsBox.getChildren().add(loadingContainer);

        Thread t = new Thread(() -> {
            try {
                final List<AnalysisResult> results = manager.runAll(collectedPosts, aiClient);
                javafx.application.Platform.runLater(() -> {
                    runAnalysisButton.setDisable(false);
                    renderDetailedReport(results);
                });
            } catch (Exception e) {
                e.printStackTrace();
                javafx.application.Platform.runLater(() -> {
                    runAnalysisButton.setDisable(false);
                    resultsBox.getChildren().clear();
                    Label errLabel = new Label("Analysis Error: " + e.getMessage());
                    errLabel.setStyle("-fx-text-fill: #c0392b; -fx-font-weight: bold;");
                    resultsBox.getChildren().add(errLabel);
                    showAlert("Analysis Error", "Failed to run analysis: " + e.getMessage());
                });
            }
        });
        
        t.setContextClassLoader(getClass().getClassLoader());
        t.start();
    }

    @SuppressWarnings("unchecked")
    private void renderDetailedReport(List<AnalysisResult> results) {
        resultsBox.getChildren().clear();
        resultsBox.setSpacing(16);

        // Header Panel
        VBox header = new VBox(4);
        header.setPadding(new Insets(10, 10, 15, 10));
        header.setStyle("-fx-border-color: -color-border; -fx-border-width: 0 0 1 0;");
        Label title = new Label("📋 AI Social Media Analysis Report");
        title.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: -color-text;");
        Label subtitle = new Label("Generated on: " + new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()) 
            + "  •  Analyzed " + collectedPosts.size() + " posts");
        subtitle.setStyle("-fx-font-size: 12px; -fx-text-fill: -color-muted;");
        header.getChildren().addAll(title, subtitle);
        resultsBox.getChildren().add(header);

        for (AnalysisResult res : results) {
            VBox card = new VBox(10);
            card.setPadding(new Insets(14));
            card.setStyle("-fx-background-color: white; -fx-border-color: #d8dce4; -fx-border-radius: 6; -fx-background-radius: 6;");
            
            Label cardTitle = new Label();
            cardTitle.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: -color-text;");
            card.getChildren().add(cardTitle);

            String name = res.getAnalyzerName();
            if ("UrgencyCategorizer".equalsIgnoreCase(name)) {
                cardTitle.setText("🚨 Urgency & Criticality Distribution");
                
                Map<String, Integer> urgencyCount = (Map<String, Integer>) res.get("urgencyCount");
                if (urgencyCount != null && !urgencyCount.isEmpty()) {
                    int total = urgencyCount.values().stream().mapToInt(Integer::intValue).sum();
                    
                    GridPane grid = new GridPane();
                    grid.setHgap(10);
                    grid.setVgap(8);
                    
                    int row = 0;
                    for (Map.Entry<String, Integer> entry : urgencyCount.entrySet()) {
                        String level = entry.getKey();
                        int count = entry.getValue();
                        double pct = total > 0 ? (double) count / total : 0.0;
                        
                        Label lvlLabel = new Label(level.substring(0, 1).toUpperCase() + level.substring(1));
                        lvlLabel.setMinWidth(70);
                        lvlLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: -color-text;");
                        
                        ProgressBar bar = new ProgressBar(pct);
                        bar.setMaxWidth(Double.MAX_VALUE);
                        GridPane.setHgrow(bar, Priority.ALWAYS);
                        
                        // Color-coded bar
                        if ("critical".equalsIgnoreCase(level)) {
                            bar.setStyle("-fx-accent: #c0392b;");
                        } else if ("high".equalsIgnoreCase(level)) {
                            bar.setStyle("-fx-accent: #e67e22;");
                        } else if ("medium".equalsIgnoreCase(level)) {
                            bar.setStyle("-fx-accent: #f1c40f;");
                        } else {
                            bar.setStyle("-fx-accent: #2ecc71;");
                        }
                        
                        Label countLabel = new Label(count + " (" + (int)(pct * 100) + "%)");
                        countLabel.setMinWidth(60);
                        countLabel.setStyle("-fx-text-fill: -color-muted;");
                        
                        grid.add(lvlLabel, 0, row);
                        grid.add(bar, 1, row);
                        grid.add(countLabel, 2, row);
                        row++;
                    }
                    card.getChildren().add(grid);
                } else {
                    card.getChildren().add(new Label("No urgency data available."));
                }
                
            } else if ("ReliefSentimentAnalyzer".equalsIgnoreCase(name)) {
                cardTitle.setText("📦 Resource & Relief Demand Frequencies");
                
                Map<String, Integer> itemDemand = (Map<String, Integer>) res.get("itemDemand");
                if (itemDemand != null && !itemDemand.isEmpty()) {
                    int total = itemDemand.values().stream().mapToInt(Integer::intValue).sum();
                    
                    GridPane grid = new GridPane();
                    grid.setHgap(10);
                    grid.setVgap(8);
                    
                    int row = 0;
                    for (Map.Entry<String, Integer> entry : itemDemand.entrySet()) {
                        String item = entry.getKey();
                        int count = entry.getValue();
                        double pct = total > 0 ? (double) count / total : 0.0;
                        
                        String emoji = "";
                        if ("food".equalsIgnoreCase(item)) emoji = " 🍞";
                        else if ("water".equalsIgnoreCase(item)) emoji = " 💧";
                        else if ("medical".equalsIgnoreCase(item)) emoji = " 💊";
                        else if ("shelter".equalsIgnoreCase(item)) emoji = " ⛺";
                        else if ("rescue".equalsIgnoreCase(item)) emoji = " 🛟";
                        
                        Label itemLabel = new Label(item.substring(0, 1).toUpperCase() + item.substring(1) + emoji);
                        itemLabel.setMinWidth(90);
                        itemLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: -color-text;");
                        
                        ProgressBar bar = new ProgressBar(pct);
                        bar.setMaxWidth(Double.MAX_VALUE);
                        GridPane.setHgrow(bar, Priority.ALWAYS);
                        bar.setStyle("-fx-accent: -color-secondary;");
                        
                        Label countLabel = new Label(count + " requests");
                        countLabel.setMinWidth(80);
                        countLabel.setStyle("-fx-text-fill: -color-muted;");
                        
                        grid.add(itemLabel, 0, row);
                        grid.add(bar, 1, row);
                        grid.add(countLabel, 2, row);
                        row++;
                    }
                    card.getChildren().add(grid);
                } else {
                    card.getChildren().add(new Label("No relief demand data available."));
                }
                
            } else if ("SentimentOverTime".equalsIgnoreCase(name)) {
                cardTitle.setText("🎭 Sentiment & Emotion Analysis");
                
                Map<String, Double> timeline = (Map<String, Double>) res.get("negativeScoreTimeline");
                Map<String, String> emotions = (Map<String, String>) res.get("dominantEmotions");
                
                if (timeline != null && !timeline.isEmpty()) {
                    // Calculate overall stats
                    double sum = 0.0;
                    for (double score : timeline.values()) {
                        sum += score;
                    }
                    double avgNegativity = sum / timeline.size();
                    
                    VBox stats = new VBox(6);
                    HBox avgRow = new HBox(10);
                    avgRow.setAlignment(Pos.CENTER_LEFT);
                    Label avgLabel = new Label("Average Negativity Index:");
                    avgLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: -color-text;");
                    ProgressBar avgBar = new ProgressBar(avgNegativity);
                    avgBar.setMinWidth(150);
                    if (avgNegativity > 0.7) {
                        avgBar.setStyle("-fx-accent: #c0392b;");
                    } else if (avgNegativity > 0.4) {
                        avgBar.setStyle("-fx-accent: #e67e22;");
                    } else {
                        avgBar.setStyle("-fx-accent: #2ecc71;");
                    }
                    Label avgVal = new Label(String.format("%.1f%%", avgNegativity * 100));
                    avgVal.setStyle("-fx-font-weight: bold; -fx-text-fill: -color-text;");
                    avgRow.getChildren().addAll(avgLabel, avgBar, avgVal);
                    
                    Label detailsTitle = new Label("Post Details:");
                    detailsTitle.setStyle("-fx-font-weight: bold; -fx-padding: 8 0 2 0; -fx-text-fill: -color-text;");
                    
                    VBox list = new VBox(6);
                    for (String postId : timeline.keySet()) {
                        double neg = timeline.get(postId);
                        String emo = emotions.get(postId);
                        if (emo == null) emo = "unknown";
                        
                        String emoji = "😐";
                        if ("anger".equalsIgnoreCase(emo)) emoji = "😡";
                        else if ("sadness".equalsIgnoreCase(emo)) emoji = "😢";
                        else if ("fear".equalsIgnoreCase(emo)) emoji = "😨";
                        else if ("joy".equalsIgnoreCase(emo)) emoji = "😊";
                        else if ("disgust".equalsIgnoreCase(emo)) emoji = "🤢";
                        else if ("surprise".equalsIgnoreCase(emo)) emoji = "😮";
                        else if ("anxiety".equalsIgnoreCase(emo)) emoji = "😰";
                        
                        HBox itemRow = new HBox(12);
                        itemRow.setAlignment(Pos.CENTER_LEFT);
                        itemRow.setPadding(new Insets(4, 8, 4, 8));
                        itemRow.setStyle("-fx-background-color: #f8f9fa; -fx-background-radius: 4; -fx-border-color: #e9ecef; -fx-border-radius: 4;");
                        
                        Label idLbl = new Label("Post ID: " + (postId.length() > 14 ? postId.substring(0, 14) + "..." : postId));
                        idLbl.setMinWidth(150);
                        idLbl.setStyle("-fx-font-family: monospace; -fx-text-fill: -color-muted;");
                        
                        Label emoLbl = new Label(emoji + " " + emo.substring(0, 1).toUpperCase() + emo.substring(1));
                        emoLbl.setMinWidth(100);
                        emoLbl.setStyle("-fx-font-weight: bold; -fx-text-fill: -color-text;");
                        
                        Label negLbl = new Label("Negativity: " + (int)(neg * 100) + "%");
                        negLbl.setStyle("-fx-text-fill: " + (neg > 0.6 ? "#c0392b" : "-color-text") + ";");
                        
                        itemRow.getChildren().addAll(idLbl, emoLbl, negLbl);
                        list.getChildren().add(itemRow);
                    }
                    
                    stats.getChildren().addAll(avgRow, detailsTitle, list);
                    card.getChildren().add(stats);
                } else {
                    card.getChildren().add(new Label("No sentiment timeline data available."));
                }
            } else {
                cardTitle.setText("📊 " + name);
                card.getChildren().add(new Label(res.toString()));
            }

            resultsBox.getChildren().add(card);
        }
    }

    @FXML
    private void onFindRoute() {
        String startStr = startLocField.getText().trim();
        String destStr = destLocField.getText().trim();

        if (startStr.isEmpty() || destStr.isEmpty()) {
            showAlert("Validation", "Please enter start and destination.");
            return;
        }

        routeOutput.setText("Resolving locations and drawing route...");

        // Perform geocoding (Nominatim API query is a network call, we run it on background thread to prevent GUI lockup)
        Thread routeThread = new Thread(() -> {
            Location start = geocodeAddressWithFallback(startStr);
            Location dest = geocodeAddressWithFallback(destStr);

            javafx.application.Platform.runLater(() -> {
                RouteFinder finder = new RouteFinder();
                List<Location> route = finder.AStarRouteFinder(start, dest);

                StringBuilder sb = new StringBuilder("Route Coordinates:\n");
                sb.append(String.format("  Start: %s (%.6f, %.6f)%n", start.getAddress(), start.getLatitude(), start.getLongitude()));
                sb.append(String.format("  Dest:  %s (%.6f, %.6f)%n%n", dest.getAddress(), dest.getLatitude(), dest.getLongitude()));
                sb.append("Visual routing lines have been plotted on the OpenStreetMap view.");
                routeOutput.setText(sb.toString());

                // Execute JavaScript call to Leaflet map
                if (mapWebView != null) {
                    try {
                        String script = String.format(Locale.US, "setRoute(%f, %f, %f, %f, '%s', '%s');",
                            start.getLatitude(), start.getLongitude(),
                            dest.getLatitude(), dest.getLongitude(),
                            start.getAddress().replace("'", "\\'"),
                            dest.getAddress().replace("'", "\\'")
                        );
                        mapWebView.getEngine().executeScript(script);
                    } catch (Exception e) {
                        System.err.println("[!] Failed to call Leaflet JS: " + e.getMessage());
                    }
                }
            });
        });
        routeThread.setContextClassLoader(getClass().getClassLoader());
        routeThread.start();
    }

    private Location geocodeAddress(String address) {
        try {
            HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .followRedirects(HttpClient.Redirect.ALWAYS)
                .build();
                
            String encodedAddr = java.net.URLEncoder.encode(address, "UTF-8");
            String queryUrl = "https://nominatim.openstreetmap.org/search?q=" + encodedAddr + "&format=json&limit=1";
            
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(queryUrl))
                .header("User-Agent", "DisasterReliefSystem/1.0 (muffin@example.com)") // required by Nominatim
                .timeout(Duration.ofSeconds(6))
                .GET()
                .build();
                
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                String body = response.body();
                JsonArray array = JsonParser.parseString(body).getAsJsonArray();
                if (array.size() > 0) {
                    JsonObject obj = array.get(0).getAsJsonObject();
                    double lat = obj.get("lat").getAsDouble();
                    double lon = obj.get("lon").getAsDouble();
                    String displayName = obj.get("display_name").getAsString();
                    return new Location(lat, lon, displayName);
                }
            }
        } catch (Exception e) {
            System.err.println("[!] Nominatim Geocoding API failed for address '" + address + "': " + e.getMessage());
        }
        return null;
    }

    private Location geocodeAddressWithFallback(String address) {
        Location loc = geocodeAddress(address);
        if (loc != null) {
            return loc;
        }
        
        // Fallback geocoding dictionary for major Vietnam locations
        String lower = address.toLowerCase();
        if (lower.contains("hà nội") || lower.contains("hanoi")) {
            return new Location(21.028511, 105.804817, "Hà Nội, Việt Nam (Local Fallback)");
        } else if (lower.contains("đà nẵng") || lower.contains("da nang")) {
            return new Location(16.047079, 108.206230, "Đà Nẵng, Việt Nam (Local Fallback)");
        } else if (lower.contains("quảng trị") || lower.contains("quang tri")) {
            return new Location(16.742491, 107.184914, "Quảng Trị, Việt Nam (Local Fallback)");
        } else if (lower.contains("đắk lắk") || lower.contains("dak lak") || lower.contains("đắc lắc")) {
            return new Location(12.686121, 108.016359, "Đắk Lắk, Việt Nam (Local Fallback)");
        } else if (lower.contains("hồ chí minh") || lower.contains("ho chi minh") || lower.contains("hcm") || lower.contains("sài gòn") || lower.contains("sai gon")) {
            return new Location(10.823099, 106.629664, "TP. Hồ Chí Minh, Việt Nam (Local Fallback)");
        }
        
        // Final central fallback
        return new Location(14.0583, 108.2772, address + " (Fallback Central Vietnam)");
    }

    private void showAlert(String title, String msg) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION, msg, ButtonType.OK);
        alert.setTitle(title);
        alert.showAndWait();
    }
}
