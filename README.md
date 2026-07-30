# Humanitarian-Logistics-AI-System

> **Ghi chú về Nguồn gốc & Phát triển (v2.0)** 🚀
> 
> Dự án này khởi đầu là một đồ án môn học được thực hiện cùng nhóm tác giả ban đầu. Bạn có thể tham khảo mã nguồn gốc tại [Tom984-vn/KeyEmotion_through_SocialMedia](https://github.com/Tom984-vn/KeyEmotion_through_SocialMedia).
> 
> Ở phiên bản hiện tại (v2.0), dự án đã được tôi phát triển độc lập, tái cấu trúc (refactoring) mạnh mẽ và nâng cấp thành một hệ thống Client-Server hoàn chỉnh. Các tính năng nổi bật được bổ sung bao gồm: giao diện GUI hiện đại (JavaFX), tích hợp mô hình AI NLP & KNN nâng cao, và hệ thống bản đồ điều phối cứu trợ (Logistics Routing) thời gian thực.

---
## Our Goal 🎯
  In today age, social media had become one of the main and important way people express feeling, opinion and interest. So based on that motive, we made an AI system for fast, accuracy detect crowd emotion through post, react, comments, ... on social media like Facebook, Twitter (commonly know as X), ...

  Our goal is based on our detection on bad emotion on a specific keyword for improving the efficiency of Humanitarian Logistics. for example: "village *XYZ* being badly damaged by the Yagi storm and the resident really need food and water right away", so based on that we could send a trucks contain food, water and medical supply to exactly that XYZ village right away.
  
---
## How our system works 🛠

### Project tree 🌲
```bash
Humanitarian-Logistics-AI-System/
├── docs/
│   ├── OOP_Report_UML.pdf
│   └── AI_Report_Model_Search.pdf
│
├── java_core/
│   ├── pom.xml
│   └── src/main/java/com/project/
│       ├── Main.java
│       │
│       ├── datacollection/
│       │   ├── model/
│       │   │   └── SocialMediaPost.java
│       │   ├── platform/
│       │   │   ├── Platform.java
│       │   │   ├── FacebookScraper.java
│       │   │   └── XScraper.java
│       │   └── PlatformFactory.java
│       │
│       ├── ai_client/
│       │   ├── AiRestClient.java
│       │   └── AiResponseDto.java
│       │
│       └── logistics/
│           ├── entities/
│           │   ├── Location.java
│           │   ├── Vehicle.java
│           │   └── ReliefCenter.java
│           │
│           └── search/
│               └── AStarRouteFinder.java
│
└── python_ai_engine/
    ├── requirements.txt
    ├── main.py
    ├── schemas.py
    │
    ├── services/
    │   ├── nlp_service.py
    │   ├── sentiment_service.py
    │   └── categorization_service.py
    │
    └── models/
```
