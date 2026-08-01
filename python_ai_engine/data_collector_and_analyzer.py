"""
Disaster Data Collector & Preprocessing Analyzer
Selected Disaster: Typhoon Yagi (Bão số 3) - Q3 2024
"""

import re
import unicodedata

# ==========================================
# PART 1: REAL SOCIAL MEDIA API TEMPLATES
# ==========================================
# (These templates show how you can fetch real data using official Python libraries)

"""
1. TWITTER (X) SCRAPING:
import tweepy
def fetch_twitter_data(api_key, api_secret, access_token, access_token_secret, query, max_results=100):
    client = tweepy.Client(bearer_token="YOUR_BEARER_TOKEN")
    response = client.search_recent_tweets(query=query, tweet_fields=['created_at', 'public_metrics'], max_results=max_results)
    tweets = []
    if response.data:
        for tweet in response.data:
            tweets.append({
                "id": tweet.id,
                "text": tweet.text,
                "created_at": tweet.created_at.isoformat(),
                "platform": "x",
                "retweets": tweet.public_metrics['retweet_count'],
                "likes": tweet.public_metrics['like_count']
            })
    return tweets

2. YOUTUBE COMMENT/VIDEO SCRAPING:
from googleapiclient.discovery import build
def fetch_youtube_comments(api_key, video_id, max_results=50):
    youtube = build('youtube', 'v3', developerKey=api_key)
    request = youtube.commentThreads().list(
        part="snippet",
        videoId=video_id,
        maxResults=max_results
    )
    response = request.execute()
    comments = []
    for item in response.get('items', []):
        snippet = item['snippet']['topLevelComment']['snippet']
        comments.append({
            "author": snippet['authorDisplayName'],
            "text": snippet['textDisplay'],
            "published_at": snippet['publishedAt'],
            "likes": snippet['likeCount']
        })
    return comments
"""

# ==========================================
# PART 2: BUILT-IN CRAWLER / EVENT DATASET
# ==========================================
# Simulated data collector for Typhoon Yagi (July - Sept 2024) across platforms

