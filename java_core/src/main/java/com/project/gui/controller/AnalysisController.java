package com.project.gui.controller;

import com.project.ai_client.IAiClient;
import com.project.ai_client.dto.AnalyzeReq;
import com.project.ai_client.dto.AnalyzeRes;
import com.project.analysis.*;
import com.project.datacollection.model.SocialMediaPost;
import com.project.logistics.entities.DistressPoint;
import com.project.logistics.entities.Location;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;

import org.springframework.stereotype.Component;

import java.net.URL;
import java.util.*;
import java.util.function.Consumer;

@Component
public class AnalysisController implements Initializable {

    @FXML private CheckBox sentimentCheck;
    @FXML private CheckBox damageCheck;
    @FXML private CheckBox reliefCheck;
    @FXML private Button runAnalysisButton;
    @FXML private VBox resultsBox;
    @FXML private LineChart<String, Number> negativityLineChart;
    @FXML private StackedBarChart<String, Number> emotionStackedChart;
    @FXML private PieChart severityPieChart;
    @FXML private BarChart<String, Number> damageTypeBarChart;
    @FXML private VBox timelineMilestonesBox;
    @FXML private VBox damageExplanationsBox;
    @FXML private TableView<HistoricalPostRow> historicalPostsTable;
    @FXML private TableColumn<HistoricalPostRow, String> colHistPostId;
    @FXML private TableColumn<HistoricalPostRow, String> colHistPlatform;
    @FXML private TableColumn<HistoricalPostRow, String> colHistContent;
    @FXML private TableColumn<HistoricalPostRow, String> colHistSeverity;
    @FXML private TableColumn<HistoricalPostRow, String> colHistDamageType;

    private List<SocialMediaPost> collectedPosts;
    private IAiClient aiClient;
    private Consumer<List<DistressPoint>> onDistressPointsDiscovered;

    public void setSharedState(List<SocialMediaPost> collectedPosts, IAiClient aiClient) {
        this.collectedPosts = collectedPosts;
        this.aiClient = aiClient;
    }

    public void setOnDistressPointsDiscovered(Consumer<List<DistressPoint>> callback) {
        this.onDistressPointsDiscovered = callback;
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        if (historicalPostsTable != null) {
            colHistPostId.setCellValueFactory(new PropertyValueFactory<>("id"));
            colHistPlatform.setCellValueFactory(new PropertyValueFactory<>("platform"));
            colHistContent.setCellValueFactory(new PropertyValueFactory<>("content"));
            colHistSeverity.setCellValueFactory(new PropertyValueFactory<>("severity"));
            colHistDamageType.setCellValueFactory(new PropertyValueFactory<>("damageType"));

            colHistContent.setCellFactory(tc -> {
                TableCell<HistoricalPostRow, String> cell = new TableCell<>() {
                    @Override
                    protected void updateItem(String item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty || item == null) {
                            setText(null);
                        } else {
                            setText(item);
                        }
                    }
                };
                cell.setWrapText(true);
                return cell;
            });

            colHistDamageType.setCellFactory(tc -> {
                TableCell<HistoricalPostRow, String> cell = new TableCell<>() {
                    @Override
                    protected void updateItem(String item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty || item == null) {
                            setText(null);
                        } else {
                            setText(item);
                            setStyle("-fx-text-fill: -color-secondary; -fx-font-weight: bold;");
                        }
                    }
                };
                cell.setWrapText(true);
                return cell;
            });

