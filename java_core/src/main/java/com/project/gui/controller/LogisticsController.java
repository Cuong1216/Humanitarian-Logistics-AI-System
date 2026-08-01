package com.project.gui.controller;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.project.gui.MockDataProvider;
import com.project.logistics.entities.DistressPoint;
import com.project.logistics.entities.Location;
import com.project.logistics.entities.SupportCenter;
import com.project.logistics.utils.RouteFinder;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Worker;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;

import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;

@Component
public class LogisticsController implements Initializable {

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

    private WebEngine webEngine;
    private boolean mapLoaded = false;
    private ObservableList<SupportCenter> supportCenterList = FXCollections.observableArrayList();
    private ObservableList<DistressPoint> distressList = FXCollections.observableArrayList();

    private final RouteFinder routeFinder;

    @org.springframework.beans.factory.annotation.Autowired
    public LogisticsController(RouteFinder routeFinder) {
        this.routeFinder = routeFinder;
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupLogisticsTables();
        loadSupportCenters();
        // Bản đồ sẽ được tải lưỜi qua loadMapIfNeeded() khi Tab được chọn
    }

    public void updateDistressPoints(List<DistressPoint> points) {
        distressList.clear();
        if (points != null) {
            distressList.addAll(points);
        }
        if (distressTable != null) {
            distressTable.setItems(distressList);
        }
    }

    private void setupLogisticsTables() {
        if (distressTable != null) {
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

            distressTable.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal != null) {
                    matchDistressLocation(newVal);
                }
            });
        }

        if (supportCenterTable != null) {
            colCenterName.setCellValueFactory(new PropertyValueFactory<>("address"));
            colCenterResources.setCellValueFactory(new PropertyValueFactory<>("resourcesString"));
            colCenterVehicles.setCellValueFactory(new PropertyValueFactory<>("vehicleCount"));
        }
    }

    private void loadSupportCenters() {
        supportCenterList.clear();
        supportCenterList.addAll(MockDataProvider.getMockSupportCenters());
        if (supportCenterTable != null) {
            supportCenterTable.setItems(supportCenterList);
        }
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
            
            if (webEngine != null) {
                try {
                    String script = String.format(Locale.US,
                        "setRoute(%f,%f,%f,%f,'%s','%s');",
                        bestCenter.getLatitude(), bestCenter.getLongitude(),
                        dp.getLatitude(), dp.getLongitude(),
                        bestCenter.getAddress().split(" \\(")[0].replace("'","\\'"),
                        dp.getAddress().replace("'","\\'")
                    );
                    webEngine.executeScript(script);
                } catch (Exception e) {
                    System.err.println("[Map] setRoute failed: " + e.getMessage());
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
    private void onFindRoute() {
        String startStr = startLocField.getText().trim();
        String destStr = destLocField.getText().trim();

        if (startStr.isEmpty() || destStr.isEmpty()) {
            showAlert("Xác thực dữ liệu", "Vui lòng nhập điểm xuất phát và điểm đến.");
            return;
        }

        routeOutput.setText("Đang tìm vị trí và vẽ lộ trình...");

        Thread routeThread = new Thread(() -> {
            Location start = geocodeAddressWithFallback(startStr);
            Location dest = geocodeAddressWithFallback(destStr);

            Platform.runLater(() -> {
                List<Location> route = routeFinder.AStarRouteFinder(start, dest);

                StringBuilder sb = new StringBuilder("Thông tin Lộ trình Cứu trợ:\n");
                sb.append(String.format("  Xuất phát: %s (%.6f, %.6f)%n", start.getAddress(), start.getLatitude(), start.getLongitude()));
                sb.append(String.format("  Điểm đến:  %s (%.6f, %.6f)%n%n", dest.getAddress(), dest.getLatitude(), dest.getLongitude()));
                sb.append("Đường vẽ lộ trình trực quan đã được hiển thị trên bản đồ OpenStreetMap.");
                routeOutput.setText(sb.toString());

                if (webEngine != null) {
                    try {
                        String script = String.format(Locale.US,
                            "setRoute(%f,%f,%f,%f,'%s','%s');",
                            start.getLatitude(), start.getLongitude(),
                            dest.getLatitude(), dest.getLongitude(),
                            start.getAddress().replace("'","\\'"),
                            dest.getAddress().replace("'","\\'")
                        );
                        webEngine.executeScript(script);
                    } catch (Exception e) {
                        System.err.println("[Map] onFindRoute JS failed: " + e.getMessage());
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
                .header("User-Agent", "DisasterReliefSystem/1.0 (muffin@example.com)") 
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

    @FXML
    private void onResetMap() {
        if (distressTable != null) distressTable.getSelectionModel().clearSelection();
        if (supportCenterTable != null) supportCenterTable.getSelectionModel().clearSelection();
        startLocField.clear();
        destLocField.clear();
        matchedCenterLabel.setText("Chưa chọn");
        matchedVehicleLabel.setText("Chưa chọn");
        routeOutput.setText("Đã xóa bản đồ và các thiết lập lựa chọn.");
        
        if (webEngine != null) {
            try { webEngine.executeScript("clearMap(); map.setView([14.0583,108.2772],6);");
            } catch (Exception e) {}
        }
    }

    /**
     * Lazy-load bản đồ: chỉ tải HTML khi Tab điều phối được nhấp lần đầu.
     * Lúc này WebView đã có kích thước thật nên Leaflet tậnh đúng size → không bị vỡ tile.
     */
    public void loadMapIfNeeded() {
        if (mapLoaded || mapWebView == null) return;
        mapLoaded = true;

        webEngine = mapWebView.getEngine();
        URL url = getClass().getResource("/com/project/gui/resources/html/map.html");
        if (url == null) {
            System.err.println("[!] map.html not found in classpath!");
            return;
        }

        webEngine.getLoadWorker().stateProperty().addListener((obs, old, state) -> {
            if (state == Worker.State.SUCCEEDED) {
                // Lắng nghe sự thay đổi chiều rộng thực tế của WebView
                mapWebView.widthProperty().addListener((wObs, oldWidth, newWidth) -> {
                    if (newWidth.doubleValue() > 0) {
                        Platform.runLater(() -> {
                            try { webEngine.executeScript("setTimeout(function(){ if (typeof map !== 'undefined') map.invalidateSize(true); }, 300);"); } catch (Exception e) {}
                        });
                    }
                });

                // Lắng nghe sự thay đổi chiều cao thực tế của WebView
                mapWebView.heightProperty().addListener((hObs, oldHeight, newHeight) -> {
                    if (newHeight.doubleValue() > 0) {
                        Platform.runLater(() -> {
                            try { webEngine.executeScript("setTimeout(function(){ if (typeof map !== 'undefined') map.invalidateSize(true); }, 300);"); } catch (Exception e) {}
                        });
                    }
                });
                
                // Gọi thử một lần trong trường hợp WebView đã có kích thước ngay lúc load xong
                Platform.runLater(() -> {
                    try { webEngine.executeScript("setTimeout(function(){ if (typeof map !== 'undefined') map.invalidateSize(true); }, 500);"); } catch (Exception e) {}
                });
            }
        });
        webEngine.load(url.toExternalForm());
    }

    private void showAlert(String title, String msg) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION, msg, ButtonType.OK);
        alert.setTitle(title);
        alert.showAndWait();
    }
}
