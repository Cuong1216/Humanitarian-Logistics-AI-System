# KeyEmotion_through_SocialMedia
---
## Our Goal 🎯
  In today age, social media had become one of the main and important way people express feeling, opinion and interest. So based on that motive, we made an AI system for fast, accuracy detect crowd emotion through post, react, comments, ... on social media like Facebook, Twitter (commonly know as X), ...

  Our goal is based on our detection on bad emotion on a specific keyword for improving the effiecentcy of Humanitarian Logistics. for example: village *XYZ* being badly damaged and the resident need food and water from Yagi storms, so based on that we could send a trucks contain food and medical supply to exactly that village right away.
  
---
## How our system works 🛠

### Project tree 🌲
'''bash
KeyEmotion_through_SocialMedia/
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
    │
    ├── nlp_processor/
    │   ├── text_cleaner.py
    │   └── entity_extractor.py
    │
    └── model/
        ├── train.py
        ├── classifier.py
        └── trained_model.pkl
'''