class DisasterDataCollector:
    def __init__(self, campaign="Bão Yagi 2024"):
        self.campaign = campaign
        self.keywords = ["bão yagi", "bão số 3", "ngập lụt", "lũ quét", "sạt lở", "cứu hộ", "tiếp tế"]
        self.hashtags = ["#YagiStorm", "#BaoSo3", "#CuuTroLuLut", "#BaoVeMienBac", "#LaoCaiNgapLut"]

    def fetch_simulated_posts(self):
        """Simulates raw, un-cleaned posts fetched from Facebook, X (Twitter), TikTok, and YouTube"""
        raw_posts = [
            {
                "id": "fb-yagi-101",
                "platform": "facebook",
                "author": "Đài Khí Tượng Thủy Văn Bắc Bộ",
                "text": "⚠️ CẢNH BÁO KHẨN CẤP: Siêu bão Yagi (#BaoSo3) đang tiến sát Vịnh Bắc Bộ với sức gió giật cấp 17. Dự báo từ ngày 7/9/2024, khu vực Hải Phòng, Quảng Ninh sẽ chịu ảnh hưởng trực tiếp. Đề nghị bà con neo đậu tàu thuyền ngay lập tức!!! Xem thêm tại https://kttv.gov.vn",
                "created_at": "2024-09-06T08:30:00Z",
                "reactions": {"like": 1200, "care": 450, "sad": 10},
                "comments": [
                    "Bão này mạnh quá, mong mọi người cẩn thận.",
                    "Hải Phòng bắt đầu có gió rít rồi bà con ơi.",
                    "Hy vọng bão đổi hướng giảm cấp."
                ]
            },
            {
                "id": "x-yagi-102",
                "platform": "x",
                "author": "vietnam_news_agency",
                "text": "Landslide reported in Luc Yen, Yen Bai province after heavy rains from #YagiStorm. Road blocked, rescue teams are on their way. Please avoid highway 70. @VNA_English #BaoSo3",
                "created_at": "2024-09-08T10:15:00Z",
                "reactions": {"like": 450, "retweet": 120},
                "comments": [
                    "Luc Yen is flooded heavily right now.",
                    "Hope everyone stays safe in Yen Bai."
                ]
            },
            {
                "id": "tiktok-yagi-103",
                "platform": "tiktok",
                "author": "review_tay_bac",
                "text": "Lũ quét cuốn trôi cầu tạm tại xã Bản Phố, huyện Bắc Hà, Lào Cai 😭 Cả bản đang bị cô lập hoàn toàn rồi mọi người ơi. Lương thực dự trữ chỉ còn hết ngày mai thôi, nước dâng cao ngập hết ruộng lúa rồi. Mong chính quyền cứu hộ tiếp tế lương thực và nước sạch gấp ạ! #CuuTroLuLut #BaoSo3 #YagiStorm #LaoCai",
                "created_at": "2024-09-08T14:20:00Z",
                "reactions": {"like": 15000, "share": 3400},
                "comments": [
                    "Thương quá Bản Phố ơi, cầu mong nước mau rút.",
                    "Có đoàn cứu trợ nào gần Bắc Hà liên hệ chính quyền bản đi ạ.",
                    "Cần tiếp tế mì tôm nước suối đóng chai gấp!"
                ]
            },
            {
                "id": "youtube-yagi-104",
                "platform": "youtube",
                "author": "VTV24_Official",
                "text": "Bản tin tối 07/09/2024: Siêu bão Yagi đổ bộ Quảng Ninh, Hải Phòng gây thiệt hại nặng nề. Hàng loạt cây đổ, nhà tốc mái. Xem trực tiếp tình hình tại đây: https://youtube.com/watch?v=vtv24live #VTV24 #BaoSo3 #YagiStorm",
                "created_at": "2024-09-07T19:00:00Z",
                "reactions": {"like": 8500, "dislike": 20},
                "comments": [
                    "Kinh khủng quá, gió thổi bay cả mái tôn trước nhà tôi.",
                    "Quảng Ninh mất điện toàn bộ rồi.",
                    "Cầu mong không có thiệt hại lớn về người."
                ]
            },
            {
                "id": "fb-yagi-105",
                "platform": "facebook",
                "author": "Đoàn Thiện Nguyện Sen Xanh",
                "text": "Tin vui: Đoàn chúng tôi đã chuyển thành công 500 thùng mì tôm, 200 thùng nước lọc đóng chai và thuốc men cơ bản tới bà con vùng ngập lụt xã Hải Lăng, Quảng Trị. Cảm ơn sự đồng hành ủng hộ từ các nhà hảo tâm khắp cả nước! ❤️ #CuuTroLuLut #TinhDongBao",
                "created_at": "2024-09-12T16:45:00Z",
                "reactions": {"like": 3200, "love": 1400, "care": 900},
                "comments": [
                    "Tuyệt vời quá, cảm ơn đoàn nhiều ạ.",
                    "Hải Lăng đang ngập sâu, nhận được tiếp tế thật quý giá."
                ]
            },
            {
                "id": "fb-yagi-106",
                "platform": "facebook",
                "author": "Ngo_Van_Binh",
                "text": "Nhà em ở Thường Xuân, Thanh Hóa đang bị nước lũ tràn vào nhà ngập nửa mét rồi ạ. Điện mất từ trưa, sạc dự phòng sắp hết pin. Cần xuồng cứu hộ đưa mẹ em lớn tuổi và trẻ nhỏ di dời gấp. Ai ở gần giúp gia đình em với ạ! Số điện thoại liên hệ: 0912xxxxxx. Cứu hộ khẩn cấp!!! #CuuTroLuLut #ThanhHoa #SOS",
                "created_at": "2024-09-08T22:10:00Z",
                "reactions": {"like": 85, "sad": 340, "care": 110},
                "comments": [
                    "Đã chia sẻ bài viết mong cứu hộ tiếp cận sớm.",
                    "Thường Xuân nước đang lên nhanh lắm, cẩn thận nha bạn.",
                    "Mong gia đình bình an thoát lũ."
                ]
            }
        ]
        return raw_posts


