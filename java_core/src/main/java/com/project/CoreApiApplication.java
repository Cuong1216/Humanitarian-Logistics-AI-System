package com.project;

import com.project.gui.config.JavaFxApplication;
import javafx.application.Application;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class CoreApiApplication {
    public static void main(String[] args) {
        System.setProperty("jdk.gtk.version", "2");
        Application.launch(JavaFxApplication.class, args);
    }
}
