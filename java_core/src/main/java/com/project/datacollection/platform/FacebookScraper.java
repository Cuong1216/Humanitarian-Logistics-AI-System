package main.java.com.project.datacollection.platform;

import main.java.com.project.datacollection.model.SocialMediaPost;
import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

public class FacebookScraper implements Platform {

    @Override
    public List<SocialMediaPost> fetchPost(String keyword, Date startDate, Date endDate) {
        List<SocialMediaPost> posts = new ArrayList<>();
        
        // Thiết lập WebDriverManager để tự động tải ChromeDriver
        WebDriverManager.chromedriver().setup();
        
        // Cấu hình ChromeOptions
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--disable-notifications"); // Tắt thông báo
        // options.addArguments("--headless"); // Bỏ comment nếu muốn chạy ngầm không hiện trình duyệt
        
        WebDriver driver = new ChromeDriver(options);
        
        try {
            // Mở trang Facebook và đăng nhập trước khi tìm kiếm
            loginToFacebook(driver, "your_email@example.com", "your_password");

            // Sau khi đăng nhập, mở trang tìm kiếm
            String searchUrl = "https://www.facebook.com/search/posts?q=" + keyword.replace(" ", "%20");
            driver.get(searchUrl);

            // Đợi bài viết tải xong
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
            try {
                wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("div[role='article']")));
            } catch (Exception e) {
                System.out.println("Có thể trang tải chậm hoặc không có bài viết.");
            }
            
            // Cuộn trang để tải thêm dữ liệu (scroll down)
            JavascriptExecutor js = (JavascriptExecutor) driver;
            for (int i = 0; i < 3; i++) {
                js.executeScript("window.scrollTo(0, document.body.scrollHeight);");
                Thread.sleep(2000); // Đợi 2 giây cho mỗi lần cuộn
            }

            // Lấy các element chứa bài viết (Selector có thể thay đổi do UI Facebook cập nhật liên tục)
            // LƯU Ý: Các class "x1yztwqm" là class tự động sinh (obfuscated) của Facebook, 
            // bạn sẽ cần cập nhật lại selector này bằng cách Inspect Element thực tế trên trình duyệt.
            List<WebElement> postElements = driver.findElements(By.cssSelector("div[role='article']"));

            for (WebElement element : postElements) {
                try {
                    String content = "";
                    String author = "Unknown";
                    
                    LocalDateTime postTime = LocalDateTime.now();

                    // Tìm thẻ chứa timestamp để parse
                    List<WebElement> timeElements = element.findElements(By.cssSelector("span > span > a[href*='/posts/'], span > span > a[href*='/videos/']"));
                    if (!timeElements.isEmpty()) {
                        postTime = parseFacebookTimestamp(timeElements.get(0).getText());
                    }

                    LocalDateTime startLdt = startDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
                    LocalDateTime endLdt = endDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();

                    if (postTime == null || postTime.isBefore(startLdt) || postTime.isAfter(endLdt)) {
                        continue; // Bỏ qua nếu nằm ngoài khoảng thời gian
                    }

                    // Lấy nội dung bài viết
                    List<WebElement> contentElements = element.findElements(By.cssSelector("div[data-ad-comet-preview='message']"));
                    if (!contentElements.isEmpty()) {
                        content = contentElements.get(0).getText();
                    }

                    // Lấy tên tác giả
                    List<WebElement> authorElements = element.findElements(By.cssSelector("strong"));
                    if (!authorElements.isEmpty()) {
                        author = authorElements.get(0).getText();
                    }

                    if (!content.isEmpty()) {
                        SocialMediaPost post = new SocialMediaPost(
                            UUID.randomUUID().toString(),
                            content,
                            author,
                            postTime,
                            getPlatformName()
                        );
                        posts.add(post);
                    }
                } catch (Exception e) {
                    System.err.println("Lỗi khi parse 1 bài viết: " + e.getMessage());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            // Đóng trình duyệt sau khi cào xong
            driver.quit();
        }

        return posts;
    }

    @Override
    public String getPlatformName() {
        return "Facebook";
    }

    private void loginToFacebook(WebDriver driver, String email, String password) {
        try {
            driver.get("https://www.facebook.com/");
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
            
            // Tìm và điền email
            WebElement emailInput = wait.until(ExpectedConditions.presenceOfElementLocated(By.id("email")));
            emailInput.sendKeys(email);
            
            // Tìm và điền mật khẩu
            WebElement passwordInput = driver.findElement(By.id("pass"));
            passwordInput.sendKeys(password);
            
            // Bấm nút đăng nhập
            WebElement loginButton = driver.findElement(By.name("login"));
            loginButton.click();
            
            // Đợi chuyển hướng sau đăng nhập
            Thread.sleep(5000); 
        } catch (Exception e) {
            System.err.println("Lỗi đăng nhập Facebook: " + e.getMessage());
        }
    }

    private LocalDateTime parseFacebookTimestamp(String timeText) {
        if (timeText == null || timeText.trim().isEmpty()) return LocalDateTime.now();
        String lower = timeText.toLowerCase().trim();
        LocalDateTime now = LocalDateTime.now();
        try {
            if (lower.contains("vừa xong") || lower.contains("just now")) {
                return now;
            } else if (lower.matches(".*\\d+\\s*(phút|m).*")) {
                long min = Long.parseLong(lower.replaceAll("\\D", ""));
                return now.minusMinutes(min);
            } else if (lower.matches(".*\\d+\\s*(giờ|h).*")) {
                long h = Long.parseLong(lower.replaceAll("\\D", ""));
                return now.minusHours(h);
            } else if (lower.matches(".*\\d+\\s*(ngày|d).*")) {
                long d = Long.parseLong(lower.replaceAll("\\D", ""));
                return now.minusDays(d);
            } else if (lower.contains("hôm qua") || lower.contains("yesterday")) {
                return now.minusDays(1);
            }
        } catch (Exception e) {
            System.err.println("Lỗi parse thời gian FB: " + timeText);
        }
        return now;