# ==========================================
# PART 3: DATA PREPROCESSING PIPELINES
# ==========================================

class TextPreprocessor:
    def __init__(self):
        # Sample common Vietnamese stopwords to remove for NLP processing
        self.vietnamese_stopwords = {
            "và", "là", "thì", "mà", "của", "được", "bị", "có", "trong", "cho",
            "ở", "này", "cơ", "những", "các", "cái", "ra", "với", "tại", "vào"
        }

    def pipeline_a_basic_clean(self, text: str) -> str:
        """Pipeline 1: Strip URLs, Mentions, Extra spacing, and Normalize Unicode"""
        text = unicodedata.normalize("NFC", text or "")
        # Remove URLs
        text = re.sub(r"https?://\S+|www\.\S+", " ", text, flags=re.IGNORECASE)
        # Remove Mentions (@username)
        text = re.sub(r"@\w+", " ", text)
        # Unwrap hashtags (#BaoSo3 -> BaoSo3)
        text = re.sub(r"#(\w+)", r"\1", text)
        # Normalize spaces
        text = re.sub(r"\s+", " ", text)
        return text.strip()

    def pipeline_b_stopwords_removed(self, text: str) -> str:
        """Pipeline 2: Basic cleaning + lowercase + Vietnamese stopwords removal"""
        cleaned = self.pipeline_a_basic_clean(text).lower()
        # Remove punctuation for word-token splitting
        words = re.findall(r"\b\w+\b", cleaned)
        filtered_words = [w for w in words if w not in self.vietnamese_stopwords]
        return " ".join(filtered_words)

    def pipeline_c_feature_tokenization(self, text: str) -> dict:
        """Pipeline 3: Segment text and extract special metadata features (Hashtags, Mentions, Emojis)"""
        raw_text = text or ""
        # Find raw hashtags
        hashtags = re.findall(r"#\w+", raw_text)
        # Find raw mentions
        mentions = re.findall(r"@\w+", raw_text)
        # Basic clean remaining text
        cleaned_text = self.pipeline_a_basic_clean(raw_text)
        
        # Tokenize words
        tokens = cleaned_text.lower().split()
        
        return {
            "clean_text": cleaned_text,
            "tokens": tokens,
            "hashtags": hashtags,
            "mentions": mentions,
            "token_count": len(tokens)
        }


# ==========================================
# PART 4: SOLVING HIERARCHICAL DISASTER PROBLEMS
# ==========================================

