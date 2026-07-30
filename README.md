# Humanitarian-Logistics-AI-System
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
