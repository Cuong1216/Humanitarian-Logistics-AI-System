package com.project.datacollection.platform;

import com.project.datacollection.model.SocialMediaPost;
import main.java.com.project.config.CredentialConfig;
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
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class XScraper implements Platform {

    @Override
    public List<SocialMediaPost> scrapePosts(String keyword) {
        List<SocialMediaPost> posts = new ArrayList<>();
        
        // Cài đặt ChromeDriver
        WebDriverManager.chromedriver().setup();
        
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--disable-notifications");
        // options.addArguments("--headless");
        
        WebDriver driver = new ChromeDriver(options);
        
        try {
            // Đăng nhập X (Twitter) trước
            loginToX(driver, CredentialConfig.getXUsername(), CredentialConfig.getXPassword());

            // Sau khi đăng nhập, mở trang tìm kiếm
            String searchUrl = "https://x.com/search?q=" + keyword.replace(" ", "%20") + "&src=typed_query";
            driver.get(searchUrl);

            // Đợi một khoảng thời gian để trang tải xong
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
            try {
                wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("article[data-testid='tweet']")));
            } catch (Exception e) {
                System.out.println("Có thể trang tải chậm hoặc không có bài viết.");
            }
            
            // Cuộn trang để lấy thêm bài viết
            JavascriptExecutor js = (JavascriptExecutor) driver;
            for (int i = 0; i < 3; i++) {
                js.executeScript("window.scrollTo(0, document.body.scrollHeight);");
                Thread.sleep(2000); 
            }

            // Tìm các element chứa bài viết (Tweet)
            // LƯU Ý: Selector của Twitter/X thay đổi rất thường xuyên. Dưới đây là selector thường dùng (thẻ article).
            List<WebElement> tweetElements = driver.findElements(By.cssSelector("article[data-testid='tweet']"));

            for (WebElement element : tweetElements) {
                try {
                    String content = "";
                    String author = "Unknown";
                    
                    // Lấy nội dung tweet
                    List<WebElement> contentElements = element.findElements(By.cssSelector("div[data-testid='tweetText']"));
                    if (!contentElements.isEmpty()) {
                        content = contentElements.get(0).getText();
                    }

                    // Lấy tên người dùng / tác giả
                    List<WebElement> authorElements = element.findElements(By.cssSelector("div[data-testid='User-Name']"));
                    if (!authorElements.isEmpty()) {
                        // Tách tên hiển thị hoặc username
                        String fullAuthorText = authorElements.get(0).getText();
                        String[] parts = fullAuthorText.split("\n");
                        if (parts.length > 0) {
                            author = parts[0]; 
                        }
                    }

                    if (!content.isEmpty()) {
                        SocialMediaPost post = new SocialMediaPost(
                            UUID.randomUUID().toString(),
                            content,
                            author,
                            LocalDateTime.now(), 
                            getPlatformName()
                        );
                        posts.add(post);
                    }
                } catch (Exception e) {
                    System.err.println("Lỗi khi parse 1 tweet: " + e.getMessage());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            driver.quit();
        }

        return posts;
    }

    @Override
    public String getPlatformName() {
        return "X";
    }

    private void loginToX(WebDriver driver, String username, String password) {
        try {
            driver.get("https://x.com/i/flow/login");
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(15));
            
            // Bước 1: Điền username
            WebElement usernameInput = wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("input[autocomplete='username']")));
            usernameInput.sendKeys(username);
            
            // Bấm nút Next
            List<WebElement> buttons = driver.findElements(By.cssSelector("button[role='button']"));
            WebElement nextButton = null;
            for (WebElement btn : buttons) {
                String text = btn.getText();
                if (text.equalsIgnoreCase("Next") || text.equalsIgnoreCase("Tiếp theo")) {
                    nextButton = btn;
                    break;
                }
            }
            if (nextButton != null) {
                nextButton.click();
            } else {
                throw new RuntimeException("Không tìm thấy nút Next");
            }
            
            // Bước 2: Điền password
            WebElement passwordInput = wait.until(ExpectedConditions.presenceOfElementLocated(By.name("password")));
            passwordInput.sendKeys(password);
            
            // Bấm nút Log in
            WebElement loginButton = driver.findElements(By.cssSelector("button[data-testid='LoginForm_Login_Button']"))
                .stream().findFirst().orElseThrow(() -> new RuntimeException("Không tìm thấy nút Login"));
            loginButton.click();
            
            // Đợi trang chủ tải sau đăng nhập
            Thread.sleep(5000); 
        } catch (Exception e) {
            System.err.println("Lỗi đăng nhập X: " + e.getMessage());
        }
    }
}