class DisasterTaskSolver:
    def __init__(self):
        # Keywords suggesting categories
        self.category_keywords = {
            "food": ["lương thực", "mì tôm", "gạo", "đồ ăn", "lương khô", "food"],
            "water": ["nước", "nước sạch", "nước suối", "nước lọc", "water"],
            "medical": ["y tế", "thuốc", "bông băng", "kháng sinh", "trạm y tế", "medical"],
            "shelter": ["bạt", "lều", "trú ẩn", "tốc mái", "sập nhà", "shelter"],
            "rescue": ["cứu hộ", "xuồng", "phao", "mắc kẹt", "lũ quét", "sạt lở", "rescue"]
        }
        
        # Keywords suggesting critical emergency
        self.emergency_keywords = ["cứu", "cấp cứu", "khẩn cấp", "mắc kẹt", "lũ quét", "sạt lở", "sập", "trôi", "cô lập", "sos"]

    # ------------------------------------------
    # TASK 1: Urgency & Emergency Classification
    # ------------------------------------------
    def solve_task_1_urgency(self, clean_text: str, reactions: dict) -> dict:
        text_lower = clean_text.lower()
        
        # Count hit markers
        emergency_hits = sum(1 for kw in self.emergency_keywords if kw in text_lower)
        sad_care_reactions = reactions.get("sad", 0) + reactions.get("care", 0)
        
        # Determine score
        is_emergency = False
        urgency = "low"
        
        if emergency_hits >= 3 or (emergency_hits >= 1 and sad_care_reactions > 200):
            is_emergency = True
            urgency = "critical"
        elif emergency_hits >= 1 or sad_care_reactions > 100:
            is_emergency = True
            urgency = "high"
        elif "ngập" in text_lower or "mưa" in text_lower:
            urgency = "medium"
            
        return {
            "is_emergency": is_emergency,
            "urgency": urgency,
            "emergency_score_hits": emergency_hits
        }

    # ------------------------------------------
    # TASK 2: Supply Demand Categorization
    # ------------------------------------------
    def solve_task_2_demand_categorization(self, clean_text: str) -> list:
        text_lower = clean_text.lower()
        demanded_categories = []
        
        for category, kws in self.category_keywords.items():
            if any(kw in text_lower for kw in kws):
                demanded_categories.append(category)
                
        if not demanded_categories:
            demanded_categories.append("unknown")
            
        return demanded_categories

    # ------------------------------------------
    # TASK 3: Sentiment & Dominant Emotion Analysis
    # ------------------------------------------
    def solve_task_3_sentiment_analysis(self, clean_text: str, reactions: dict) -> dict:
        text_lower = clean_text.lower()
        
        # Calculate negative score from keywords and reactions
        sad_angry = reactions.get("sad", 0) + reactions.get("angry", 0)
        likes = reactions.get("like", 0)
        total_reactions = sum(reactions.values())
        
        reaction_ratio = (sad_angry / total_reactions) if total_reactions > 0 else 0.0
        
        # Text-based indicators
        sadness_triggers = ["chia buồn", "thương tâm", "tử vong", "chết", "mất mát", "😭", "😞"]
        anger_triggers = ["bất bình", "tức giận", "chậm trễ", "phẫn nộ"]
        joy_triggers = ["cảm ơn", "vui mừng", "tốt đẹp", "may mắn", "❤️"]
        
        # Determine dominant emotion
        dominant_emotion = "neutral"
        negative_score = 0.1
        
        if any(w in text_lower for w in sadness_triggers) or reaction_ratio > 0.4:
            dominant_emotion = "sadness"
            negative_score = 0.8
        elif any(w in text_lower for w in anger_triggers):
            dominant_emotion = "anger"
            negative_score = 0.75
        elif any(w in text_lower for w in joy_triggers) and likes > sad_angry:
            dominant_emotion = "joy"
            negative_score = 0.05
        elif any(w in text_lower for w in self.emergency_keywords):
            dominant_emotion = "fear"
            negative_score = 0.90
            
        return {
            "dominant_emotion": dominant_emotion,
            "negative_score": negative_score,
            "sad_angry_ratio": round(reaction_ratio, 2)
        }

    # ------------------------------------------
    # TASK 4: Location Extraction & Priority Ranking
    # ------------------------------------------
    def solve_task_4_priority_ranking(self, analyzed_posts: list) -> list:
        """Gathers posts, groups them by extracted locations, and scores/ranks locations by priority"""
        # Geocode fallback matcher
        vietnam_provinces = ["Lào Cai", "Yên Bái", "Hải Phòng", "Quảng Ninh", "Quảng Trị", "Thanh Hóa"]
        
        location_scores = {}
        
        for post in analyzed_posts:
            text = post["clean_text"]
            # Basic NER mock extractor
            detected_loc = "Khác"
            for prov in vietnam_provinces:
                if prov.lower() in text.lower():
                    detected_loc = prov
                    break
                    
            urgency = post["urgency"]
            neg_score = post["negative_score"]
            
            # Map urgency to points
            urgency_points = {"critical": 0.9, "high": 0.7, "medium": 0.4, "low": 0.1}[urgency]
            post_score = urgency_points * 0.6 + neg_score * 0.4
            
            if detected_loc not in location_scores:
                location_scores[detected_loc] = {
                    "location": detected_loc,
                    "total_score": 0.0,
                    "post_count": 0,
                    "demands": set()
                }
                
            location_scores[detected_loc]["total_score"] += post_score
            location_scores[detected_loc]["post_count"] += 1
            location_scores[detected_loc]["demands"].update(post["demands"])
            
        # Calculate averages and sort by total score
        ranked_locations = []
        for loc, data in location_scores.items():
            avg_score = round(data["total_score"] / data["post_count"], 3)
            ranked_locations.append({
                "location": loc,
                "priority_score": avg_score,
                "post_count": data["post_count"],
                "demands": list(data["demands"])
            })
            
        # Sort desc by score
        ranked_locations.sort(key=lambda x: x["priority_score"], reverse=True)
        return ranked_locations


