package com.project.gui;

import com.project.datacollection.model.SocialMediaPost;
import com.project.logistics.entities.SupportCenter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MockDataProvider {

    public static List<SocialMediaPost> getMockPosts() {
        List<SocialMediaPost> tempPosts = new ArrayList<>();
        
        SocialMediaPost p1 = new SocialMediaPost(
            "live-fb-1-yagi",
            "Mưa lớn kéo dài gây ngập lụt nghiêm trọng tại huyện Hải Lăng, Quảng Trị. Hơn 50 hộ dân bị cô lập hoàn toàn, nước dâng cao đến mái nhà. Hiện tại bà con đang thiếu lương thực, nước uống sạch trầm trọng. Cần hỗ trợ khẩn cấp xuồng cứu hộ và mì tôm, nước suối đóng chai.",
            "Thông tin Phòng chống thiên tai",
            java.time.LocalDateTime.now(),
            "Facebook"
        );
        p1.getReactions().put("sad", 150);
        p1.getReactions().put("care", 80);
        p1.getReactions().put("like", 10);
        p1.getReactions().put("angry", 5);
        p1.getComments().add("Nước dâng nhanh quá, mong đoàn cứu trợ đến sớm!");
        p1.getComments().add("Hải Lăng đang ngập sâu lắm, nhà em ngập nửa người rồi.");
        p1.getComments().add("Cần nước ngọt gấp ạ!");
        tempPosts.add(p1);

        SocialMediaPost p2 = new SocialMediaPost(
            "live-fb-2-catba",
            "Tình hình tại đảo Cát Bà, Hải Phòng đang rất nguy cấp sau khi bão đổ bộ. Nhiều ngôi nhà bị tốc mái hoàn toàn, hệ thống điện nước bị cắt. Trạm y tế địa phương đang quá tải và thiếu hụt bông băng, thuốc sát trùng, thuốc kháng sinh cơ bản. Mong các đoàn cứu trợ tiếp tế y tế gấp!",
            "Hội Chữ thập đỏ Cát Hải",
            java.time.LocalDateTime.now(),
            "Facebook"
        );
        p2.getReactions().put("sad", 210);
        p2.getReactions().put("care", 95);
        p2.getReactions().put("like", 15);
        p2.getReactions().put("angry", 12);
        p2.getComments().add("Mất điện từ hôm qua tới giờ chưa có lại.");
        p2.getComments().add("Trạm y tế Cát Hải đang quá tải trầm trọng.");
        p2.getComments().add("Mong mọi người bình an.");
        tempPosts.add(p2);

        SocialMediaPost p3 = new SocialMediaPost(
            "live-fb-3-lucyen",
            "Sạt lở đất nghiêm trọng tại Lục Yên, Yên Bái làm sập 3 ngôi nhà, giao thông hoàn toàn bị chia cắt. Có người bị thương đang chờ được đưa đi cấp cứu nhưng xe cứu thương không vào được. Cần lực lượng cứu nạn cứu hộ và rào chắn giao thông tiếp cận khẩn cấp.",
            "Yên Bái 24h",
            java.time.LocalDateTime.now(),
            "Facebook"
        );
        p3.getReactions().put("sad", 340);
        p3.getReactions().put("care", 120);
        p3.getReactions().put("like", 8);
        p3.getReactions().put("angry", 45);
        p3.getComments().add("Thương quá, sạt lở đất đá đè sập cả nhà rồi.");
        p3.getComments().add("Đường Lục Yên sạt nặng, xe cứu trợ chưa vào được đâu.");
        p3.getComments().add("Cầu mong không có thêm thiệt hại về người.");
        tempPosts.add(p3);

        SocialMediaPost p4 = new SocialMediaPost(
            "live-fb-4-bacha",
            "Bản Phố, Bắc Hà, Lào Cai bị cô lập do lũ quét sạch cầu tràn. Bà con ở đây tạm thời an toàn nhưng lương thực dự trữ chỉ còn dùng được hết ngày mai. Cần tiếp tế gạo, muối ăn và bạt dựng lều tạm vì nhiều nhà bị hư hại nặng.",
            "Bắc Hà News",
            java.time.LocalDateTime.now(),
            "Facebook"
        );
        p4.getReactions().put("sad", 180);
        p4.getReactions().put("care", 70);
        p4.getReactions().put("like", 12);
        p4.getReactions().put("angry", 2);
        p4.getComments().add("Bản Phố cầu trôi rồi cô lập hoàn toàn.");
        p4.getComments().add("Bà con Bắc Hà rất cần bạt và mì tôm gạo ăn tạm.");
        tempPosts.add(p4);

        SocialMediaPost p5 = new SocialMediaPost(
            "live-fb-5-thank",
            "Cảm ơn các nhà hảo tâm và chính quyền địa phương đã kịp thời vận chuyển 200 thùng mì tôm và nước sạch đến cho bà con vùng lũ lụt Thường Xuân, Thanh Hóa hôm nay. Tình hình đang dần ổn định trở lại.",
            "Người Dân Xứ Thanh",
            java.time.LocalDateTime.now(),
            "Facebook"
        );
        p5.getReactions().put("like", 450);
        p5.getReactions().put("care", 280);
        p5.getReactions().put("sad", 5);
        p5.getComments().add("Tuyệt vời quá, cảm ơn đoàn cứu trợ.");
        p5.getComments().add("Ấm lòng tình đồng bào miền Trung lúc hoạn nạn.");
        tempPosts.add(p5);
        
        return tempPosts;
    }

    public static List<SupportCenter> getMockSupportCenters() {
        List<SupportCenter> supportCenterList = new ArrayList<>();
        
        SupportCenter sc1 = new SupportCenter(21.028511, 105.804817, "Hà Nội (Trung tâm Logistics Hà Nội)", 5);
        sc1.setCurrentSupplies(Arrays.asList("food", "water", "medical", "rescue"));

        SupportCenter sc2 = new SupportCenter(16.047079, 108.206230, "Đà Nẵng (Kho dự trữ Đà Nẵng)", 8);
        sc2.setCurrentSupplies(Arrays.asList("food", "water", "medical", "shelter", "rescue"));

        SupportCenter sc3 = new SupportCenter(18.673244, 105.692440, "Vinh (Kho Vinh - Nghệ An)", 3);
        sc3.setCurrentSupplies(Arrays.asList("food", "water", "medical"));

        supportCenterList.addAll(Arrays.asList(sc1, sc2, sc3));
        return supportCenterList;
    }
}
