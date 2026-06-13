package com.project.gui.controller;

import com.project.ai_client.*;
import com.project.ai_client.dto.*;
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
import javafx.scene.chart.*;
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
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.io.Reader;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

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
    @FXML private TableColumn<SocialMediaPost, String> colReactions;
    @FXML private TableColumn<SocialMediaPost, String> colComments;
    @FXML private TableColumn<SocialMediaPost, String> colTimestamp;
    @FXML private Label statusLabel;
    @FXML private ComboBox<String> campaignComboBox;

    // ── Analysis Tab ──
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

    // ── Logistics Tab ──
    @FXML private TableView<DistressPoint> distressTable;
    @FXML private TableColumn<DistressPoint, String> colDistressLocation;
    @FXML private TableColumn<DistressPoint, String> colDistressUrgency;
    @FXML private TableColumn<DistressPoint, String> colDistressSupplies;
    @FXML private TableColumn<DistressPoint, String> colDistressStatus;

    @FXML private TableView<SupportCenter> supportCenterTable;
    @FXML private TableColumn<SupportCenter, String> colCenterName;
    @FXML private TableColumn<SupportCenter, String> colCenterResources;
    @FXML private TableColumn<SupportCenter, Integer> colCenterVehicles;

    @FXML private Label matchedCenterLabel;
    @FXML private Label matchedVehicleLabel;
    @FXML private TextField startLocField;
    @FXML private TextField destLocField;
    @FXML private Button dispatchButton;
    @FXML private TextArea routeOutput;
    @FXML private WebView mapWebView;

    private List<SocialMediaPost> collectedPosts = new ArrayList<>();
    private ObservableList<SupportCenter> supportCenterList = FXCollections.observableArrayList();

    public static class HistoricalPostRow {
        private final String id;
        private final String platform;
        private final String content;
        private final String severity; // Damage Category
        private final String damageType; // Evidence Keywords

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
        setupLogisticsTables();
        loadMockSupportCenters();
        if (pagesListView != null) {
            pagesListView.setItems(FXCollections.observableArrayList(
                "https://www.facebook.com/phongchongthientaivn"
            ));
        }
        if (campaignComboBox != null) {
            campaignComboBox.setItems(FXCollections.observableArrayList(
                "Bão Yagi (Q3/2024)",
                "Lũ lụt Miền Trung (2025)"
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
        colReactions.setCellValueFactory(new PropertyValueFactory<>("reactionsString"));
        colComments.setCellValueFactory(new PropertyValueFactory<>("commentsString"));
        colTimestamp.setCellValueFactory(new PropertyValueFactory<>("timestamp"));

        // Enable text wrapping and dynamic cell height resizing
        colContent.setCellFactory(tc -> {
            TableCell<SocialMediaPost, String> cell = new TableCell<>() {
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

        // Enable text wrapping for comments
        colComments.setCellFactory(tc -> {
            TableCell<SocialMediaPost, String> cell = new TableCell<>() {
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

        // Configure historicalPostsTable
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

    private void setupLogisticsTables() {
        // Distress table
        colDistressLocation.setCellValueFactory(new PropertyValueFactory<>("address"));
        colDistressUrgency.setCellValueFactory(new PropertyValueFactory<>("urgency"));
        colDistressSupplies.setCellValueFactory(new PropertyValueFactory<>("suppliesString"));
        colDistressStatus.setCellValueFactory(new PropertyValueFactory<>("status"));

        colDistressUrgency.setCellFactory(tc -> {
            TableCell<DistressPoint, String> cell = new TableCell<>() {
                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setText(null);
                        setStyle("");
                    } else {
                        String lower = item.toLowerCase();
                        if (lower.contains("critical")) {
                            setText("🔴 Nguy kịch");
                            setStyle("-fx-text-fill: #c0392b; -fx-font-weight: bold;");
                        } else if (lower.contains("high")) {
                            setText("🟠 Cao");
                            setStyle("-fx-text-fill: #d35400; -fx-font-weight: bold;");
                        } else if (lower.contains("medium")) {
                            setText("🟡 Trung bình");
                            setStyle("-fx-text-fill: #f39c12; -fx-font-weight: bold;");
                        } else {
                            setText("🔵 Thấp");
                            setStyle("-fx-text-fill: #27ae60;");
                        }
                    }
                }
            };
            return cell;
        });

        colDistressStatus.setCellFactory(tc -> {
            TableCell<DistressPoint, String> cell = new TableCell<>() {
                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setText(null);
                        setStyle("");
                    } else {
                        if ("dispatched".equalsIgnoreCase(item)) {
                            setText("🟢 Đã điều phối");
                            setStyle("-fx-text-fill: #27ae60; -fx-font-weight: bold;");
                        } else {
                            setText("🔴 Đang chờ");
                            setStyle("-fx-text-fill: #c0392b; -fx-font-weight: bold;");
                        }
                    }
                }
            };
            return cell;
        });

        // Support Center table
        colCenterName.setCellValueFactory(new PropertyValueFactory<>("address"));
        colCenterResources.setCellValueFactory(new PropertyValueFactory<>("resourcesString"));
        colCenterVehicles.setCellValueFactory(new PropertyValueFactory<>("vehicleCount"));

        // Add selection listener to distress table
        distressTable.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                matchDistressLocation(newVal);
            }
        });
    }

    private void loadMockSupportCenters() {
        supportCenterList.clear();

        SupportCenter sc1 = new SupportCenter(21.028511, 105.804817, "Hà Nội (Trung tâm Logistics Hà Nội)", 5);
        sc1.setCurrentSupplies(Arrays.asList("food", "water", "medical", "rescue"));

        SupportCenter sc2 = new SupportCenter(16.047079, 108.206230, "Đà Nẵng (Kho dự trữ Đà Nẵng)", 8);
        sc2.setCurrentSupplies(Arrays.asList("food", "water", "medical", "shelter", "rescue"));

        SupportCenter sc3 = new SupportCenter(18.673244, 105.692440, "Vinh (Kho Vinh - Nghệ An)", 3);
        sc3.setCurrentSupplies(Arrays.asList("food", "water", "medical"));

        supportCenterList.addAll(sc1, sc2, sc3);
        supportCenterTable.setItems(supportCenterList);
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
            statusLabel.setText("Đang hủy thao tác cào dữ liệu... Vui lòng đợi.");
            FacebookScraper.quitActiveDriver();
            scrapingThread.interrupt();
            
            fetchButton.setDisable(false);
            loadingBox.setVisible(false);
            loadingBox.setManaged(false);
            cancelButton.setVisible(false);
            cancelButton.setManaged(false);
            statusLabel.setText("Đã hủy cào dữ liệu bởi người dùng.");
        }
    }

    @FXML
    private void onFetchPosts() {
        String keyword = keywordField.getText().trim();
        List<String> pagesList = (pagesListView != null) ? pagesListView.getItems() : new ArrayList<>();

        if (pagesList.isEmpty()) {
            showAlert("Xác thực dữ liệu", "Vui lòng thêm ít nhất một trang/liên kết mục tiêu.");
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
        statusLabel.setText("Đang thu thập các bài viết trực tiếp... Vui lòng đợi.");

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
                System.err.println("[!] Live scraping failed, loading high-quality mock relief posts for testing: " + e.getMessage());
                errorMsg = e.getMessage();
            }

            if (tempPosts.isEmpty()) {
                SocialMediaPost p1 = new SocialMediaPost(
                    "live-fb-1-yagi",
                    "Mưa lớn kéo dài gây ngập lụt nghiêm trọng tại huyện Hải Lăng, Quảng Trị. Hơn 50 hộ dân bị cô lập hoàn toàn, nước dâng cao đến mái nhà. Hiện tại bà con đang thiếu lương thực, nước uống sạch trầm trọng. Cần hỗ trợ khẩn cấp xuồng cứu hộ và mì tôm, nước suối đóng chai.",
                    "Thông tin Phòng chống thiên tai",
                    java.time.LocalDateTime.now(),
                    "Facebook"
                );
                p1.getReactions().put("sad", 150);
                p1.getReactions().put("care", 80);
                p1.getReactions().put("like", 10);
                p1.getReactions().put("angry", 5);
                p1.getComments().add("Nước dâng nhanh quá, mong đoàn cứu trợ đến sớm!");
                p1.getComments().add("Hải Lăng đang ngập sâu lắm, nhà em ngập nửa người rồi.");
                p1.getComments().add("Cần nước ngọt gấp ạ!");
                tempPosts.add(p1);

                SocialMediaPost p2 = new SocialMediaPost(
                    "live-fb-2-catba",
                    "Tình hình tại đảo Cát Bà, Hải Phòng đang rất nguy cấp sau khi bão đổ bộ. Nhiều ngôi nhà bị tốc mái hoàn toàn, hệ thống điện nước bị cắt. Trạm y tế địa phương đang quá tải và thiếu hụt bông băng, thuốc sát trùng, thuốc kháng sinh cơ bản. Mong các đoàn cứu trợ tiếp tế y tế gấp!",
                    "Hội Chữ thập đỏ Cát Hải",
                    java.time.LocalDateTime.now(),
                    "Facebook"
                );
                p2.getReactions().put("sad", 210);
                p2.getReactions().put("care", 95);
                p2.getReactions().put("like", 15);
                p2.getReactions().put("angry", 12);
                p2.getComments().add("Mất điện từ hôm qua tới giờ chưa có lại.");
                p2.getComments().add("Trạm y tế Cát Hải đang quá tải trầm trọng.");
                p2.getComments().add("Mong mọi người bình an.");
                tempPosts.add(p2);

                SocialMediaPost p3 = new SocialMediaPost(
                    "live-fb-3-lucyen",
                    "Sạt lở đất nghiêm trọng tại Lục Yên, Yên Bái làm sập 3 ngôi nhà, giao thông hoàn toàn bị chia cắt. Có người bị thương đang chờ được đưa đi cấp cứu nhưng xe cứu thương không vào được. Cần lực lượng cứu nạn cứu hộ và rào chắn giao thông tiếp cận khẩn cấp.",
                    "Yên Bái 24h",
                    java.time.LocalDateTime.now(),
                    "Facebook"
                );
                p3.getReactions().put("sad", 340);
                p3.getReactions().put("care", 120);
                p3.getReactions().put("like", 8);
                p3.getReactions().put("angry", 45);
                p3.getComments().add("Thương quá, sạt lở đất đá đè sập cả nhà rồi.");
                p3.getComments().add("Đường Lục Yên sạt nặng, xe cứu trợ chưa vào được đâu.");
                p3.getComments().add("Cầu mong không có thêm thiệt hại về người.");
                tempPosts.add(p3);

                SocialMediaPost p4 = new SocialMediaPost(
                    "live-fb-4-bacha",
                    "Bản Phố, Bắc Hà, Lào Cai bị cô lập do lũ quét sạch cầu tràn. Bà con ở đây tạm thời an toàn nhưng lương thực dự trữ chỉ còn dùng được hết ngày mai. Cần tiếp tế gạo, muối ăn và bạt dựng lều tạm vì nhiều nhà bị hư hại nặng.",
                    "Bắc Hà News",
                    java.time.LocalDateTime.now(),
                    "Facebook"
                );
                p4.getReactions().put("sad", 180);
                p4.getReactions().put("care", 70);
                p4.getReactions().put("like", 12);
                p4.getReactions().put("angry", 2);
                p4.getComments().add("Bản Phố cầu trôi rồi cô lập hoàn toàn.");
                p4.getComments().add("Bà con Bắc Hà rất cần bạt và mì tôm gạo ăn tạm.");
                tempPosts.add(p4);

                SocialMediaPost p5 = new SocialMediaPost(
                    "live-fb-5-thank",
                    "Cảm ơn các nhà hảo tâm và chính quyền địa phương đã kịp thời vận chuyển 200 thùng mì tôm và nước sạch đến cho bà con vùng lũ lụt Thường Xuân, Thanh Hóa hôm nay. Tình hình đang dần ổn định trở lại.",
                    "Người Dân Xứ Thanh",
                    java.time.LocalDateTime.now(),
                    "Facebook"
                );
                p5.getReactions().put("like", 450);
                p5.getReactions().put("care", 280);
                p5.getReactions().put("sad", 5);
                p5.getComments().add("Tuyệt vời quá, cảm ơn đoàn cứu trợ.");
                p5.getComments().add("Ấm lòng tình đồng bào miền Trung lúc hoạn nạn.");
                tempPosts.add(p5);
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
                        statusLabel.setText("Đã hủy cào dữ liệu.");
                    } else {
                        statusLabel.setText("Lỗi: " + finalError);
                        showAlert("Lỗi cào dữ liệu", "Cào dữ liệu trực tuyến thất bại: " + finalError);
                    }
                } else {
                    statusLabel.setText("Đã thu thập " + collectedPosts.size() + " bài viết.");
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
            showAlert("Không có dữ liệu", "Vui lòng thu thập bài viết trước.");
            return;
        }

        AnalysisManager manager = new AnalysisManager();
        if (sentimentCheck.isSelected()) manager.addAnalyzer(new SentimentAnalyzeOverTime());
        if (damageCheck.isSelected())   manager.addAnalyzer(new DamageCategorizer());
        if (reliefCheck.isSelected())   manager.addAnalyzer(new ReliefSentimentAnalyzer());

        if (manager.getAnalyzers().isEmpty()) {
            showAlert("Chưa chọn bộ phân tích", "Vui lòng chọn ít nhất một bộ phân tích.");
            return;
        }

        // Disable run button and show dynamic progress indicator
        runAnalysisButton.setDisable(true);
        resultsBox.getChildren().clear();
        
        VBox loadingContainer = new VBox(12);
        loadingContainer.setAlignment(Pos.CENTER);
        loadingContainer.setPadding(new Insets(40));
        ProgressIndicator progressIndicator = new ProgressIndicator();
        Label progressLabel = new Label("Đang phân tích bài viết bằng công cụ AI... Vui lòng đợi.");
        progressLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: -color-secondary; -fx-font-weight: bold;");
        loadingContainer.getChildren().addAll(progressIndicator, progressLabel);
        resultsBox.getChildren().add(loadingContainer);

        Thread t = new Thread(() -> {
            try {
                final List<AnalysisResult> results = manager.runAll(collectedPosts, aiClient);
                
                // Extract distress points from AI analysis responses
                final List<DistressPoint> distressList = new ArrayList<>();
                for (SocialMediaPost post : collectedPosts) {
                    try {
                        String preprocessedText = WordPreprocessor.preprocess(post.getContent());
                        AnalyzeReq.PostData postData = new AnalyzeReq.PostData(
                            post.getId() != null ? post.getId() : java.util.UUID.randomUUID().toString(),
                            post.getPlatform() != null ? post.getPlatform().toLowerCase() : "facebook",
                            post.getAuthor() != null ? post.getAuthor() : "unknown",
                            preprocessedText,
                            "",
                            "",
                            post.getReactions(),
                            post.getComments(),
                            post.getShareCount()
                        );
                        AnalyzeRes res = aiClient.executeTask("/analyze", new AnalyzeReq(postData), AnalyzeRes.class);
                        if (res != null && res.getHumanitarianSignal() != null) {
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

                                Location resolved = geocodeAddressWithFallback(locName);
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
                    } catch (Exception ex) {
                        System.err.println("[!] Error extracting distress point: " + ex.getMessage());
                    }
                }

                javafx.application.Platform.runLater(() -> {
                    runAnalysisButton.setDisable(false);
                    renderDetailedReport(results);
                    renderHistoricalCharts(results);
                    distressTable.setItems(FXCollections.observableArrayList(distressList));
                });
            } catch (Exception e) {
                e.printStackTrace();
                javafx.application.Platform.runLater(() -> {
                    runAnalysisButton.setDisable(false);
                    resultsBox.getChildren().clear();
                    Label errLabel = new Label("Lỗi phân tích: " + e.getMessage());
                    errLabel.setStyle("-fx-text-fill: #c0392b; -fx-font-weight: bold;");
                    resultsBox.getChildren().add(errLabel);
                    showAlert("Lỗi phân tích", "Chạy phân tích thất bại: " + e.getMessage());
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
        Label title = new Label("📋 Báo cáo phân tích mạng xã hội bằng AI");
        title.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: -color-text;");
        Label subtitle = new Label("Tạo lúc: " + new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()) 
            + "  •  Đã phân tích " + collectedPosts.size() + " bài viết");
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

                    // Add spacer
                    Separator sep = new Separator();
                    sep.setPadding(new Insets(8, 0, 4, 0));
                    card.getChildren().add(sep);

                    // Evidence Panel
                    VBox evidenceHeader = new VBox(4);
                    Label evidenceTitle = new Label("🚨 Bằng chứng xác thực: Các bài đăng khẩn cấp & có độ ưu tiên cao");
                    evidenceTitle.setStyle("-fx-font-weight: bold; -fx-text-fill: -color-text; -fx-font-size: 13px;");
                    evidenceHeader.getChildren().add(evidenceTitle);
                    card.getChildren().add(evidenceHeader);

                    VBox evidenceContainer = new VBox(8);
                    int evidenceCount = 0;
                    for (SocialMediaPost post : collectedPosts) {
                        try {
                            String preprocessed = WordPreprocessor.preprocess(post.getContent());
                            AnalyzeReq.PostData postData = new AnalyzeReq.PostData(
                                post.getId() != null ? post.getId() : java.util.UUID.randomUUID().toString(),
                                post.getPlatform() != null ? post.getPlatform().toLowerCase() : "facebook",
                                post.getAuthor() != null ? post.getAuthor() : "unknown",
                                preprocessed,
                                "",
                                "",
                                post.getReactions(),
                                post.getComments(),
                                post.getShareCount()
                            );
                            AnalyzeRes aiRes = aiClient.executeTask("/analyze", new AnalyzeReq(postData), AnalyzeRes.class);
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
                        } catch (Exception e) {
                            // Ignore
                        }
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
                        if ("food".equalsIgnoreCase(item)) {
                            emoji = " 🍞";
                            itemTranslated = "Lương thực";
                        } else if ("water".equalsIgnoreCase(item)) {
                            emoji = " 💧";
                            itemTranslated = "Nước sạch";
                        } else if ("medical".equalsIgnoreCase(item)) {
                            emoji = " 💊";
                            itemTranslated = "Thuốc men/Y tế";
                        } else if ("shelter".equalsIgnoreCase(item)) {
                            emoji = " ⛺";
                            itemTranslated = "Nhà ở/Bạt che";
                        } else if ("rescue".equalsIgnoreCase(item)) {
                            emoji = " 🛟";
                            itemTranslated = "Cứu hộ/Áo phao";
                        }
                        
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

                    // Add spacer
                    Separator sep = new Separator();
                    sep.setPadding(new Insets(8, 0, 4, 0));
                    card.getChildren().add(sep);

                    // Evidence Panel
                    VBox evidenceHeader = new VBox(4);
                    Label evidenceTitle = new Label("📦 Bằng chứng xác thực: Yêu cầu cung cấp nhu yếu phẩm cụ thể");
                    evidenceTitle.setStyle("-fx-font-weight: bold; -fx-text-fill: -color-text; -fx-font-size: 13px;");
                    evidenceHeader.getChildren().add(evidenceTitle);
                    card.getChildren().add(evidenceHeader);

                    VBox evidenceContainer = new VBox(8);
                    int demandCount = 0;
                    for (SocialMediaPost post : collectedPosts) {
                        try {
                            String preprocessed = WordPreprocessor.preprocess(post.getContent());
                            AnalyzeReq.PostData postData = new AnalyzeReq.PostData(
                                post.getId() != null ? post.getId() : java.util.UUID.randomUUID().toString(),
                                post.getPlatform() != null ? post.getPlatform().toLowerCase() : "facebook",
                                post.getAuthor() != null ? post.getAuthor() : "unknown",
                                preprocessed,
                                "",
                                "",
                                post.getReactions(),
                                post.getComments(),
                                post.getShareCount()
                            );
                            AnalyzeRes aiRes = aiClient.executeTask("/analyze", new AnalyzeReq(postData), AnalyzeRes.class);
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
                                    
                                    Label evAction = new Label(String.format("Đề xuất: %s", 
                                        aiRes.getHumanitarianSignal().getRecommendedAction()));
                                    evAction.setStyle("-fx-font-weight: bold; -fx-text-fill: #67c23a;");
                                    
                                    evCard.getChildren().addAll(evTitle, evText, evAction);
                                    evidenceContainer.getChildren().add(evCard);
                                    demandCount++;
                                }
                            }
                        } catch (Exception e) {
                            // Ignore
                        }
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
                    // Calculate overall stats
                    double sum = 0.0;
                    for (double score : timeline.values()) {
                        sum += score;
                    }
                    double avgNegativity = sum / timeline.size();
                    
                    VBox stats = new VBox(6);
                    HBox avgRow = new HBox(10);
                    avgRow.setAlignment(Pos.CENTER_LEFT);
                    Label avgLabel = new Label("Chỉ số tiêu cực trung bình:");
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
                    
                    Label detailsTitle = new Label("Chi tiết bài đăng:");
                    detailsTitle.setStyle("-fx-font-weight: bold; -fx-padding: 8 0 2 0; -fx-text-fill: -color-text;");
                    
                    VBox list = new VBox(8);
                    for (String postId : timeline.keySet()) {
                        double neg = timeline.get(postId);
                        String emo = emotions.get(postId);
                        if (emo == null) emo = "unknown";
                        
                        String emoji = "😐";
                        String emoTranslated = "Bình thường";
                        if ("anger".equalsIgnoreCase(emo)) {
                            emoji = "😡";
                            emoTranslated = "Giận dữ";
                        } else if ("sadness".equalsIgnoreCase(emo)) {
                            emoji = "😢";
                            emoTranslated = "Buồn bã";
                        } else if ("fear".equalsIgnoreCase(emo)) {
                            emoji = "😨";
                            emoTranslated = "Lo sợ";
                        } else if ("joy".equalsIgnoreCase(emo)) {
                            emoji = "😊";
                            emoTranslated = "Vui vẻ";
                        } else if ("disgust".equalsIgnoreCase(emo)) {
                            emoji = "🤢";
                            emoTranslated = "Ghê tởm";
                        } else if ("surprise".equalsIgnoreCase(emo)) {
                            emoji = "😮";
                            emoTranslated = "Ngạc nhiên";
                        } else if ("anxiety".equalsIgnoreCase(emo)) {
                            emoji = "😰";
                            emoTranslated = "Lo âu";
                        }
                        
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
                        
                        SocialMediaPost actualPost = findPostById(postId);
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

    @FXML
    private void onFindRoute() {
        String startStr = startLocField.getText().trim();
        String destStr = destLocField.getText().trim();

        if (startStr.isEmpty() || destStr.isEmpty()) {
            showAlert("Xác thực dữ liệu", "Vui lòng nhập điểm xuất phát và điểm đến.");
            return;
        }

        routeOutput.setText("Đang tìm vị trí và vẽ lộ trình...");

        // Perform geocoding (Nominatim API query is a network call, we run it on background thread to prevent GUI lockup)
        Thread routeThread = new Thread(() -> {
            Location start = geocodeAddressWithFallback(startStr);
            Location dest = geocodeAddressWithFallback(destStr);

            javafx.application.Platform.runLater(() -> {
                RouteFinder finder = new RouteFinder();
                List<Location> route = finder.AStarRouteFinder(start, dest);

                StringBuilder sb = new StringBuilder("Thông tin Lộ trình Cứu trợ:\n");
                sb.append(String.format("  Xuất phát: %s (%.6f, %.6f)%n", start.getAddress(), start.getLatitude(), start.getLongitude()));
                sb.append(String.format("  Điểm đến:  %s (%.6f, %.6f)%n%n", dest.getAddress(), dest.getLatitude(), dest.getLongitude()));
                sb.append("Đường vẽ lộ trình trực quan đã được hiển thị trên bản đồ OpenStreetMap.");
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
        } else if (lower.contains("yên bái") || lower.contains("yen bai") || lower.contains("lục yên")) {
            return new Location(21.722926, 104.911306, "Huyện Lục Yên, Yên Bái, Việt Nam (Local Fallback)");
        } else if (lower.contains("lào cai") || lower.contains("lao cai") || lower.contains("bắc hà")) {
            return new Location(22.485639, 103.970669, "Huyện Bắc Hà, Lào Cai, Việt Nam (Local Fallback)");
        } else if (lower.contains("hải phòng") || lower.contains("hai phong") || lower.contains("cát bà")) {
            return new Location(20.865139, 106.683830, "Đảo Cát Bà, Hải Phòng, Việt Nam (Local Fallback)");
        } else if (lower.contains("thanh hóa") || lower.contains("thanh hoa") || lower.contains("thường xuân")) {
            return new Location(19.806678, 105.785084, "Thường Xuân, Thanh Hóa, Việt Nam (Local Fallback)");
        }
        
        // Final central fallback
        return new Location(14.0583, 108.2772, address + " (Fallback Central Vietnam)");
    }

    private void matchDistressLocation(DistressPoint dp) {
        if (dp == null) return;
        
        SupportCenter bestCenter = null;
        double minDistance = Double.MAX_VALUE;
        
        for (SupportCenter center : supportCenterList) {
            boolean hasMatchingSupply = false;
            if (dp.getRequiredSupplies() != null && !dp.getRequiredSupplies().isEmpty()) {
                for (String supply : dp.getRequiredSupplies()) {
                    if (center.getCurrentSupplies().contains(supply.toLowerCase())) {
                        hasMatchingSupply = true;
                        break;
                    }
                }
            } else {
                hasMatchingSupply = true;
            }
            
            if (hasMatchingSupply) {
                double dx = center.getLatitude() - dp.getLatitude();
                double dy = center.getLongitude() - dp.getLongitude();
                double dist = dx*dx + dy*dy;
                if (dist < minDistance) {
                    minDistance = dist;
                    bestCenter = center;
                }
            }
        }
        
        if (bestCenter == null) {
            for (SupportCenter center : supportCenterList) {
                double dx = center.getLatitude() - dp.getLatitude();
                double dy = center.getLongitude() - dp.getLongitude();
                double dist = dx*dx + dy*dy;
                if (dist < minDistance) {
                    minDistance = dist;
                    bestCenter = center;
                }
            }
        }
        
        if (bestCenter != null) {
            matchedCenterLabel.setText(bestCenter.getAddress().split(" \\(")[0]);
            matchedVehicleLabel.setText("VH-00" + (supportCenterList.indexOf(bestCenter) + 1) + " (Sẵn sàng)");
            startLocField.setText(bestCenter.getAddress());
            destLocField.setText(dp.getAddress());
            
            if (mapWebView != null) {
                try {
                    String script = String.format(Locale.US, "setRoute(%f, %f, %f, %f, '%s', '%s');",
                        bestCenter.getLatitude(), bestCenter.getLongitude(),
                        dp.getLatitude(), dp.getLongitude(),
                        bestCenter.getAddress().replace("'", "\\'"),
                        dp.getAddress().replace("'", "\\'")
                    );
                    mapWebView.getEngine().executeScript(script);
                } catch (Exception e) {
                    System.err.println("[!] Failed to call Leaflet JS: " + e.getMessage());
                }
            }
            
            StringBuilder sb = new StringBuilder();
            sb.append("📋 KẾ HOẠCH ĐIỀU PHỐI ĐÃ TẠO\n");
            sb.append("----------------------------\n");
            sb.append(String.format("📍 Lộ trình: %s ➔ %s%n", bestCenter.getAddress().split(" \\(")[0], dp.getAddress()));
            sb.append(String.format("🚛 Phương tiện được gán: VH-00%d%n", supportCenterList.indexOf(bestCenter) + 1));
            sb.append(String.format("📦 Hàng cứu trợ: %s%n", dp.getSuppliesString()));
            sb.append("🚦 Trạng thái: Sẵn sàng điều phối. Nhấp 'Bắt đầu vận chuyển' để thực hiện.");
            routeOutput.setText(sb.toString());
        }
    }

    @FXML
    private void onDispatchResources() {
        DistressPoint dp = distressTable.getSelectionModel().getSelectedItem();
        if (dp == null) {
            showAlert("Chưa chọn", "Vui lòng chọn một điểm khẩn cấp từ bảng trước.");
            return;
        }
        
        if ("DISPATCHED".equalsIgnoreCase(dp.getStatus())) {
            showAlert("Đã điều phối", "Nguồn lực cứu trợ đã được điều phối đến vị trí này rồi.");
            return;
        }
        
        String startVal = startLocField.getText().trim();
        SupportCenter matchedCenter = null;
        for (SupportCenter center : supportCenterList) {
            if (center.getAddress().equalsIgnoreCase(startVal)) {
                matchedCenter = center;
                break;
            }
        }
        
        if (matchedCenter == null && !supportCenterList.isEmpty()) {
            matchedCenter = supportCenterList.get(0);
        }
        
        if (matchedCenter != null) {
            if (matchedCenter.getVehicleCount() > 0) {
                matchedCenter.setVehicleCount(matchedCenter.getVehicleCount() - 1);
            } else {
                showAlert("Không có phương tiện", "Trung tâm được chọn hiện không có sẵn phương tiện.");
                return;
            }
        }
        
        dp.setStatus("DISPATCHED");
        distressTable.refresh();
        supportCenterTable.refresh();
        
        String vId = matchedVehicleLabel.getText().split(" ")[0];
        matchedVehicleLabel.setText(vId + " (Đã xuất phát)");
        
        StringBuilder sb = new StringBuilder();
        sb.append("🚀 ĐIỀU PHỐI THÀNH CÔNG\n");
        sb.append("----------------------------\n");
        sb.append(String.format("✔ Phương tiện %s đã xuất phát từ %s.%n", vId, startVal.split(" \\(")[0]));
        sb.append(String.format("✔ Điểm đến: %s%n", dp.getAddress()));
        sb.append(String.format("✔ Hàng cứu trợ: %s%n", dp.getSuppliesString()));
        sb.append("✔ Đang theo dõi lộ trình cứu trợ.");
        routeOutput.setText(sb.toString());
        
        showAlert("Thành công", "Đã bắt đầu điều phối cứu trợ cho " + dp.getAddress());
    }

    @FXML
    private void onResetMap() {
        distressTable.getSelectionModel().clearSelection();
        supportCenterTable.getSelectionModel().clearSelection();
        startLocField.clear();
        destLocField.clear();
        matchedCenterLabel.setText("Chưa chọn");
        matchedVehicleLabel.setText("Chưa chọn");
        routeOutput.setText("Đã xóa bản đồ và các thiết lập lựa chọn.");
        
        if (mapWebView != null) {
            try {
                mapWebView.getEngine().executeScript("clearMap(); map.setView([14.0583, 108.2772], 6);");
            } catch (Exception e) {
                System.err.println("[!] Failed to reset map via JS: " + e.getMessage());
            }
        }
    }

    private SocialMediaPost findPostById(String id) {
        for (SocialMediaPost p : collectedPosts) {
            if (p.getId() != null && p.getId().equals(id)) {
                return p;
            }
        }
        return null;
    }

    private void showAlert(String title, String msg) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION, msg, ButtonType.OK);
        alert.setTitle(title);
        alert.showAndWait();
    }

    @FXML
    private void onLoadHistoricalCampaign() {
        String selected = campaignComboBox.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Chưa chọn chiến dịch", "Vui lòng chọn một chiến dịch từ danh sách thả xuống trước.");
            return;
        }

        String filePath = "";
        if ("Bão Yagi (Q3/2024)".equalsIgnoreCase(selected)) {
            filePath = "src/main/resources/data/yagi_dataset.json"; 
        } else if ("Lũ lụt Miền Trung (2025)".equalsIgnoreCase(selected)) {
            filePath = "src/main/resources/data/midvietnam_dataset.json";
        } else {
            System.out.println("Chưa có dataset cho lựa chọn này.");
            return;
        }

        // Tạo Gson với bộ chuyển đổi LocalDateTime
        Gson gson = new GsonBuilder()
            .registerTypeAdapter(LocalDateTime.class, (JsonDeserializer<LocalDateTime>) (json, type, jsonDeserializationContext) -> {
                try {
                    // Cắt bỏ chữ 'Z' ở cuối (nếu có) để parse chuẩn ISO Local Date Time
                    String datetime = json.getAsString().replace("Z", "");
                    return LocalDateTime.parse(datetime, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                } catch (Exception e) {
                    System.err.println("Lỗi parse thời gian: " + json.getAsString());
                    return LocalDateTime.now(); // Giá trị mặc định nếu lỗi
                }
            })
            .create();

        try (Reader reader = Files.newBufferedReader(Paths.get(filePath))) {
            // Định nghĩa kiểu dữ liệu danh sách để Gson ép kiểu
            Type listType = new TypeToken<List<SocialMediaPost>>(){}.getType();
            
            // Thực hiện đọc file và chuyển đổi
            List<SocialMediaPost> importedPosts = gson.fromJson(reader, listType);
            
            if (importedPosts != null && !importedPosts.isEmpty()) {
                // Xóa data cũ và cập nhật data mới
                collectedPosts.clear();
                collectedPosts.addAll(importedPosts);
                
                // Cập nhật lên TableView
                postsTable.setItems(FXCollections.observableArrayList(collectedPosts));
                
                // Cập nhật nhãn trạng thái
                statusLabel.setText("Đã nạp " + collectedPosts.size() + " bài viết lịch sử từ " + selected);
                System.out.println("[+] Đã nạp thành công dữ liệu lịch sử.");
                
                // Tự động chạy vẽ các biểu đồ lịch sử và bảng phân loại chi tiết (dùng fallback keyword scanner)
                renderHistoricalCharts(new ArrayList<>());
            } else {
                showAlert("Dữ liệu trống", "Tệp dữ liệu không chứa bài viết hoặc sai định dạng.");
            }

        } catch (Exception e) {
            System.err.println("[!] Lỗi khi đọc file JSON: " + e.getMessage());
            e.printStackTrace();
            showAlert("Lỗi đọc tệp", "Không thể nạp dữ liệu: " + e.getMessage());
        }
    }
    @FXML
    @SuppressWarnings("unchecked")
    private void renderHistoricalCharts(List<AnalysisResult> results) {
        if (negativityLineChart == null || emotionStackedChart == null || timelineMilestonesBox == null) return;

        negativityLineChart.getData().clear();
        emotionStackedChart.getData().clear();
        timelineMilestonesBox.getChildren().clear();
        if (severityPieChart != null) severityPieChart.getData().clear();
        if (damageTypeBarChart != null) damageTypeBarChart.getData().clear();
        if (damageExplanationsBox != null) damageExplanationsBox.getChildren().clear();
        if (historicalPostsTable != null) historicalPostsTable.getItems().clear();

        // 1. Group posts by date (sorted chronological)
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

        // Extract from results if available
        for (AnalysisResult res : results) {
            if ("SentimentOverTime".equalsIgnoreCase(res.getAnalyzerName())) {
                Map<String, Double> timeline = (Map<String, Double>) res.get("negativeScoreTimeline");
                Map<String, String> emotions = (Map<String, String>) res.get("dominantEmotions");
                if (timeline != null) postNegativity.putAll(timeline);
                if (emotions != null) postEmotion.putAll(emotions);
            }
        }

        // Fallback calculation for posts if they don't have analysis results
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

        // 2. Classify posts into 6 main damage categories
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

        // Populate PieChart: Severity (Proportions of Damage Categories)
        if (severityPieChart != null) {
            ObservableList<PieChart.Data> pieData = FXCollections.observableArrayList();
            for (Map.Entry<String, Integer> entry : categoryCounts.entrySet()) {
                int count = entry.getValue();
                if (count > 0) {
                    pieData.add(new PieChart.Data(entry.getKey() + " (" + count + ")", count));
                }
            }
            severityPieChart.setData(pieData);
        }

        // Populate BarChart: Need Categories (Frequencies of Damage Categories)
        if (damageTypeBarChart != null) {
            XYChart.Series<String, Number> series = new XYChart.Series<>();
            series.setName("Số lượng báo cáo");
            for (Map.Entry<String, Integer> entry : categoryCounts.entrySet()) {
                int count = entry.getValue();
                String label = entry.getKey();
                if (label.length() > 20) {
                    label = label.substring(0, 18) + "...";
                }
                series.getData().add(new XYChart.Data<>(label, count));
            }
            damageTypeBarChart.getData().add(series);
        }

        // Populate detailed explanations reports
        if (damageExplanationsBox != null) {
            int totalPosts = collectedPosts.size();
            
            // Find the category with maximum reports
            String maxCategory = "";
            int maxCount = -1;
            for (Map.Entry<String, Integer> entry : categoryCounts.entrySet()) {
                if (!entry.getKey().contains("Khác") && entry.getValue() > maxCount) {
                    maxCount = entry.getValue();
                    maxCategory = entry.getKey();
                }
            }

            VBox overviewCard = new VBox(8);
            overviewCard.setPadding(new Insets(12));
            overviewCard.setStyle("-fx-background-color: #e8f4fd; -fx-border-color: #b3d8f6; -fx-border-radius: 6; -fx-background-radius: 6;");
            
            Label overviewTitle = new Label("💡 Tổng quan đánh giá thiệt hại phổ biến nhất");
            overviewTitle.setStyle("-fx-font-weight: bold; -fx-text-fill: #1b4f72; -fx-font-size: 13px;");
            
            String overviewText = String.format(
                "Dựa trên phân tích %d bài đăng trên mạng xã hội, loại thiệt hại phổ biến nhất được ghi nhận là: \"%s\" với %d báo cáo.\n" +
                "Điều này phản ánh mức độ nghiêm trọng và sự quan tâm cao nhất của công chúng đối với khía cạnh này trong khu vực bị ảnh hưởng, hỗ trợ đắc lực công tác phục hồi sau thiên tai.",
                totalPosts, maxCategory, maxCount
            );
            Label overviewLbl = new Label(overviewText);
            overviewLbl.setWrapText(true);
            overviewLbl.setStyle("-fx-text-fill: -color-text; -fx-font-size: 12px;");
            
            overviewCard.getChildren().addAll(overviewTitle, overviewLbl);
            damageExplanationsBox.getChildren().add(overviewCard);

            // Detailed breakdowns
            Map<String, String> descMap = new HashMap<>();
            descMap.put("Người bị ảnh hưởng", "Ghi nhận các báo cáo thương vong, người bị thương, tử vong hoặc mất tích do bão lũ. Đây là nhóm chỉ số cực kỳ quan trọng đối với công tác cứu hộ khẩn cấp và y tế.");
            descMap.put("Nhà cửa hoặc tòa nhà bị hư hỏng", "Ghi nhận nhà dân bị sập đổ, tốc mái tôn, ngập đến nóc hoặc đổ tường. Phản ánh trực tiếp nhu cầu hỗ trợ về chỗ ở tạm thời, bạt che và vật liệu phục hồi.");
            descMap.put("Cơ sở hạ tầng bị hư hỏng", "Ghi nhận cầu cống bị sập gãy, quốc lộ bị sạt lở núi gây tắc nghẽn, cột điện gãy đổ gây mất điện diện rộng hoặc vỡ đê. Phản ánh khó khăn về giao thông, viễn thông và tiếp cận cứu trợ.");
            descMap.put("Nông nghiệp & Vật nuôi bị thiệt hại", "Ghi nhận diện tích lớn hoa màu, vườn cây ăn quả và ruộng lúa bị ngập úng hoặc bồi lấp; trang trại chăn nuôi có trâu bò, lợn gà bị cuốn trôi hoặc chết hàng loạt.");
            descMap.put("Gián đoạn các hoạt động kinh tế sản xuất", "Ghi nhận hoạt động buôn bán tại chợ bị ngưng trệ, cửa hàng/công ty đóng cửa nghỉ làm, thiếu nước sạch/sóng viễn thông kéo dài hoặc tình trạng khan hiếm thực phẩm làm tăng giá cả thị trường.");
            descMap.put("Tài sản cá nhân bị mất", "Ghi nhận phương tiện đi lại như xe máy, ô tô bị ngập nước hỏng hóc hoặc bị dòng lũ cuốn trôi; đồ đạc thiết bị gia đình (ti vi, tủ lạnh) bị ngập nước hư hỏng.");
            descMap.put("Khác (Tin tức chung / Chưa phân loại)", "Các bài đăng chia sẻ tin dự báo thời tiết, thông tin cảnh báo chung hoặc nội dung chưa đủ từ khóa để phân loại thiệt hại cụ thể.");

            Map<String, String> kwMap = new HashMap<>();
            kwMap.put("Người bị ảnh hưởng", "chết, thiệt mạng, mất tích, thi thể, chấn thương, bị thương, cấp cứu...");
            kwMap.put("Nhà cửa hoặc tòa nhà bị hư hỏng", "sập nhà, tốc mái, đổ tường, bay mái tôn, sập trần...");
            kwMap.put("Cơ sở hạ tầng bị hư hỏng", "sạt lở, sập cầu, cột điện đổ, ngập quốc lộ, tắc đường, vỡ đê...");
            kwMap.put("Nông nghiệp & Vật nuôi bị thiệt hại", "ngập lúa, mất mùa, chết gà, trôi ao cá, chết gia súc, hoa màu...");
            kwMap.put("Gián đoạn các hoạt động kinh tế sản xuất", "mất điện, ngừng hoạt động, nghỉ làm, tăng giá, chợ đóng cửa...");
            kwMap.put("Tài sản cá nhân bị mất", "hỏng xe, trôi xe, trôi đồ đạc, ngập ti vi, tủ lạnh hỏng...");
            kwMap.put("Khác (Tin tức chung / Chưa phân loại)", "Không phát hiện từ khóa thiệt hại đặc trưng");

            for (String cat : damageCats) {
                int count = categoryCounts.getOrDefault(cat, 0);
                double pct = totalPosts > 0 ? (double) count / totalPosts * 100 : 0.0;
                
                VBox card = new VBox(6);
                card.setPadding(new Insets(10));
                
                String colorStyle;
                String titleColor;
                if (cat.contains("Người")) {
                    colorStyle = "-fx-background-color: #fdf2f2; -fx-border-color: #f8b4b4;";
                    titleColor = "#c0392b";
                } else if (cat.contains("Nhà cửa")) {
                    colorStyle = "-fx-background-color: #fef9f3; -fx-border-color: #fcd9bd;";
                    titleColor = "#d35400";
                } else if (cat.contains("Cơ sở")) {
                    colorStyle = "-fx-background-color: #f0f7fc; -fx-border-color: #c8e1f5;";
                    titleColor = "#2980b9";
                } else if (cat.contains("Nông nghiệp")) {
                    colorStyle = "-fx-background-color: #f4faf6; -fx-border-color: #d1ebd9;";
                    titleColor = "#27ae60";
                } else if (cat.contains("Gián đoạn")) {
                    colorStyle = "-fx-background-color: #fbf3fc; -fx-border-color: #f0cff2;";
                    titleColor = "#8e44ad";
                } else if (cat.contains("Tài sản")) {
                    colorStyle = "-fx-background-color: #fefcf3; -fx-border-color: #fbf5c8;";
                    titleColor = "#a04000";
                } else {
                    colorStyle = "-fx-background-color: #f8f9fa; -fx-border-color: #e2e4e8;";
                    titleColor = "#586069";
                }
                
                card.setStyle(colorStyle + " -fx-border-radius: 4; -fx-background-radius: 4;");
                
                Label titleLbl = new Label(String.format("📌 %s  —  %d báo cáo (%.1f%%)", cat, count, pct));
                titleLbl.setStyle("-fx-font-weight: bold; -fx-text-fill: " + titleColor + "; -fx-font-size: 13px;");
                
                Label descLbl = new Label("• Mô tả thiệt hại: " + descMap.get(cat));
                descLbl.setWrapText(true);
                descLbl.setStyle("-fx-text-fill: -color-text; -fx-font-size: 12px;");
                
                Label kwLbl = new Label("• Bằng chứng từ khóa điển hình: " + kwMap.get(cat));
                kwLbl.setStyle("-fx-text-fill: -color-secondary; -fx-font-weight: bold; -fx-font-size: 11px;");
                kwLbl.setWrapText(true);
                
                card.getChildren().addAll(titleLbl, descLbl, kwLbl);
                damageExplanationsBox.getChildren().add(card);
            }
        }

        // Bind table data
        if (historicalPostsTable != null) {
            historicalPostsTable.setItems(FXCollections.observableArrayList(tableRows));
        }

        // 3. Populate LineChart: Average Negativity Over Time
        XYChart.Series<String, Number> negativitySeries = new XYChart.Series<>();
        negativitySeries.setName("Chỉ số tiêu cực");

        // 4. Populate StackedBarChart: Emotion Breakdown
        XYChart.Series<String, Number> angerSeries = new XYChart.Series<>(); angerSeries.setName("Giận dữ 😡");
        XYChart.Series<String, Number> fearSeries = new XYChart.Series<>(); fearSeries.setName("Lo sợ 😨");
        XYChart.Series<String, Number> sadnessSeries = new XYChart.Series<>(); sadnessSeries.setName("Buồn bã 😢");
        XYChart.Series<String, Number> joySeries = new XYChart.Series<>(); joySeries.setName("Vui vẻ 😊");
        XYChart.Series<String, Number> neutralSeries = new XYChart.Series<>(); neutralSeries.setName("Bình thường 😐");

        for (Map.Entry<String, List<SocialMediaPost>> entry : postsByDate.entrySet()) {
            String dateStr = entry.getKey();
            List<SocialMediaPost> datePosts = entry.getValue();

            double sumNeg = 0.0;
            int countAnger = 0, countFear = 0, countSad = 0, countJoy = 0, countNeutral = 0;

            for (SocialMediaPost p : datePosts) {
                double neg = postNegativity.getOrDefault(p.getId(), 0.5);
                sumNeg += neg;

                String emo = postEmotion.getOrDefault(p.getId(), "neutral").toLowerCase();
                if (emo.contains("anger")) countAnger++;
                else if (emo.contains("fear") || emo.contains("anxiety")) countFear++;
                else if (emo.contains("sad")) countSad++;
                else if (emo.contains("joy")) countJoy++;
                else countNeutral++;
            }

            double avgNeg = sumNeg / datePosts.size();
            negativitySeries.getData().add(new XYChart.Data<>(dateStr, avgNeg));

            angerSeries.getData().add(new XYChart.Data<>(dateStr, countAnger));
            fearSeries.getData().add(new XYChart.Data<>(dateStr, countFear));
            sadnessSeries.getData().add(new XYChart.Data<>(dateStr, countSad));
            joySeries.getData().add(new XYChart.Data<>(dateStr, countJoy));
            neutralSeries.getData().add(new XYChart.Data<>(dateStr, countNeutral));

            VBox milestoneCard = new VBox(6);
            milestoneCard.setPadding(new Insets(10));
            
            String cardStyle = "-fx-background-color: #f8f9fa; -fx-border-color: #d8dce4; -fx-border-radius: 4; -fx-background-radius: 4;";
            String titleColor = "-color-text";
            
            if (avgNeg >= 0.75) {
                cardStyle = "-fx-background-color: #fdf2f2; -fx-border-color: #f8b4b4; -fx-border-radius: 4; -fx-background-radius: 4;";
                titleColor = "#c0392b";
            } else if (avgNeg < 0.25) {
                cardStyle = "-fx-background-color: #f0f9eb; -fx-border-color: #c2e7b0; -fx-border-radius: 4; -fx-background-radius: 4;";
                titleColor = "#67c23a";
            }
            
            milestoneCard.setStyle(cardStyle);

            Label dateLbl = new Label("📅 Ngày " + dateStr);
            dateLbl.setStyle("-fx-font-weight: bold; -fx-text-fill: " + titleColor + "; -fx-font-size: 13px;");
            
            Label summaryLbl = new Label(String.format("• Chỉ số tiêu cực trung bình: %.1f%%\n• Cảm xúc nổi bật nhất: %s\n• Số lượng ghi nhận: %d bài đăng.", 
                avgNeg * 100, 
                getDominantEmotionLabel(countAnger, countFear, countSad, countJoy, countNeutral),
                datePosts.size()
            ));
            summaryLbl.setStyle("-fx-text-fill: -color-text; -fx-font-size: 12px;");
            summaryLbl.setWrapText(true);

            milestoneCard.getChildren().addAll(dateLbl, summaryLbl);
            timelineMilestonesBox.getChildren().add(milestoneCard);
        }

        negativityLineChart.getData().add(negativitySeries);
        emotionStackedChart.getData().addAll(angerSeries, fearSeries, sadnessSeries, joySeries, neutralSeries);
    }

    private String getDominantEmotionLabel(int anger, int fear, int sadness, int joy, int neutral) {
        int max = anger;
        String label = "😡 Giận dữ";
        if (fear > max) { max = fear; label = "😨 Lo sợ"; }
        if (sadness > max) { max = sadness; label = "😢 Buồn bã"; }
        if (joy > max) { max = joy; label = "😊 Vui vẻ"; }
        if (neutral > max) { max = neutral; label = "😐 Bình thường"; }
        if (max == 0) return "😐 Bình thường";
        return label;
    }
}
