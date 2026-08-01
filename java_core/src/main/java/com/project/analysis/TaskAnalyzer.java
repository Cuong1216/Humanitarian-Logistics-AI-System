package com.project.analysis;

import com.project.ai_client.IAiClient;
import com.project.datacollection.model.SocialMediaPost;
import java.util.List;

public interface TaskAnalyzer {

    /**
     * Thực hiện phân tích tác vụ và trả về kết quả AnalysisResult.
     * <p>
     * <b>CẢNH BÁO:</b> Lập trình viên bắt buộc phải override (ghi đè) method này 
     * khi implement interface này ở lớp con. Việc gọi trực tiếp method mặc định 
     * sẽ ném ra ngoại lệ UnsupportedOperationException.
     * </p>
     * 
     * @param posts Danh sách các bài đăng trên mạng xã hội cần phân tích
     * @param aiClient Client gọi tới AI Engine
     * @return AnalysisResult Kết quả sau khi phân tích
     * @throws UnsupportedOperationException nếu lớp con không cài đặt method này
     */
    default java.util.concurrent.CompletableFuture<AnalysisResult> analyze(List<SocialMediaPost> posts, IAiClient aiClient) {
        throw new UnsupportedOperationException("Method analyze() chưa được cài đặt ở lớp con");
    }
}