            colHistSeverity.setCellFactory(tc -> {
                TableCell<HistoricalPostRow, String> cell = new TableCell<>() {
                    @Override
                    protected void updateItem(String item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty || item == null) {
                            setText(null);
                            setStyle("");
                        } else {
                            setText(item);
                            if (item.contains("Người")) {
                                setStyle("-fx-text-fill: #c0392b; -fx-font-weight: bold;");
                            } else if (item.contains("Nhà cửa")) {
                                setStyle("-fx-text-fill: #d35400; -fx-font-weight: bold;");
                            } else if (item.contains("Cơ sở")) {
                                setStyle("-fx-text-fill: #2980b9; -fx-font-weight: bold;");
                            } else if (item.contains("Nông nghiệp")) {
                                setStyle("-fx-text-fill: #27ae60; -fx-font-weight: bold;");
                            } else if (item.contains("Gián đoạn")) {
                                setStyle("-fx-text-fill: #8e44ad; -fx-font-weight: bold;");
                            } else if (item.contains("Tài sản")) {
                                setStyle("-fx-text-fill: #a04000; -fx-font-weight: bold;");
                            } else {
                                setStyle("-fx-text-fill: -color-muted;");
                            }
                        }
                    }
                };
                cell.setWrapText(true);
                return cell;
            });
        }
    }

    @FXML
    private void onRunAnalysis() {
        if (collectedPosts == null || collectedPosts.isEmpty()) {
            showAlert("Không có dữ liệu", "Vui lòng thu thập bài viết trước.");
            return;
        }

        AnalysisManager manager = new AnalysisManager();
        if (sentimentCheck != null && sentimentCheck.isSelected()) manager.addAnalyzer(new SentimentAnalyzeOverTime());
        if (damageCheck != null && damageCheck.isSelected())   manager.addAnalyzer(new DamageCategorizer());
        if (reliefCheck != null && reliefCheck.isSelected())   manager.addAnalyzer(new ReliefSentimentAnalyzer());

        if (manager.getAnalyzers().isEmpty()) {
            showAlert("Chưa chọn bộ phân tích", "Vui lòng chọn ít nhất một bộ phân tích.");
            return;
        }

        runAnalysisButton.setDisable(true);
        if (resultsBox != null) {
            resultsBox.getChildren().clear();
            VBox loadingContainer = new VBox(12);
            loadingContainer.setAlignment(Pos.CENTER);
            loadingContainer.setPadding(new Insets(40));
            ProgressIndicator progressIndicator = new ProgressIndicator();
            Label progressLabel = new Label("Đang phân tích bài viết bằng công cụ AI... Vui lòng đợi.");
            progressLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: -color-secondary; -fx-font-weight: bold;");
            loadingContainer.getChildren().addAll(progressIndicator, progressLabel);
            resultsBox.getChildren().add(loadingContainer);
        }

        Thread t = new Thread(() -> {
            try {
                final List<AnalysisResult> results = manager.runAll(collectedPosts, aiClient);
                final List<DistressPoint> distressList = new ArrayList<>();
                Map<String, AnalyzeRes> aiResultsCache = new LinkedHashMap<>();
                
                for (SocialMediaPost post : collectedPosts) {
                    try {
                        String preprocessedText = WordPreprocessor.preprocess(post.getContent());
                        AnalyzeReq.PostData postData = new AnalyzeReq.PostData(
                            post.getId() != null ? post.getId() : UUID.randomUUID().toString(),
                            post.getPlatform() != null ? post.getPlatform().toLowerCase() : "facebook",
                            post.getAuthor() != null ? post.getAuthor() : "unknown",
                            preprocessedText,
                            "",
                            "",
                            post.getReactions(),
                            post.getComments(),
                            post.getShareCount()
                        );
                        AnalyzeRes res = aiClient.executeTask("/analyze", new AnalyzeReq(postData), AnalyzeRes.class).join();
                        if (res != null) {
                            aiResultsCache.put(post.getId(), res);
                            if (res.getHumanitarianSignal() != null) {
                                AnalyzeRes.HumanitarianSignal signal = res.getHumanitarianSignal();
                                if (signal.isEmergency() || "critical".equalsIgnoreCase(signal.getUrgency()) || "high".equalsIgnoreCase(signal.getUrgency()) || "medium".equalsIgnoreCase(signal.getUrgency())) {
                                    String locName = "Đà Nẵng"; // default fallback
                                    if (signal.getLocations() != null && !signal.getLocations().isEmpty()) {
                                        locName = signal.getLocations().get(0);
                                    } else {
                                        String contentLower = post.getContent().toLowerCase();
                                        if (contentLower.contains("quảng trị")) locName = "Quảng Trị";
                                        else if (contentLower.contains("cát bà") || contentLower.contains("hải phòng")) locName = "Hải Phòng";
                                        else if (contentLower.contains("yên bái") || contentLower.contains("lục yên")) locName = "Yên Bái";
                                        else if (contentLower.contains("lào cai") || contentLower.contains("bắc hà")) locName = "Lào Cai";
                                        else if (contentLower.contains("thanh hóa") || contentLower.contains("thường xuân")) locName = "Thanh Hóa";
                                    }

                                    // Simple Mock Location fallback since geocodeAddressWithFallback is in LogisticsController
                                    Location resolved = getMockGeocode(locName);
                                    DistressPoint dp = new DistressPoint(
                                        resolved.getLatitude(),
                                        resolved.getLongitude(),
                                        resolved.getAddress(),
                                        post.getId(),
                                        signal.getUrgency(),
                                        signal.getCategories(),
                                        signal.getAffectedPeopleEstimate(),
                                        signal.getRecommendedAction()
                                    );
                                    distressList.add(dp);
                                }
                            }
                        }
                    } catch (Exception ex) {
                        System.err.println("[!] Error extracting distress point: " + ex.getMessage());
                    }
                }

                Platform.runLater(() -> {
                    runAnalysisButton.setDisable(false);
                    renderDetailedReport(results, aiResultsCache);
                    renderHistoricalCharts(results);
                    if (onDistressPointsDiscovered != null) {
                        onDistressPointsDiscovered.accept(distressList);
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> {
                    runAnalysisButton.setDisable(false);
                    if (resultsBox != null) {
                        resultsBox.getChildren().clear();
                        Label errLabel = new Label("Lỗi phân tích: " + e.getMessage());
                        errLabel.setStyle("-fx-text-fill: #c0392b; -fx-font-weight: bold;");
                        resultsBox.getChildren().add(errLabel);
                    }
                    showAlert("Lỗi phân tích", "Chạy phân tích thất bại: " + e.getMessage());
                });
            }
        });
        
        t.setContextClassLoader(getClass().getClassLoader());
        t.start();
    }
    
    // Quick fallback helper since real geocoding moved to logistics
    private Location getMockGeocode(String address) {
        String lower = address.toLowerCase();
        if (lower.contains("hà nội") || lower.contains("hanoi")) return new Location(21.028511, 105.804817, "Hà Nội, Việt Nam (Local Fallback)");
        else if (lower.contains("đà nẵng") || lower.contains("da nang")) return new Location(16.047079, 108.206230, "Đà Nẵng, Việt Nam (Local Fallback)");
        else if (lower.contains("quảng trị") || lower.contains("quang tri")) return new Location(16.742491, 107.184914, "Quảng Trị, Việt Nam (Local Fallback)");
        else if (lower.contains("đắk lắk") || lower.contains("dak lak") || lower.contains("đắc lắc")) return new Location(12.686121, 108.016359, "Đắk Lắk, Việt Nam (Local Fallback)");
        else if (lower.contains("hồ chí minh") || lower.contains("ho chi minh") || lower.contains("hcm") || lower.contains("sài gòn") || lower.contains("sai gon")) return new Location(10.823099, 106.629664, "TP. Hồ Chí Minh, Việt Nam (Local Fallback)");
        else if (lower.contains("yên bái") || lower.contains("yen bai") || lower.contains("lục yên")) return new Location(21.722926, 104.911306, "Huyện Lục Yên, Yên Bái, Việt Nam (Local Fallback)");
        else if (lower.contains("lào cai") || lower.contains("lao cai") || lower.contains("bắc hà")) return new Location(22.485639, 103.970669, "Huyện Bắc Hà, Lào Cai, Việt Nam (Local Fallback)");
        else if (lower.contains("hải phòng") || lower.contains("hai phong") || lower.contains("cát bà")) return new Location(20.865139, 106.683830, "Đảo Cát Bà, Hải Phòng, Việt Nam (Local Fallback)");
        else if (lower.contains("thanh hóa") || lower.contains("thanh hoa") || lower.contains("thường xuân")) return new Location(19.806678, 105.785084, "Thường Xuân, Thanh Hóa, Việt Nam (Local Fallback)");
        return new Location(14.0583, 108.2772, address + " (Fallback Central Vietnam)");
    }

    @SuppressWarnings("unchecked")
    private void renderDetailedReport(List<AnalysisResult> results, Map<String, AnalyzeRes> aiResultsCache) {
        if (resultsBox == null) return;
        resultsBox.getChildren().clear();
        resultsBox.setSpacing(16);

        VBox header = new VBox(4);
        header.setPadding(new Insets(10, 10, 15, 10));
        header.setStyle("-fx-border-color: -color-border; -fx-border-width: 0 0 1 0;");
        Label title = new Label("📋 Báo cáo phân tích mạng xã hội bằng AI");
        title.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: -color-text;");
        Label subtitle = new Label("Tạo lúc: " + new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()) 
            + "  •  Đã phân tích " + (collectedPosts != null ? collectedPosts.size() : 0) + " bài viết");
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
                cardTitle.setText("🚨 Phân bố mức độ khẩn cấp & nghiêm trọng");
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
                        
                        String levelText = level;
                        if ("critical".equalsIgnoreCase(level)) levelText = "Nguy kịch 🚨";
                        else if ("high".equalsIgnoreCase(level)) levelText = "Cao ⚠️";
                        else if ("medium".equalsIgnoreCase(level)) levelText = "Trung bình ⚡";
                        else if ("low".equalsIgnoreCase(level)) levelText = "Thấp ℹ️";

                        Label lvlLabel = new Label(levelText);
                        lvlLabel.setMinWidth(95);
                        lvlLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: -color-text;");
                        
                        ProgressBar bar = new ProgressBar(pct);
                        bar.setMaxWidth(Double.MAX_VALUE);
                        GridPane.setHgrow(bar, Priority.ALWAYS);
                        
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

                    Separator sep = new Separator();
                    sep.setPadding(new Insets(8, 0, 4, 0));
                    card.getChildren().add(sep);

                    VBox evidenceHeader = new VBox(4);
                    Label evidenceTitle = new Label("🚨 Bằng chứng xác thực: Các bài đăng khẩn cấp & có độ ưu tiên cao");
                    evidenceTitle.setStyle("-fx-font-weight: bold; -fx-text-fill: -color-text; -fx-font-size: 13px;");
                    evidenceHeader.getChildren().add(evidenceTitle);
                    card.getChildren().add(evidenceHeader);

                    VBox evidenceContainer = new VBox(8);
                    int evidenceCount = 0;
                    for (SocialMediaPost post : collectedPosts) {
                        try {
                            AnalyzeRes aiRes = aiResultsCache.get(post.getId());
                            if (aiRes != null && aiRes.getHumanitarianSignal() != null) {
                                String urg = aiRes.getHumanitarianSignal().getUrgency();
                                if ("critical".equalsIgnoreCase(urg) || "high".equalsIgnoreCase(urg)) {
                                    VBox evCard = new VBox(4);
                                    evCard.setPadding(new Insets(10));
                                    String bgColor = "critical".equalsIgnoreCase(urg) ? "#fdf2f2" : "#fef9f3";
                                    String borderColor = "critical".equalsIgnoreCase(urg) ? "#f8b4b4" : "#fcd9bd";
                                    evCard.setStyle(String.format("-fx-background-color: %s; -fx-border-color: %s; -fx-border-radius: 4; -fx-background-radius: 4;", bgColor, borderColor));
                                    
                                    String urgTranslated = "critical".equalsIgnoreCase(urg) ? "NGUY KỊCH" : "CAO";
                                    Label evTitle = new Label(String.format("[%s] %s (%s)", urgTranslated, post.getAuthor(), post.getPlatform()));
                                    evTitle.setStyle("-fx-font-weight: bold; -fx-text-fill: -color-text;");
                                    
                                    Label evText = new Label(post.getContent());
                                    evText.setWrapText(true);
                                    evText.setStyle("-fx-font-style: italic; -fx-text-fill: #34495e;");
                                    
                                    Label evAction = new Label("🤖 Đề xuất cứu trợ từ AI: " + aiRes.getHumanitarianSignal().getRecommendedAction());
                                    evAction.setStyle("-fx-font-weight: bold; -fx-text-fill: -color-secondary;");
                                    
                                    evCard.getChildren().addAll(evTitle, evText, evAction);
                                    evidenceContainer.getChildren().add(evCard);
                                    evidenceCount++;
                                }
                            }
                        } catch (Exception e) {}
                    }
                    if (evidenceCount == 0) {
                        Label noEv = new Label("Không tìm thấy bài đăng nào có độ khẩn cấp cao hoặc nguy kịch làm bằng chứng.");
                        noEv.setStyle("-fx-text-fill: -color-muted;");
                        evidenceContainer.getChildren().add(noEv);
                    }
                    card.getChildren().add(evidenceContainer);
                } else {
                    card.getChildren().add(new Label("Không có dữ liệu về mức độ khẩn cấp."));
                }
            } else if ("ReliefSentimentAnalyzer".equalsIgnoreCase(name)) {
                cardTitle.setText("📦 Tần suất nhu cầu tài nguyên & cứu trợ");
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
                        String itemTranslated = item;
                        if ("food".equalsIgnoreCase(item)) { emoji = " 🍞"; itemTranslated = "Lương thực"; }
                        else if ("water".equalsIgnoreCase(item)) { emoji = " 💧"; itemTranslated = "Nước sạch"; }
                        else if ("medical".equalsIgnoreCase(item)) { emoji = " 💊"; itemTranslated = "Thuốc men/Y tế"; }
                        else if ("shelter".equalsIgnoreCase(item)) { emoji = " ⛺"; itemTranslated = "Nhà ở/Bạt che"; }
                        else if ("rescue".equalsIgnoreCase(item)) { emoji = " 🛟"; itemTranslated = "Cứu hộ/Áo phao"; }
                        
                        Label itemLabel = new Label(itemTranslated + emoji);
                        itemLabel.setMinWidth(110);
                        itemLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: -color-text;");
                        
                        ProgressBar bar = new ProgressBar(pct);
                        bar.setMaxWidth(Double.MAX_VALUE);
                        GridPane.setHgrow(bar, Priority.ALWAYS);
                        bar.setStyle("-fx-accent: -color-secondary;");
                        
                        Label countLabel = new Label(count + " yêu cầu");
                        countLabel.setMinWidth(80);
                        countLabel.setStyle("-fx-text-fill: -color-muted;");
                        
                        grid.add(itemLabel, 0, row);
                        grid.add(bar, 1, row);
                        grid.add(countLabel, 2, row);
                        row++;
                    }
                    card.getChildren().add(grid);

                    Separator sep = new Separator();
                    sep.setPadding(new Insets(8, 0, 4, 0));
                    card.getChildren().add(sep);

                    VBox evidenceHeader = new VBox(4);
                    Label evidenceTitle = new Label("📦 Bằng chứng xác thực: Yêu cầu cung cấp nhu yếu phẩm cụ thể");
                    evidenceTitle.setStyle("-fx-font-weight: bold; -fx-text-fill: -color-text; -fx-font-size: 13px;");
                    evidenceHeader.getChildren().add(evidenceTitle);
                    card.getChildren().add(evidenceHeader);

                    VBox evidenceContainer = new VBox(8);
                    int demandCount = 0;
                    for (SocialMediaPost post : collectedPosts) {
                        try {
                            AnalyzeRes aiRes = aiResultsCache.get(post.getId());
                            if (aiRes != null && aiRes.getHumanitarianSignal() != null) {
                                List<String> cats = aiRes.getHumanitarianSignal().getCategories();
                                if (cats != null && !cats.isEmpty()) {
                                    VBox evCard = new VBox(4);
                                    evCard.setPadding(new Insets(10));
                                    evCard.setStyle("-fx-background-color: #f0f9eb; -fx-border-color: #c2e7b0; -fx-border-radius: 4; -fx-background-radius: 4;");
                                    
                                    List<String> translatedCats = new ArrayList<>();
                                    for (String c : cats) {
                                        switch (c.toLowerCase().trim()) {
                                            case "food": translatedCats.add("Lương thực"); break;
                                            case "water": translatedCats.add("Nước sạch"); break;
                                            case "medical": translatedCats.add("Y tế/Thuốc men"); break;
                                            case "shelter": translatedCats.add("Nhà ở/Bạt che"); break;
                                            case "rescue": translatedCats.add("Cứu hộ/Áo phao"); break;
                                            default: translatedCats.add(c);
                                        }
                                    }

                                    Label evTitle = new Label(String.format("Yêu cầu: %s | %s (%s)", translatedCats.toString(), post.getAuthor(), post.getPlatform()));
                                    evTitle.setStyle("-fx-font-weight: bold; -fx-text-fill: -color-text;");
                                    
                                    Label evText = new Label(post.getContent());
                                    evText.setWrapText(true);
                                    evText.setStyle("-fx-font-style: italic; -fx-text-fill: #34495e;");
                                    
                                    Label evAction = new Label(String.format("Đề xuất: %s", aiRes.getHumanitarianSignal().getRecommendedAction()));
                                    evAction.setStyle("-fx-font-weight: bold; -fx-text-fill: #67c23a;");
                                    
                                    evCard.getChildren().addAll(evTitle, evText, evAction);
                                    evidenceContainer.getChildren().add(evCard);
                                    demandCount++;
                                }
                            }
                        } catch (Exception e) {}
                    }
                    if (demandCount == 0) {
                        Label noEv = new Label("Không tìm thấy bài đăng nào yêu cầu cứu trợ cụ thể làm bằng chứng.");
                        noEv.setStyle("-fx-text-fill: -color-muted;");
                        evidenceContainer.getChildren().add(noEv);
                    }
                    card.getChildren().add(evidenceContainer);
                } else {
                    card.getChildren().add(new Label("Không có dữ liệu nhu cầu cứu trợ."));
                }
            } else if ("SentimentOverTime".equalsIgnoreCase(name)) {
                cardTitle.setText("🎭 Phân tích cảm xúc & thái độ");
                Map<String, Double> timeline = (Map<String, Double>) res.get("negativeScoreTimeline");
                Map<String, String> emotions = (Map<String, String>) res.get("dominantEmotions");
                
                if (timeline != null && !timeline.isEmpty()) {
                    double sum = 0.0;
                    for (double score : timeline.values()) sum += score;
                    double avgNegativity = sum / timeline.size();
                    
                    VBox stats = new VBox(6);
                    HBox avgRow = new HBox(10);
                    avgRow.setAlignment(Pos.CENTER_LEFT);
                    Label avgLabel = new Label("Chỉ số tiêu cực trung bình:");
                    avgLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: -color-text;");
                    ProgressBar avgBar = new ProgressBar(avgNegativity);
                    avgBar.setMinWidth(150);
                    if (avgNegativity > 0.7) avgBar.setStyle("-fx-accent: #c0392b;");
                    else if (avgNegativity > 0.4) avgBar.setStyle("-fx-accent: #e67e22;");
                    else avgBar.setStyle("-fx-accent: #2ecc71;");
                    
                    Label avgVal = new Label(String.format("%.1f%%", avgNegativity * 100));
                    avgVal.setStyle("-fx-font-weight: bold; -fx-text-fill: -color-text;");
                    avgRow.getChildren().addAll(avgLabel, avgBar, avgVal);
                    
                    Label detailsTitle = new Label("Chi tiết bài đăng:");
                    detailsTitle.setStyle("-fx-font-weight: bold; -fx-padding: 8 0 2 0; -fx-text-fill: -color-text;");
                    
                    VBox list = new VBox(8);
                    for (String postId : timeline.keySet()) {
                        double neg = timeline.get(postId);
                        String emo = emotions != null ? emotions.get(postId) : "unknown";
                        if (emo == null) emo = "unknown";
                        
                        String emoji = "😐";
                        String emoTranslated = "Bình thường";
                        if ("anger".equalsIgnoreCase(emo)) { emoji = "😡"; emoTranslated = "Giận dữ"; }
                        else if ("sadness".equalsIgnoreCase(emo)) { emoji = "😢"; emoTranslated = "Buồn bã"; }
                        else if ("fear".equalsIgnoreCase(emo)) { emoji = "😨"; emoTranslated = "Lo sợ"; }
                        else if ("joy".equalsIgnoreCase(emo)) { emoji = "😊"; emoTranslated = "Vui vẻ"; }
                        else if ("disgust".equalsIgnoreCase(emo)) { emoji = "🤢"; emoTranslated = "Ghê tởm"; }
                        else if ("surprise".equalsIgnoreCase(emo)) { emoji = "😮"; emoTranslated = "Ngạc nhiên"; }
                        else if ("anxiety".equalsIgnoreCase(emo)) { emoji = "😰"; emoTranslated = "Lo âu"; }
                        
                        VBox itemContainer = new VBox(4);
                        itemContainer.setPadding(new Insets(6, 10, 6, 10));
                        itemContainer.setStyle("-fx-background-color: #f8f9fa; -fx-background-radius: 4; -fx-border-color: #e9ecef; -fx-border-radius: 4;");
                        
                        HBox itemRow = new HBox(12);
                        itemRow.setAlignment(Pos.CENTER_LEFT);
                        
                        Label idLbl = new Label("Mã bài đăng: " + (postId.length() > 14 ? postId.substring(0, 14) + "..." : postId));
                        idLbl.setMinWidth(150);
                        idLbl.setStyle("-fx-font-family: monospace; -fx-text-fill: -color-muted;");
                        
                        Label emoLbl = new Label(emoji + " " + emoTranslated);
                        emoLbl.setMinWidth(100);
                        emoLbl.setStyle("-fx-font-weight: bold; -fx-text-fill: -color-text;");
                        
                        Label negLbl = new Label("Độ tiêu cực: " + (int)(neg * 100) + "%");
                        negLbl.setStyle("-fx-text-fill: " + (neg > 0.6 ? "#c0392b" : "-color-text") + ";");
                        
                        itemRow.getChildren().addAll(idLbl, emoLbl, negLbl);
                        
                        SocialMediaPost actualPost = null;
                        for (SocialMediaPost p : collectedPosts) if (p.getId().equals(postId)) { actualPost = p; break; }
                        
                        Label textSnippet = new Label();
                        textSnippet.setStyle("-fx-font-style: italic; -fx-text-fill: #34495e; -fx-font-size: 11px; -fx-padding: 2 0 0 0;");
                        textSnippet.setWrapText(true);
                        
                        VBox detailBox = new VBox(2);
                        detailBox.setPadding(new Insets(4, 0, 0, 10));
                        detailBox.setStyle("-fx-border-color: #dcdde1; -fx-border-width: 0 0 0 2;");
                        
                        if (actualPost != null) {
                            String c = actualPost.getContent();
                            textSnippet.setText("📝 " + (c.length() > 120 ? c.substring(0, 120) + "..." : c));
                            Label reactionsLbl = new Label("🎭 Phản ứng nổi bật: " + actualPost.getReactionsString());
                            reactionsLbl.setStyle("-fx-font-size: 10px; -fx-text-fill: #7f8c8d;");
                            Label commentsLbl = new Label("💬 Bình luận nổi bật:\n" + actualPost.getCommentsString());
                            commentsLbl.setStyle("-fx-font-size: 10px; -fx-text-fill: #7f8c8d;");
                            commentsLbl.setWrapText(true);
                            detailBox.getChildren().addAll(reactionsLbl, commentsLbl);
                        } else {
                            textSnippet.setText("Không có nội dung");
                        }
                        
                        itemContainer.getChildren().addAll(itemRow, textSnippet, detailBox);
                        list.getChildren().add(itemContainer);
                    }
                    
                    stats.getChildren().addAll(avgRow, detailsTitle, list);
                    card.getChildren().add(stats);
                } else {
                    card.getChildren().add(new Label("Không có dữ liệu tiến trình cảm xúc."));
                }
            } else {
                cardTitle.setText("📊 " + name);
                card.getChildren().add(new Label(res.toString()));
            }

            resultsBox.getChildren().add(card);
        }
    }

    public void renderHistoricalCharts(List<AnalysisResult> results) {
        if (negativityLineChart == null || emotionStackedChart == null || timelineMilestonesBox == null) return;

        negativityLineChart.getData().clear();
        emotionStackedChart.getData().clear();
        timelineMilestonesBox.getChildren().clear();
        if (severityPieChart != null) severityPieChart.getData().clear();
        if (damageTypeBarChart != null) damageTypeBarChart.getData().clear();
        if (damageExplanationsBox != null) damageExplanationsBox.getChildren().clear();
        if (historicalPostsTable != null) historicalPostsTable.getItems().clear();

        if (collectedPosts == null || collectedPosts.isEmpty()) return;

        Map<String, List<SocialMediaPost>> postsByDate = new TreeMap<>();
        for (SocialMediaPost post : collectedPosts) {
            if (post.getTimestamp() != null) {
                String dateStr = post.getTimestamp().toLocalDate().toString();
                postsByDate.computeIfAbsent(dateStr, k -> new ArrayList<>()).add(post);
            }
        }

        if (postsByDate.isEmpty()) {
            timelineMilestonesBox.getChildren().add(new Label("Không có thông tin thời gian để vẽ biểu đồ."));
            return;
        }

        Map<String, Double> postNegativity = new HashMap<>();
        Map<String, String> postEmotion = new HashMap<>();

        if (results != null) {
            for (AnalysisResult res : results) {
                if ("SentimentOverTime".equalsIgnoreCase(res.getAnalyzerName())) {
                    Map<String, Double> timeline = (Map<String, Double>) res.get("negativeScoreTimeline");
                    Map<String, String> emotions = (Map<String, String>) res.get("dominantEmotions");
                    if (timeline != null) postNegativity.putAll(timeline);
                    if (emotions != null) postEmotion.putAll(emotions);
                }
            }
        }

        for (SocialMediaPost post : collectedPosts) {
            if (!postNegativity.containsKey(post.getId())) {
                double neg = 0.5;
                String emo = "neutral";
                String contentLower = post.getContent().toLowerCase();
                if (contentLower.contains("khẩn cấp") || contentLower.contains("cứu") || contentLower.contains("nguy kịch")) {
                    neg = 0.9;
                    emo = "fear";
                } else if (contentLower.contains("ngập") || contentLower.contains("sạt lở") || contentLower.contains("tàn phá") || contentLower.contains("thiệt hại")) {
                    neg = 0.75;
                    emo = "sadness";
                } else if (contentLower.contains("cảm ơn") || contentLower.contains("tuyệt vời") || contentLower.contains("ổn định")) {
                    neg = 0.1;
                    emo = "joy";
                }
                postNegativity.put(post.getId(), neg);
                postEmotion.put(post.getId(), emo);
            }
        }

        List<String> damageCats = Arrays.asList(
            "Người bị ảnh hưởng",
            "Gián đoạn các hoạt động kinh tế sản xuất",
            "Nhà cửa hoặc tòa nhà bị hư hỏng",
            "Tài sản cá nhân bị mất",
            "Cơ sở hạ tầng bị hư hỏng",
            "Nông nghiệp & Vật nuôi bị thiệt hại",
            "Khác (Tin tức chung / Chưa phân loại)"
        );

        Map<String, Integer> categoryCounts = new LinkedHashMap<>();
        for (String cat : damageCats) {
            categoryCounts.put(cat, 0);
        }

        List<HistoricalPostRow> tableRows = new ArrayList<>();
        DamageCategorizer categorizer = new DamageCategorizer();

        for (SocialMediaPost post : collectedPosts) {
            DamageCategorizer.ClassificationResult classRes = categorizer.classifyPost(post.getContent());
            String cat = classRes.category;
            categoryCounts.put(cat, categoryCounts.getOrDefault(cat, 0) + 1);

            tableRows.add(new HistoricalPostRow(
                post.getId(),
                post.getPlatform() != null ? post.getPlatform().toUpperCase() : "FB",
                post.getContent(),
                cat,
                classRes.evidence
            ));
        }

        if (severityPieChart != null) {
            ObservableList<PieChart.Data> pieData = FXCollections.observableArrayList();
            for (Map.Entry<String, Integer> entry : categoryCounts.entrySet()) {
                int count = entry.getValue();
                if (count > 0) pieData.add(new PieChart.Data(entry.getKey() + " (" + count + ")", count));
            }
            severityPieChart.setData(pieData);
            severityPieChart.setLegendVisible(true);
            severityPieChart.setLegendSide(javafx.geometry.Side.BOTTOM);
        }

        if (damageTypeBarChart != null) {
            XYChart.Series<String, Number> series = new XYChart.Series<>();
            series.setName("Số lượng báo cáo");
            for (Map.Entry<String, Integer> entry : categoryCounts.entrySet()) {
                int count = entry.getValue();
                String label = entry.getKey();
                if (label.length() > 20) label = label.substring(0, 18) + "...";
                series.getData().add(new XYChart.Data<>(label, count));
            }
            damageTypeBarChart.getData().add(series);
        }
        
        if (historicalPostsTable != null) {
            historicalPostsTable.setItems(FXCollections.observableArrayList(tableRows));
        }
    }

    private void showAlert(String title, String msg) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION, msg, ButtonType.OK);
        alert.setTitle(title);
        alert.showAndWait();
    }

    public static class HistoricalPostRow {
        private final String id;
        private final String platform;
        private final String content;
        private final String severity; 
        private final String damageType; 

        public HistoricalPostRow(String id, String platform, String content, String severity, String damageType) {
            this.id = id;
            this.platform = platform;
            this.content = content;
            this.severity = severity;
            this.damageType = damageType;
        }

        public String getId() { return id; }
        public String getPlatform() { return platform; }
        public String getContent() { return content; }
        public String getSeverity() { return severity; }
        public String getDamageType() { return damageType; }
    }
}
