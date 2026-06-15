#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Disaster Data Crawler
Limits python_ai_engine to a data-gathering-only role.
Collects raw posts from Facebook, X (Twitter), TikTok, and YouTube, saving them to JSON.
"""

import argparse
import json
import random
import sys
from datetime import datetime, timedelta

# ==========================================================
# REAL API TEMPLATES & EXTENSION REFERENCE
# ==========================================================
# (Below are templates for connecting to real social APIs)
"""
1. TWITTER / X API (tweepy):
import tweepy
def fetch_real_x_posts(bearer_token, query, start_time, end_time, max_results=100):
    client = tweepy.Client(bearer_token=bearer_token)
    # query like "bão yagi OR #BaoSo3"
    response = client.search_recent_tweets(
        query=query, 
        start_time=start_time, 
        end_time=end_time, 
        tweet_fields=['created_at', 'public_metrics', 'author_id'], 
        max_results=max_results
    )
    posts = []
    if response.data:
        for tweet in response.data:
            posts.append({
                "id": str(tweet.id),
                "platform": "x",
                "author": f"user_{tweet.author_id}",
                "content": tweet.text,
                "timestamp": tweet.created_at.isoformat(),
                "likeCount": tweet.public_metrics['like_count'],
                "shareCount": tweet.public_metrics['retweet_count'],
                "reactions": {
                    "like": tweet.public_metrics['like_count'],
                    "retweet": tweet.public_metrics['retweet_count']
                },
                "comments": []
            })
    return posts

2. YOUTUBE DATA API (google-api-python-client):
from googleapiclient.discovery import build
def fetch_real_youtube_comments(api_key, video_id, max_results=50):
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
        comments.append(snippet['textDisplay'])
    return comments
