package com.disaster.datacollection.platform;

public class PlatformSetting {
    public Platform getPlatform(String platformType) {
        switch (platformType.toLowerCase()) {
            case "facebook": return new Facebook();
            case "twitter":  return new Twitter();
            default: throw new IllegalArgumentException("Unknown platform: " + platformType);
        }
    }
}
