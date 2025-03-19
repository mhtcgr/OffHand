package com.example.offhand.model;

public class MainApiResponse {
    public String code;
    public String message;
    public MainData data;

    public static class MainData {
        public LatestTrainingSummary latestTrainingSummary;
        public RecentDaysTrainingSummary recentDaysTrainingSummary;
    }
}
