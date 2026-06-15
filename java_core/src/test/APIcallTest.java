package test; 

import com.project.ai_client.FastApiRestClient;
import com.project.ai_client.dto.AnalyzeReq;
import com.project.ai_client.dto.AnalyzeRes;
import javafx.application.Application;
import javafx.stage.Stage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class APIcallTest extends Application {

    @Override
    public void start(Stage primaryStage) {
        System.out.println("=== BẮT ĐẦU KIỂM TRA KẾT NỐI FASTAPI CLIENT ===");

        try {
            // 1. Khởi tạo REST Client trỏ đến server FastAPI local
            FastApiRestClient aiClient = new FastApiRestClient("http://127.0.0.1:8000");

            // 2. Dựng dữ liệu Mock khớp với cấu trúc JSON của Python
            Map<String, Integer> mockReactions = new HashMap<>();
            mockReactions.put("sad", 120);
            mockReactions.put("angry", 32);
            mockReactions.put("care", 75);
            mockReactions.put("like", 20);

            List<String> mockComments = new ArrayList<>();
            mockComments.add("Cứu với, đường vào thôn bị nước cuốn hỏng rồi.");
            mockComments.add("Cần xe cứu trợ chở nước sạch và thuốc.");
            mockComments.add("Khoảng 300 người đang cần hỗ trợ.");

            // Khởi tạo Object dữ liệu bài đăng lõi
            AnalyzeReq.PostData postData = new AnalyzeReq.PostData(
                    "post-yagi-001",
                    "facebook",
                    "nguoi_dan_dia_phuong",
                    "Thôn A xã Bình Minh bị ngập nặng sau bão Yagi. Nhiều hộ dân đang mắc kẹt, thiếu lương thực, nước sạch và thuốc men khẩn cấp.",
                    "bão Yagi",
                    "thôn A xã Bình Minh",
                    mockReactions,
                    mockComments,
                    88
            );

            // Gói vào DTO Request tổng
            AnalyzeReq testRequest = new AnalyzeReq(postData);

            System.out.println("[Java] Đang gửi dữ liệu mẫu sang Python qua endpoint /analyze...");

            // 3. Thực thi gọi API
            AnalyzeRes testResponse = aiClient.executeTask("/analyze", testRequest, AnalyzeRes.class);

            // 4. In kết quả nhận được từ Python ra Terminal để kiểm tra
            System.out.println("\n=== KẾT QUẢ TRẢ VỀ TỪ PYTHON AI ENGINE ===");
            System.out.println("Mã bài đăng (Post ID) : " + testResponse.getPostId());
            System.out.println("Từ khóa (Keyword)    : " + testResponse.getKeyword());
            System.out.println("Cảm xúc chủ đạo      : " + testResponse.getDominantEmotion());
            System.out.println("Điểm tiêu cực        : " + testResponse.getNegativeScore());
            System.out.println("Nguồn xử lý (Source) : " + testResponse.getSource());
            System.out.println("Tóm tắt từ AI        : " + testResponse.getSummary());

            // In chi tiết Object lồng bên trong (Humanitarian Signal)
            AnalyzeRes.HumanitarianSignal signal = testResponse.getHumanitarianSignal();
            if (signal != null) {
                System.out.println("\n--- TÍN HIỆU NHÂN ĐẠO CHI TIẾT ---");
                System.out.println("Tình trạng khẩn cấp  : " + signal.isEmergency());
                System.out.println("Mức độ khẩn cấp      : " + signal.getUrgency());
                System.out.println("Danh mục cần hỗ trợ  : " + signal.getCategories()); // In ra mảng [food, water...]
                System.out.println("Vị trí trích xuất    : " + signal.getLocations());
                System.out.println("Ước tính số người ảnh hưởng: " + signal.getAffectedPeopleEstimate());
                System.out.println("Hành động khuyến nghị: " + signal.getRecommendedAction());
            }
            System.out.println("\n=== KIỂM TRA HOÀN TẤT: ĐẦU NỐI KẾT NỐI THÀNH CÔNG ===");

        } catch (Exception e) {
            System.err.println("\n[LỖI THẤT BẠI] Không thể kết nối hoặc parse dữ liệu:");
            System.err.println("Chi tiết lỗi: " + e.getMessage());
            e.printStackTrace();
        }

        // Tắt ứng dụng JavaFX sau khi test xong trên Terminal
        System.exit(0);
    }

    public static void main(String[] args) {
        launch(args);
    }
}