# ==========================================
# MAIN EXECUTION
# ==========================================

if __name__ == "__main__":
    print("=========================================================")
    print("🚀 DISASTER DATA SCRAPER & NLP PREPROCESSOR DEMONSTRATION")
    print("=========================================================\n")
    
    # 1. Fetch posts
    collector = DisasterDataCollector()
    raw_posts = collector.fetch_simulated_posts()
    print(f"[+] Loaded {len(raw_posts)} raw posts from Typhoon Yagi campaign (Q3 2024).\n")
    
    # Initialize components
    preprocessor = TextPreprocessor()
    solver = DisasterTaskSolver()
    
    analyzed_posts = []
    
    # Process each post
    for post in raw_posts:
        print(f"--- Processing Post ID: {post['id']} ({post['platform'].upper()}) ---")
        print(f"Raw Text: {post['text']}")
        
        # Apply Preprocessing Pipelines
        basic_cleaned = preprocessor.pipeline_a_basic_clean(post['text'])
        stopwords_removed = preprocessor.pipeline_b_stopwords_removed(post['text'])
        feature_metadata = preprocessor.pipeline_c_feature_tokenization(post['text'])
        
        print(f"\n[Preprocessing Option A] Basic Cleaned:\n  {basic_cleaned}")
        print(f"[Preprocessing Option B] Stopwords Removed:\n  {stopwords_removed}")
        print(f"[Preprocessing Option C] Token Count: {feature_metadata['token_count']}")
        print(f"  Hashtags: {feature_metadata['hashtags']}")
        
        # Solve Tasks
        urgency_res = solver.solve_task_1_urgency(basic_cleaned, post['reactions'])
        demand_res = solver.solve_task_2_demand_categorization(basic_cleaned)
        sentiment_res = solver.solve_task_3_sentiment_analysis(basic_cleaned, post['reactions'])
        
        print(f"\n[Task 1: Urgency] Is Emergency: {urgency_res['is_emergency']} | Level: {urgency_res['urgency'].upper()}")
        print(f"[Task 2: Supply Demands] Detected Categories: {demand_res}")
        print(f"[Task 3: Sentiment] Dominant Emotion: {sentiment_res['dominant_emotion'].upper()} | Negativity: {sentiment_res['negative_score']*100}%")
        print("\n" + "="*50)
        
        # Save analysis
        analyzed_posts.append({
            "id": post["id"],
            "clean_text": basic_cleaned,
            "urgency": urgency_res["urgency"],
            "negative_score": sentiment_res["negative_score"],
            "demands": demand_res
        })
        
    # Solve Task 4: Location Priority Ranking
    print("\n[+] Solving Task 4: Humanitarian Logistics Priority Ranking (Aggregated Areas)")
    ranked_areas = solver.solve_task_4_priority_ranking(analyzed_posts)
    
    for idx, area in enumerate(ranked_areas, start=1):
        print(f"  Rank {idx}: {area['location']}")
        print(f"    - Priority Score: {area['priority_score']} (Out of 1.0)")
        print(f"    - Post Count: {area['post_count']}")
        print(f"    - Demanded Supplies: {area['demands']}")
        print()
    
    print("=========================================================")
    print("💡 SYSTEM SUGGESTION FOR JAVA & FASTAPI PROJECT INTEGRATION")
    print("=========================================================")
    print("1. Integrates Preprocessing (Option B) to remove noisy stopwords inside:")
    print("   -> python_ai_engine/services/text_cleaning_service.py")
    print("   This keeps key-phrases clear for Gemini API extraction & KNN training.")
    print("2. Expand KNN Features with token metrics (Option C) inside:")
    print("   -> python_ai_engine/services/knn_severity_service.py")
    print("   Inject features like 'hashtag_count' and 'comment_density' into the classifier.")
    print("=========================================================")