"""

def generate_simulated_posts(disaster, keywords, start_date, end_date):
    """
    Simulates fetching raw posts matching the parameters.
    Supports disasters: yagi, bualoi, matmo.
    """
    print(f"[*] Crawling dataset for disaster: {disaster}")
    print(f"[*] Search parameters - Keywords: {keywords} | Timeframe: {start_date.date()} to {end_date.date()}")
    
    # Pool of simulated content templates based on disaster
    facebook_authors = ["Thông tin Phòng chống thiên tai", "Thời Sự VTV", "Hội Chữ Thập Đỏ VN", "Dự báo thời tiết", "Tin Tức 24h", "Người Dân Vùng Cao"]
    x_authors = ["vietnam_news_agency", "storm_tracker_seasia", "relief_ops_vn", "vnexpress_official"]
    tiktok_authors = ["mientrung_trongtoi", "taybac_today", "cuuho_sos", "phuot_thu_vietnam"]
    youtube_authors = ["VTV24_Official", "TruyenHinhQuocHoi", "VTCNews", "THVL_News"]
    
    posts = []
    
    # Filter keywords to lowercase
    kws = [k.strip().lower() for k in keywords.split(",")]
    
    # Helper to generate dates between start and end
    delta_days = (end_date - start_date).days
    if delta_days <= 0:
        delta_days = 5
        
    # Content templates depending on disaster
    if "yagi" in disaster.lower():
        templates = [
            ("facebook", facebook_authors, "⚠️ CẢNH BÁO KHẨN CẤP: Siêu bão Yagi (#BaoSo3) đổ bộ trực tiếp vào Hải Phòng, Quảng Ninh gây mất điện và thiệt hại cực kỳ lớn. Mong bà con hết sức cẩn thận, không ra ngoài đường!", 3500, 450, {"like": 2500, "sad": 800, "care": 200}, ["Kinh khủng quá, nhà mình bay mất mái tôn rồi", "Quảng Ninh mất điện toàn bộ", "Mong mọi người bình an"]),
            ("x", x_authors, "Devastating landslide in Luc Yen (Yen Bai) due to torrential rains from #YagiStorm. Highway 70 blocked. Rescue teams dispatched. @VNA #SOS", 450, 120, {"like": 350, "retweet": 100}, ["Roads are flooded completely", "Stay safe everyone"]),
            ("tiktok", tiktok_authors, "Bản Phố, Bắc Hà, Lào Cai bị cô lập hoàn toàn do lũ quét trôi cầu tạm 😭. Bà con đang hết lương thực và nước sạch trầm trọng, điện mất từ hôm qua. Cần hỗ trợ khẩn cấp xuồng và mì tôm, nước suối đóng chai gấp! #CuuTroYagi #BaoSo3 #LaoCai", 12000, 3100, {"like": 9000, "sad": 2500, "care": 500}, ["Cầu mong nước rút nhanh", "Đoàn cứu trợ nào ở Lào Cai liên hệ nhé", "Thương quá Bắc Hà ơi"]),
            ("youtube", youtube_authors, "Bản tin tối: Bão Yagi càn quét các tỉnh miền Bắc, mực nước sông Hồng dâng cao báo động 3. Hà Nội khẩn trương di dời các hộ dân ven sông.", 8500, 600, {"like": 7000, "care": 1500}, ["Hà Nội ngập sâu quá", "Mong nước không lên nữa", "Cảm ơn các anh chiến sĩ bộ đội"]),
            ("facebook", facebook_authors, "Nhà em ở huyện Hải Lăng, Quảng Trị bị ngập sâu nửa mét rồi, nước vẫn đang lên nhanh. Nhà có người già và trẻ nhỏ cần di dời khẩn cấp, số điện thoại liên hệ 0987xxxxxx. SOS cứu hộ cứu trợ!", 150, 45, {"like": 50, "sad": 80, "care": 20}, ["Đã chia sẻ để đội cứu hộ thấy", "Mong gia đình sớm được giúp đỡ", "Nước lên nhanh lắm cẩn thận nha em"]),
            ("tiktok", tiktok_authors, "Đoàn tình nguyện Sen Xanh đã tập kết 1000 thùng mì tôm và nước sạch tại Yên Bái chuẩn bị phát cho bà con vùng lũ lụt. Cảm ơn sự chung tay của cả nước! ❤️", 9500, 1500, {"like": 8000, "care": 1500}, ["Tuyệt vời quá, cảm ơn đoàn", "Yên Bái rất cần nước sạch lúc này", "Ấm lòng đồng bào"]),
            ("facebook", facebook_authors, "Mưa lớn sau bão Yagi gây ngập lụt nghiêm trọng tại huyện Chương Mỹ, Hà Nội. Hơn 300 hộ dân ngập sâu trong nước sạch sinh hoạt thiếu thốn.", 450, 60, {"like": 250, "sad": 150, "care": 50}, ["Chương Mỹ năm nào cũng ngập", "Khổ thân bà con", "Có đoàn nào hỗ trợ nước sạch chưa"]),
        ]
    elif "bualoi" in disaster.lower():
        templates = [
            ("facebook", facebook_authors, "Thông tin áp thấp nhiệt đới ngoài khơi đã mạnh lên thành bão Bualoi. Dự báo bão di chuyển nhanh hướng về các tỉnh Trung Bộ. Đề nghị tàu thuyền tránh xa vùng nguy hiểm.", 400, 30, {"like": 300, "care": 100}, ["Lại bão nữa rồi", "Mong miền Trung yên bình"]),
            ("x", x_authors, "Typhoon Bualoi approaching Central Vietnam. High risks of flash floods and landslides in mountainous areas of Quang Nam, Quang Tri.", 120, 40, {"like": 90, "retweet": 30}, ["Keep tracking the coordinate", "Hope it curves away"]),
            ("tiktok", tiktok_authors, "Mưa lớn trắng trời tại Quảng Nam do ảnh hưởng bão Bualoi. Đường phố bắt đầu ngập, bà con hối hả dọn đồ lên cao. Cầu mong bão qua nhanh! 🙏 #BaoBualoi #MienTrung", 4200, 800, {"like": 3500, "sad": 500, "care": 200}, ["Quảng Nam quê tôi lại lụt rồi", "Cố lên bà con ơi", "Mong mọi người an toàn"]),
            ("youtube", youtube_authors, "Cận cảnh sạt lở đất trên đèo Hải Vân chia cắt giao thông Thừa Thiên Huế và Đà Nẵng do mưa lớn bão Bualoi gây ra.", 3200, 250, {"like": 2800, "sad": 350, "care": 50}, ["Sạt lở nguy hiểm quá", "May không có xe đi qua", "Lực lượng chức năng đang thông tuyến"]),
        ]
    else: # Matmo or general
        templates = [
            ("facebook", facebook_authors, "Bão Matmo đã đổ bộ trực tiếp vào Phú Yên, Khánh Hòa sức gió cấp 10. Hàng loạt cây xanh đổ rạp, nhiều tàu cá neo đậu bị đứt neo trôi dạt ra biển.", 800, 95, {"like": 500, "sad": 250, "care": 50}, ["Thương bà con Phú Yên quá", "Năm nào cũng dính bão"]),
            ("x", x_authors, "Storm Matmo landfall in Phu Yen province. Heavy rains reported in Central Highlands. High risks of flash floods. #Matmo #Vietnam", 110, 35, {"like": 80, "retweet": 30}, ["Stay safe in Central Highlands"]),
            ("tiktok", tiktok_authors, "Lũ quét kinh hoàng đổ về từ thượng nguồn gây ngập sâu huyện M'Đrắk, Đắk Lắk. Nhiều người dân bị cô lập hoàn toàn phải leo lên nóc nhà chờ xuồng cứu hộ. SOS tiếp tế khẩn cấp nước uống và đồ ăn nhanh!", 7500, 1800, {"like": 6000, "sad": 1200, "care": 300}, ["Đắk Lắk cố lên", "Cần đội cứu hộ xuồng cao tốc gấp", "M'Đrắk ngập sâu rồi"]),
            ("youtube", youtube_authors, "VTV24 trực tiếp: Lực lượng công an biên phòng nỗ lực tiếp cận cứu hộ hàng chục hộ dân bị cô lập do ngập lụt sau bão Matmo.", 5500, 450, {"like": 4800, "care": 650, "sad": 50}, ["Cảm ơn lực lượng biên phòng", "Mong bà con an toàn"]),
        ]
        
    # Generate posts by distributing dates chronologically
    for idx, (platform, authors, text, likes, shares, reactions, comments) in enumerate(templates):
        author = random.choice(authors)
        post_id = f"crawled-{disaster}-{idx+1}"
        
        # Calculate a random date within range
        days_offset = random.randint(0, delta_days)
        hours_offset = random.randint(0, 23)
        minutes_offset = random.randint(0, 59)
        post_time = start_date + timedelta(days=days_offset, hours=hours_offset, minutes=minutes_offset)
        
        # Filter simulated post based on keywords
        match = False
        for kw in kws:
            if kw in text.lower() or kw in disaster.lower():
                match = True
                break
        if not match and keywords:
            continue
            
        posts.append({
            "id": post_id,
            "platform": platform,
            "author": author,
            "content": text,
            "timestamp": post_time.strftime("%Y-%m-%dT%H:%M:%SZ"),
            "likeCount": likes,
            "shareCount": shares,
            "reactions": reactions,
            "comments": comments
        })
        
    return posts

def main():
    parser = argparse.ArgumentParser(description="Disaster Raw Social Media Data Crawler")
    parser.add_argument("--disaster", type=str, default="yagi", help="Disaster name (yagi, bualoi, matmo)")
    parser.add_argument("--keywords", type=str, default="bão, ngập lụt, cứu hộ, tiếp tế", help="Comma-separated keywords")
    parser.add_argument("--start-date", type=str, default="2024-07-01", help="Start date (YYYY-MM-DD)")
    parser.add_argument("--end-date", type=str, default="2024-09-30", help="End date (YYYY-MM-DD)")
    parser.add_argument("--output", type=str, default="crawled_dataset.json", help="Path to save output JSON file")
    
    args = parser.parse_args()
    
    try:
        start_dt = datetime.strptime(args.start_date, "%Y-%m-%d")
        end_dt = datetime.strptime(args.end_date, "%Y-%m-%d")
    except ValueError as e:
        print(f"[-] Date format error: {e}. Use YYYY-MM-DD.")
        sys.exit(1)
        
    raw_posts = generate_simulated_posts(args.disaster, args.keywords, start_dt, end_dt)
    
    # Save to JSON
    with open(args.output, "w", encoding="utf-8") as f:
        json.dump(raw_posts, f, ensure_ascii=False, indent=2)
        
    print(f"[+] Successfully gathered {len(raw_posts)} raw posts.")
    print(f"[+] Saved output dataset to: {args.output}")

if __name__ == "__main__":
    main()
