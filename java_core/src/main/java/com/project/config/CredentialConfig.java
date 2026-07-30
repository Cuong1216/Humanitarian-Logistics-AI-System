package main.java.com.project.config;

public class CredentialConfig {

    public static String getFacebookEmail() {
        return getRequiredConfig("FB_EMAIL", "Missing mandatory credential: FB_EMAIL (Facebook Email)");
    }

    public static String getFacebookPassword() {
        return getRequiredConfig("FB_PASSWORD", "Missing mandatory credential: FB_PASSWORD (Facebook Password)");
    }

    public static String getXUsername() {
        return getRequiredConfig("X_USERNAME", "Missing mandatory credential: X_USERNAME (X/Twitter Username)");
    }

    public static String getXPassword() {
        return getRequiredConfig("X_PASSWORD", "Missing mandatory credential: X_PASSWORD (X/Twitter Password)");
    }

    private static String getRequiredConfig(String key, String errorMessage) {
        // Fallback: System environment variable first, then System property
        String value = System.getenv(key);
        if (value == null || value.trim().isEmpty()) {
            value = System.getProperty(key);
        }
        
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalStateException(errorMessage + ". Please set it as an environment variable or via -D flag.");
        }
        
        return value.trim();
    }
}
