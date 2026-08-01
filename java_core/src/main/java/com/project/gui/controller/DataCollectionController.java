package com.project.gui.controller;

import com.project.ai_client.IAiClient;
import com.project.datacollection.model.SocialMediaPost;
import com.project.datacollection.platform.FacebookScraper;
import com.project.datacollection.platform.PlatformSetting;
import com.project.gui.MockDataProvider;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializer;
import com.google.gson.reflect.TypeToken;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;

import org.springframework.stereotype.Component;

import java.io.Reader;
import java.lang.reflect.Type;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.ResourceBundle;

@Component
public class DataCollectionController implements Initializable {

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

    private List<SocialMediaPost> collectedPosts;
    private IAiClient aiClient;
    private Thread scrapingThread;
    private Runnable onHistoricalDataLoadedCallback;

    public void setSharedState(List<SocialMediaPost> collectedPosts, IAiClient aiClient) {
        this.collectedPosts = collectedPosts;
        this.aiClient = aiClient;
        if (postsTable != null) {
            postsTable.setItems(FXCollections.observableArrayList(this.collectedPosts));
        }
    }

    public void setOnHistoricalDataLoadedCallback(Runnable callback) {
        this.onHistoricalDataLoadedCallback = callback;
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupPostsTable();
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
    }

    private void setupPostsTable() {
        colPostId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colContent.setCellValueFactory(new PropertyValueFactory<>("content"));
        colReactions.setCellValueFactory(new PropertyValueFactory<>("reactionsString"));
        colComments.setCellValueFactory(new PropertyValueFactory<>("commentsString"));
        colTimestamp.setCellValueFactory(new PropertyValueFactory<>("timestamp"));

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
                tempPosts.addAll(MockDataProvider.getMockPosts());
            }

            final List<SocialMediaPost> finalPosts = tempPosts;
            final String finalError = errorMsg;

            Platform.runLater(() -> {
                collectedPosts.clear();
                collectedPosts.addAll(finalPosts);
                postsTable.setItems(FXCollections.observableArrayList(collectedPosts));

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
    private void onLoadHistoricalCampaign() {
        String selected = campaignComboBox.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Chưa chọn chiến dịch", "Vui lòng chọn một chiến dịch từ danh sách thả xuống trước.");
            return;
        }

        String resourcePath = "";
        if ("Bão Yagi (Q3/2024)".equalsIgnoreCase(selected)) {
            resourcePath = "/data/yagi_dataset.json"; 
        } else if ("Lũ lụt Miền Trung (2025)".equalsIgnoreCase(selected)) {
            resourcePath = "/data/midvietnam_dataset.json";
        } else {
            System.out.println("Chưa có dataset cho lựa chọn này.");
            return;
        }

        Gson gson = new GsonBuilder()
            .registerTypeAdapter(LocalDateTime.class, (JsonDeserializer<LocalDateTime>) (json, type, jsonDeserializationContext) -> {
                try {
                    String datetime = json.getAsString().replace("Z", "");
                    return LocalDateTime.parse(datetime, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                } catch (Exception e) {
                    System.err.println("Lỗi parse thời gian: " + json.getAsString());
                    return LocalDateTime.now();
                }
            })
            .create();

        java.io.InputStream is = getClass().getResourceAsStream(resourcePath);
        
        // Cố gắng tìm file trực tiếp từ thư mục nếu getResourceAsStream bị lỗi classpath
        if (is == null) {
            try {
                java.io.File f = new java.io.File("java_core/src/main/resources" + resourcePath);
                if (!f.exists()) f = new java.io.File("src/main/resources" + resourcePath);
                if (f.exists()) is = new java.io.FileInputStream(f);
            } catch (Exception ex) { }
        }

        if (is == null) {
            showAlert("Lỗi đọc tệp", "Không tìm thấy dữ liệu tại: " + resourcePath);
            return;
        }
        try (Reader reader = new java.io.InputStreamReader(is, java.nio.charset.StandardCharsets.UTF_8)) {
            Type listType = new TypeToken<List<SocialMediaPost>>(){}.getType();
            List<SocialMediaPost> importedPosts = gson.fromJson(reader, listType);
            
            if (importedPosts != null && !importedPosts.isEmpty()) {
                collectedPosts.clear();
                collectedPosts.addAll(importedPosts);
                postsTable.setItems(FXCollections.observableArrayList(collectedPosts));
                
                statusLabel.setText("Đã nạp " + collectedPosts.size() + " bài viết lịch sử từ " + selected);
                System.out.println("[+] Đã nạp thành công dữ liệu lịch sử.");
                
                if (onHistoricalDataLoadedCallback != null) {
                    onHistoricalDataLoadedCallback.run();
                }
            } else {
                showAlert("Dữ liệu trống", "Tệp dữ liệu không chứa bài viết hoặc sai định dạng.");
            }

        } catch (Exception e) {
            System.err.println("[!] Lỗi khi đọc file JSON: " + e.getMessage());
            e.printStackTrace();
            showAlert("Lỗi đọc tệp", "Không thể nạp dữ liệu: " + e.getMessage());
        }
    }

    private void showAlert(String title, String msg) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION, msg, ButtonType.OK);
        alert.setTitle(title);
        alert.showAndWait();
    }
}
