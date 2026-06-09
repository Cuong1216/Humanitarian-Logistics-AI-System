package com.project.datacollection;

public class PlatformFactory {
    private PlatformFactory() {
        // Prevent instantiation
    }

    public static Platform create(String name) {
        if (name == null) {
            return new UnknownPlatform();
        }

        switch (name.trim().toLowerCase()) {
            case "twitter":
                return new TwitterPlatform();
            case "facebook":
                return new FacebookPlatform();
            case "instagram":
                return new InstagramPlatform();
            case "linkedin":
                return new LinkedInPlatform();
            default:
                return new UnknownPlatform();
        }
    }

    public interface Platform {
        String getName();
        String getApiEndpoint();
    }

    private static class TwitterPlatform implements Platform {
        @Override
        public String getName() {
            return "Twitter";
        }

        @Override
        public String getApiEndpoint() {
            return "https://api.twitter.com";
        }
    }

    private static class FacebookPlatform implements Platform {
        @Override
        public String getName() {
            return "Facebook";
        }

        @Override
        public String getApiEndpoint() {
            return "https://graph.facebook.com";
        }
    }

    private static class InstagramPlatform implements Platform {
        @Override
        public String getName() {
            return "Instagram";
        }

        @Override
        public String getApiEndpoint() {
            return "https://graph.instagram.com";
        }
    }

    private static class LinkedInPlatform implements Platform {
        @Override
        public String getName() {
            return "LinkedIn";
        }

        @Override
        public String getApiEndpoint() {
            return "https://api.linkedin.com";
        }
    }

    private static class UnknownPlatform implements Platform {
        @Override
        public String getName() {
            return "Unknown";
        }

        @Override
        public String getApiEndpoint() {
            return "";
        }
    }
}
