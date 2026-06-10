package com.project.datacollection;

import com.project.datacollection.platform.FacebookScraper;
import com.project.datacollection.platform.Platform;
import com.project.datacollection.platform.XScraper;

public class PlatformFactory {
    private PlatformFactory() {
        // Prevent instantiation
    }

    public static Platform create(String name) {
        if (name == null) {
            return null;
        }

        switch (name.trim().toLowerCase()) {
            case "x":
            case "twitter":
                return new XScraper();
            case "facebook":
                return new FacebookScraper();
            default:
                throw new IllegalArgumentException("Unknown platform: " + name);
        }
    }
}
