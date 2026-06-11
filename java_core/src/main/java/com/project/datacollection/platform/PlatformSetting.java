package main.java.com.project.datacollection.platform;

public class PlatformSetting {
    public Platform getPlatform(String platformType) {
        switch (platformType.toLowerCase()) {
            case "facebook": return new FacebookScraper();
            case "twitter":  return new XScraper();
            default: throw new IllegalArgumentException("Unknown platform: " + platformType);
        }
    }
}
