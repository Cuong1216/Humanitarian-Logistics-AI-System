# -*- coding: utf-8 -*-
"""
Disaster Data Collector & Preprocessing Analyzer
Selected Disaster: Typhoon Yagi (Bão số 3) - Q3 2024
"""

import re
import json
import unicodedata
from datetime import datetime
from collections import Counter

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
                "text": "⚠️ CẢNH BÁO KHẨN CẤP: Siêu bão Yagiraw_posts = [
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
        "id": "fb-yagi-104",
        "platform": "facebook",
        "author": "Thông tin Chính phủ",
        "text": "Thủ tướng Chính phủ yêu cầu các địa phương tuyệt đối không chủ quan trước diễn biến phức tạp của hoàn lưu bão số 3. Đặc biệt lưu ý nguy cơ lũ quét, sạt lở đất tại các tỉnh miền núi phía Bắc: Lào Cai, Yên Bái, Cao Bằng. Các lực lượng quân đội, công an túc trực 24/24 để sẵn sàng ứng cứu nhân dân.",
        "created_at": "2024-09-09T07:00:00Z",
        "reactions": {"like": 25000, "care": 4200},
        "comments": [
            "Đảng và Nhà nước luôn đồng hành cùng nhân dân.",
            "Mong các chú bộ đội giữ gìn sức khỏe để cứu giúp bà con.",
            "Yên Bái đang ngập sâu lắm rồi ạ."
        ]
    },
    {
        "id": "tiktok-yagi-105",
        "platform": "tiktok",
        "author": "hanoi_24h",
        "text": "Gió giật kinh hoàng tại Hà Nội chiều nay. Cây cổ thụ bật gốc la liệt trên phố Phan Đình Phùng, Lò Đúc. Mọi người tuyệt đối không ra đường lúc này nhé, cực kỳ nguy hiểm! 🌪️🌳 #BaoYagi #HaNoi #BaoSo3",
        "created_at": "2024-09-07T16:45:00Z",
        "reactions": {"like": 89000, "share": 12000},
        "comments": [
            "Sợ quá, cửa kính nhà mình rung bần bật.",
            "Mong những người vô gia cư có nơi trú ẩn an toàn.",
            "Đừng ai gọi ship đồ ăn lúc này nha, tội các bạn shipper lắm."
        ]
    },
    {
        "id": "fb-yagi-106",
        "platform": "facebook",
        "author": "Đoàn Thiện Nguyện Ánh Sao",
        "text": "🚨 [GÓC KÊU GỌI] Đêm nay đoàn chúng mình sẽ xuất phát mang 500 áo phao, 1000 thùng nước suối và lương khô lên hỗ trợ bà con vùng rốn lũ Thái Nguyên. Hiện tại chúng mình vẫn đang thiếu xuồng máy hơi. Ai có hoặc biết chỗ thuê ở quanh khu vực Hà Nội - Thái Nguyên xin hãy liên hệ gấp SĐT: 098x.xxx.xxx! Cảm ơn mọi người!",
        "created_at": "2024-09-10T11:20:00Z",
        "reactions": {"like": 5400, "heart": 3200, "share": 1800},
        "comments": [
            "Đã chia sẻ. Chúc đoàn đi đường bình an.",
            "Mình có một chiếc xuồng hơi ở Mê Linh, bạn qua lấy nhé.",
            "Cho mình số tài khoản để góp chút đỉnh mua áo phao."
        ]
    },
    {
        "id": "x-yagi-107",
        "platform": "x",
        "author": "UNICEF_vietnam",
        "text": "Urgent: Typhoon #Yagi has caused severe damage to schools and infrastructure in Northern Vietnam. UNICEF is working closely with local authorities to ensure children are safe and have access to clean water and sanitation. Support our emergency response! 🇻🇳🌍 #EmergencyAid",
        "created_at": "2024-09-11T09:00:00Z",
        "reactions": {"like": 850, "retweet": 340},
        "comments": [
            "Thank you UNICEF for stepping in.",
            "Where can we donate to this specific cause?"
        ]
    },
    {
        "id": "fb-yagi-108",
        "platform": "facebook",
        "author": "Trần Văn A (Người dân)",
        "text": "CỨU HỘ GẤP SOS!!! Nhà mình ở số 15 đường Hoàng Văn Thụ, TP. Thái Nguyên. Nước đã ngập lên đến nóc tầng 1, nhà có người già bị tai biến và 2 trẻ em nhỏ. Điện thoại sắp hết pin, điện đã mất từ chiều qua. Xin các đội cứu hộ có xuồng qua ứng cứu gia đình với ạ!!! 😭😭😭",
        "created_at": "2024-09-09T22:15:00Z",
        "reactions": {"sad": 1200, "care": 800, "share": 4500},
        "comments": [
            "Đội cứu hộ 116 đang di chuyển vào khu đó, bác giữ máy nhé.",
            "Đã gọi báo tổng đài 113 giúp gia đình.",
            "Cố lên mọi người ơi, nước sẽ rút nhanh thôi."
        ]
    },
    {
        "id": "tiktok-yagi-109",
        "platform": "tiktok",
        "author": "vtv24_official",
        "text": "Toàn cảnh vụ sập nhịp cầu Phong Châu (Phú Thọ) sáng nay do nước lũ dâng cao và chảy xiết. Lực lượng chức năng đang khẩn trương phân luồng giao thông và tìm kiếm cứu nạn. 🎥 Nguồn: Người dân cung cấp. #VTV24 #TinTuc #CauPhongChau",
        "created_at": "2024-09-09T10:30:00Z",
        "reactions": {"like": 250000, "sad": 15000, "share": 45000},
        "comments": [
            "Nhìn sợ quá, mong không có thiệt hại về người.",
            "Nước chảy xiết thế kia thì móng cầu nào chịu nổi.",
            "Ai đi qua đoạn này nhớ vòng đường khác nhé, nguy hiểm lắm."
        ]
    },
    {
        "id": "fb-yagi-110",
        "platform": "facebook",
        "author": "EVN - Tập đoàn Điện lực Việt Nam",
        "text": "THÔNG BÁO TÌNH HÌNH KHÔI PHỤC ĐIỆN: Tính đến 17h chiều nay, EVN đã nỗ lực khôi phục điện cho hơn 80% khách hàng tại Quảng Ninh và Hải Phòng bị ảnh hưởng bởi bão số 3. Các cột điện gãy đổ đang được anh em công nhân thi công xuyên đêm để thay thế. Rất mong sự thông cảm của quý khách hàng tại những khu vực vẫn còn mất điện.",
        "created_at": "2024-09-08T18:00:00Z",
        "reactions": {"like": 8900, "love": 2100},
        "comments": [
            "Cảm ơn các anh thợ điện đã vất vả ngày đêm.",
            "Khu vực Đồ Sơn nhà em đã có điện rồi, mừng rơi nước mắt.",
            "Cố gắng an toàn nhé các anh."
        ]
    },
    {
        "id": "x-yagi-111",
        "platform": "x",
        "author": "diplomacy_vn",
        "text": "We deeply appreciate the international community's solidarity. The US, Japan, and Australia have announced emergency aid packages to help Vietnam overcome the aftermath of Typhoon Yagi. Together we are stronger. 🤝 #Vietnam #Resilience",
        "created_at": "2024-09-12T14:20:00Z",
        "reactions": {"like": 1200, "retweet": 200},
        "comments": [
            "A true example of international cooperation.",
            "Sending prayers from Tokyo."
        ]
    },
    {
        "id": "fb-yagi-112",
        "platform": "facebook",
        "author": "Tôi Người Hải Phòng",
        "text": "Sau bão Yagi, đường phố Hải Phòng tan hoang như vừa trải qua một trận chiến. Rất nhiều tôn lợp, biển quảng cáo bay lả tả. Ai không có việc gì gấp thì ở yên trong nhà để dọn dẹp, nhường đường cho xe môi trường và xe cắt tỉa cây xanh nhé. Cùng nhau vực dậy nào thành phố Hoa Phượng Đỏ ơi! ❤️",
        "created_at": "2024-09-08T09:00:00Z",
        "reactions": {"like": 15000, "sad": 1200, "care": 3000},
        "comments": [
            "Nhà em bay mất nửa cái nóc rồi.",
            "Mong thành phố sớm dọn dẹp xong để mọi người đi làm lại.",
            "Hải Phòng mình mạnh mẽ mà, cố lên!"
        ]
    },
    {
        "id": "tiktok-yagi-113",
        "platform": "tiktok",
        "author": "phuot_luon_cung_toi",
        "text": "Sạt lở đất nghiêm trọng vùi lấp toàn bộ thôn Làng Nủ, Bảo Yên, Lào Cai. Cảnh tượng đau xót không nói nên lời. Hàng trăm bộ đội và chó nghiệp vụ đang bới từng tấc đất tìm người mất tích. Mọi người cùng cầu nguyện phép màu xảy ra nhé... 🙏 #LangNu #BaoYen #SatoLoDat",
        "created_at": "2024-09-10T16:00:00Z",
        "reactions": {"like": 120000, "sad": 35000, "share": 20000},
        "comments": [
            "Trời ơi xem mà rớt nước mắt.",
            "Xóa sổ cả một ngôi làng, thiên tai quá tàn khốc.",
            "Mong các linh hồn được siêu thoát."
        ]
    },
    {
        "id": "fb-yagi-114",
        "platform": "facebook",
        "author": "Báo Tuổi Trẻ",
        "text": "Mực nước sông Hồng tại Hà Nội đã vượt báo động 2, tiệm cận báo động 3. Hàng nghìn hộ dân ven sông khu vực Phúc Tân, Chương Dương Độ đang khẩn trương di dời tài sản trong đêm. Chính quyền địa phương đã chuẩn bị sẵn các điểm tránh trú an toàn cho người dân.",
        "created_at": "2024-09-11T20:30:00Z",
        "reactions": {"like": 6500, "care": 2100},
        "comments": [
            "Hơn 20 năm rồi mới thấy nước sông Hồng lên cao thế này.",
            "Bà con cố gắng di dời sớm, đừng tiếc của mà nguy hiểm tính mạng.",
            "Chúc Hà Nội bình an qua đợt lũ này."
        ]
    },
    {
        "id": "fb-flood-201",
        "platform": "facebook",
        "author": "Người Miền Trung",
        "text": "Mưa lớn liên tục 3 ngày đêm, Hội An lại chìm trong biển nước rồi bà con ạ. Chợ bồng bềnh chạy lụt. Ai về Hội An mùa này chịu khó đi đò thay đi xe đạp nhé 😅. Vẫn lạc quan để sống chung với lũ!",
        "created_at": "2023-10-15T08:00:00Z",
        "reactions": {"like": 4500, "haha": 1200},
        "comments": [
            "Năm nào cũng lụt, riết rồi quen.",
            "Nhìn ảnh thấy thương mà caption làm phì cười.",
            "Hội An mùa nước nổi cũng có nét đẹp riêng."
        ]
    },
    {
        "id": "x-flood-202",
        "platform": "x",
        "author": "climate_monitor_vn",
        "text": "Flash flood warning issued for mountainous areas in Quang Nam and Thua Thien Hue due to intense tropical depression. Local authorities are evacuating high-risk zones. 🌧️⚠️ #VietnamFloods #ClimateAlert",
        "created_at": "2023-11-02T10:00:00Z",
        "reactions": {"like": 320, "retweet": 85},
        "comments": [
            "Stay safe everyone in Central Vietnam.",
            "The frequency of these floods is alarming."
        ]
    },
    {
        "id": "tiktok-yagi-115",
        "platform": "tiktok",
        "author": "cuuho_sos_116",
        "text": "Hành trình đưa một sản phụ từ vùng rốn lũ Tuyên Quang đi viện sinh mổ an toàn bằng ca nô. May mắn cả mẹ và bé đều khỏe mạnh. Xin gửi lời cảm ơn đến trạm y tế xã đã hỗ trợ kịp thời! 🚤👶 #CuuHo #TuyenQuang #Yagi",
        "created_at": "2024-09-12T11:45:00Z",
        "reactions": {"like": 210000, "heart": 45000},
        "comments": [
            "Tuyệt vời quá các chiến sĩ ơi.",
            "Em bé sinh ra trong bão lũ sau này chắc chắn sẽ rất kiên cường.",
            "Chúc mừng gia đình mẹ tròn con vuông."
        ]
    },
    {
        "id": "fb-yagi-116",
        "platform": "facebook",
        "author": "Mặt trận Tổ quốc Việt Nam",
        "text": "HƯỞNG ỨNG LỜI KÊU GỌI ỦNG HỘ ĐỒNG BÀO BỊ THIỆT HẠI DO CƠN BÃO SỐ 3 (YAGI): Ủy ban Trung ương MTTQ Việt Nam đã tiếp nhận hàng trăm tỷ đồng từ các tổ chức, doanh nghiệp và cá nhân. Chúng tôi cam kết sẽ phân bổ minh bạch, kịp thời đến tận tay những hoàn cảnh đang cần giúp đỡ nhất. Danh sách sao kê sẽ được cập nhật liên tục hàng ngày.",
        "created_at": "2024-09-13T14:00:00Z",
        "reactions": {"like": 35000, "love": 12000, "share": 8000},
        "comments": [
            "Mong tiền sớm đến tay bà con để tái thiết cuộc sống.",
            "Đã chuyển khoản ủng hộ, lá lành đùm lá rách.",
            "Hoan nghênh việc sao kê minh bạch hàng ngày."
        ]
    },
    {
        "id": "x-yagi-117",
        "platform": "x",
        "author": "saola_tracker",
        "text": "Typhoon #Yagi has dissipated, but the trailing heavy rains are causing devastating floods in the Red River basin. Water levels at Hanoi station expected to peak tonight. #Vietnam",
        "created_at": "2024-09-11T12:00:00Z",
        "reactions": {"like": 410, "retweet": 150},
        "comments": [
            "Tracking this closely from Bangkok.",
            "The aftermath is always worse than the wind."
        ]
    },
    {
        "id": "tiktok-flood-203",
        "platform": "tiktok",
        "author": "mien_trung_que_choa",
        "text": "Đàn heo nhà bà cụ bị nước cuốn trôi hết cả rồi, bà khóc ngất bên hiên nhà nhìn tài sản cả năm trời đổ sông đổ bể. Mưa lũ miền Trung sao khắc nghiệt quá! 😢 #MienTrung #BaoLu",
        "created_at": "2022-10-18T09:30:00Z",
        "reactions": {"like": 78000, "sad": 12000},
        "comments": [
            "Tội ngoại quá, cả gia tài của người nông dân.",
            "Ai có địa chỉ cụ không, cho mình xin gửi cụ chút tiền mua gạo.",
            "Thiên nhiên tàn nhẫn thật sự."
        ]
    },
    {
        "id": "fb-yagi-118",
        "platform": "facebook",
        "author": "Sở GD&ĐT Hà Nội",
        "text": "THÔNG BÁO KHẨN: Để đảm bảo an toàn cho học sinh trước diễn biến phức tạp của mưa lũ sau bão số 3, Sở GD&ĐT Hà Nội chỉ đạo các trường học thuộc 10 quận/huyện vùng ven sông, trũng thấp tạm cho học sinh nghỉ học trực tiếp từ ngày 12/09/2024 cho đến khi có thông báo mới. Các trường chủ động chuyển sang hình thức học trực tuyến.",
        "created_at": "2024-09-11T16:00:00Z",
        "reactions": {"like": 14000, "share": 9500},
        "comments": [
            "Quyết định hợp lý, an toàn là trên hết.",
            "Nhà em ngập không có điện wifi để học online luôn ạ.",
            "Các phụ huynh chú ý quản lý con em, tuyệt đối không cho ra vũng nước chơi."
        ]
    }
] (#BaoSo3) đang tiến sát Vịnh Bắc Bộ với sức gió giật cấp 17. Dự báo từ ngày 7/9/2024, khu vực Hải Phòng, Quảng Ninh sẽ chịu ảnh hưởng trực tiếp. Đề nghị bà con neo đậu tàu thuyền ngay lập tức!!! Xem thêm tại https://kttv.gov.vn",
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
