# KeyEmotion_through_SocialMedia
---
## Our Goal 🎯
  In today age, social media had become one of the main and important way people express feeling, opinion and interest. So based on that motive, we made an AI system for fast, accuracy detect crowd emotion through post, react, comments, ... on social media like Facebook, Twitter (commonly know as X), ...

  Our goal is based on our detection on bad emotion on a specific keyword for improving the efficiency of Humanitarian Logistics. for example: "village *XYZ* being badly damaged by the Yagi storm and the resident really need food and water right away", so based on that we could send a trucks contain food, water and medical supply to exactly that XYZ village right away.
  
---
## How our system works 🛠

### Project tree 🌲
```bash
KeyEmotion_through_SocialMedia/
├── LICENSE
├── README.md
├── docs
│   ├── AI_Report_Model_Search.pdf
│   └── OOP_Report_UML.pdf
├── java_core
│   ├── pom.xml
│   └── src
│       └── main
│           └── java
│               └── com
│                   └── project
│                       ├── Main.java
│                       ├── ai_client
│                       │   ├── AiResponseDto.java
│                       │   └── AiRestClient.java
│                       ├── datacollection
│                       │   ├── PlatformFactory.java
│                       │   ├── model
│                       │   │   └── SocialMediaPost.java
│                       │   └── platform
│                       │       ├── FacebookScraper.java
│                       │       ├── Platform.java
│                       │       └── XScraper.java
│                       └── logistics
│                           ├── entities
│                           │   ├── Location.java
│                           │   ├── ReliefCenter.java
│                           │   └── Vehicle.java
│                           └── search
│                               └── AStarRouteFinder.java
└── python_ai_engine
    ├── main.py
    ├── model
    │   ├── classifier.py
    │   ├── train.py
    │   └── trained_model.pkl
    ├── nlp_processor
    │   ├── entity_extractor.py
    │   └── text_cleaner.py
    └── requirements.txt
```
