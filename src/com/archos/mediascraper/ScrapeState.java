package com.archos.mediascraper;

public class ScrapeState {
    private static volatile boolean sIsManualScraping = false;

    public static boolean isManualScrapingRunning() {
        return sIsManualScraping;
    }

    public static void setManualScrapingRunning(boolean isRunning) {
        sIsManualScraping = isRunning;
    }
}