package test;
 
import com.project.ai_client.FastApiRestClient;
import com.project.ai_client.dto.AnalyzeReq;
import com.project.ai_client.dto.AnalyzeRes;
import com.project.datacollection.model.SocialMediaPost;
import com.project.datacollection.platform.FacebookScraper;
 
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
 
public class CrawlerTest {
 
    public static void main(String[] args) {
        System.out.println("=== BẮT ĐẦU TEST CRAWLER VÀ PHÂN TÍCH QUA PYTHON AI ENGINE ===");
 
        try {
            // 1. Chạy FacebookScraper bằng Java để lấy dữ liệu bão lụt
            System.out.println("\n[Java] Đang chạy FacebookScraper...");
            FacebookScraper scraper = new FacebookScraper();
            List<SocialMediaPost> posts = scraper.fetchPost("bão Yagi", new Date(), new Date());
            
            System.out.println("\n[Java] Lấy thành công " + posts.size() + " bài đăng:");
            for (SocialMediaPost post : posts) {
                System.out.println(" - [" + post.getId() + "] Tác giả: " + post.getAuthor());
                System.out.println("   Nội dung: " + post.getContent());
                System.out.println("   Thích: " + post.getLikeCount() + " | Chia sẻ: " + post.getShareCount());
                System.out.println("   Bình luận (" + post.getComments().size() + "):");
                for (String comment : post.getComments()) {
                    System.out.println("     * " + comment);
                }
            }
 
            // 2. Khởi tạo REST Client trỏ đến server FastAPI local
            FastApiRestClient aiClient = new FastApiRestClient("http://127.0.0.1:8000");
 
            System.out.println("\n[Java] Đang gửi các bài đăng sang Python qua endpoint /analyze...");
            for (SocialMediaPost post : posts) {
                // Tạo request cho từng bài đăng
                AnalyzeReq.PostData postData = new AnalyzeReq.PostData(
                        post.getId(),
                        post.getPlatform().toLowerCase(),
                        post.getAuthor(),
                        post.getContent(),
                        "bão Yagi",
                        "",
                        post.getReactions(),
                        post.getComments(),
                        post.getShareCount()
                );
                
                AnalyzeReq req = new AnalyzeReq(postData);
                AnalyzeRes res = aiClient.executeTask("/analyze", req, AnalyzeRes.class);
 
                System.out.println("\n=== KẾT QUẢ PHÂN TÍCH BÀI ĐĂNG [" + res.getPostId() + "] ===");
                System.out.println("Cảm xúc chủ đạo : " + res.getDominantEmotion());
                System.out.println("Điểm tiêu cực   : " + res.getNegativeScore());
                System.out.println("Tóm tắt AI      : " + res.getSummary());
                
                AnalyzeRes.HumanitarianSignal signal = res.getHumanitarianSignal();
                if (signal != null) {
                    System.out.println("Khẩn cấp?       : " + signal.isEmergency());
                    System.out.println("Mức độ nguy cấp : " + signal.getUrgency());
                    System.out.println("Nhu cầu hỗ trợ  : " + signal.getCategories());
                    System.out.println("Địa điểm cứu trợ: " + signal.getLocations());
                    System.out.println("Hành động gợi ý : " + signal.getRecommendedAction());
                }
            }
 
            System.out.println("\n=== THỬ NGHIỆM THÀNH CÔNG HOÀN TOÀN ===");
            
        } catch (Exception e) {
            System.err.println("\n[LỖI] Thử nghiệm thất bại:");
            e.printStackTrace();
        }
    }
}
