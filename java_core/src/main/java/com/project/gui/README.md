# Disaster Relief System — JavaFX GUI

## Project Structure

```
DisasterReliefSystem/
├── pom.xml
└── src/
    └── main/
        ├── java/
        │   └── com/disaster/
        │       ├── MainApp.java                          ← Entry point
        │       │
        │       ├── gui/
        │       │   └── AppController.java               ← FXML controller
        │       │
        │       ├── datacollection/
        │       │   ├── model/
        │       │   │   └── SocialMediaPost.java
        │       │   └── platform/
        │       │       ├── Platform.java                ← interface
        │       │       ├── Facebook.java
        │       │       ├── Twitter.java
        │       │       └── PlatformSetting.java
        │       │
        │       ├── analysis/
        │       │   ├── TaskAnalyzer.java                ← interface
        │       │   ├── AnalysisResult.java
        │       │   ├── AnalysisManager.java
        │       │   ├── SentimentAnalyzeOverTime.java
        │       │   ├── DamageCategorizer.java
        │       │   └── ReliefSentimentAnalyzer.java
        │       │
        │       ├── logistics/
        │       │   ├── entities/
        │       │   │   ├── Location.java
        │       │   │   ├── Vehicle.java
        │       │   │   └── SupportCenter.java
        │       │   └── utils/
        │       │       └── RouteFinder.java
        │       │
        │       └── ai_client/
        │           ├── AiClient.java
        │           ├── AIRequestDTO.java
        │           └── AIResponseDTO.java
        │
        └── resources/
            └── com/disaster/gui/
                ├── fxml/
                │   └── MainView.fxml                    ← Main UI layout
                └── css/
                    └── style.css                        ← Stylesheet
```

## GUI Tabs

| Tab | Chức năng |
|-----|-----------|
| 📡 Data Collection | Nhập keyword, chọn nền tảng (Facebook/Twitter), fetch posts vào bảng |
| 🧠 Analysis | Chọn analyzer (Sentiment / Damage / Relief), chạy phân tích, xem kết quả |
| 🚛 Logistics | Tìm route A*, xem danh sách phương tiện và trạng thái |

## Yêu cầu

- Java 17+
- Maven 3.8+
- JavaFX 17 (được tự động tải qua Maven)

## Chạy ứng dụng

```bash
cd DisasterReliefSystem
mvn clean javafx:run
```

## Hoặc compile và run thủ công

```bash
mvn clean package
java --module-path /path/to/javafx-sdk/lib \
     --add-modules javafx.controls,javafx.fxml \
     -jar target/DisasterReliefSystem-1.0-SNAPSHOT.jar
```
