package com.project.gui.config;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URL;

@Component
public class StageReadyEventListener implements ApplicationListener<StageReadyEvent> {

    private final ApplicationContext applicationContext;
    private final String applicationTitle;
    private final Resource mainViewResource;

    public StageReadyEventListener(
            ApplicationContext applicationContext,
            @Value("${spring.application.name:Hệ thống Phân tích Mạng xã hội & Điều phối Cứu trợ Thiên tai}") String applicationTitle,
            @Value("classpath:/com/project/gui/resources/fxml/MainView.fxml") Resource mainViewResource) {
        this.applicationContext = applicationContext;
        this.applicationTitle = applicationTitle;
        this.mainViewResource = mainViewResource;
    }

    @Override
    public void onApplicationEvent(StageReadyEvent event) {
        try {
            Stage stage = event.getStage();
            URL url = mainViewResource.getURL();
            FXMLLoader fxmlLoader = new FXMLLoader(url);
            
            // Cấu hình Controller Factory để Spring khởi tạo các Controllers
            fxmlLoader.setControllerFactory(applicationContext::getBean);

            Parent root = fxmlLoader.load();
            Scene scene = new Scene(root, 1280, 800);
            scene.getStylesheets().add(
                getClass().getResource("/com/project/gui/resources/css/style.css").toExternalForm()
            );

            stage.setTitle(applicationTitle);
            stage.setScene(scene);
            stage.setMinWidth(1000);
            stage.setMinHeight(700);
            stage.show();
        } catch (IOException e) {
            throw new RuntimeException("Lỗi khởi tạo giao diện JavaFX", e);
        }
    }
}
