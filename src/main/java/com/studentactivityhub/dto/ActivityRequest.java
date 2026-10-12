package com.studentactivityhub.dto;

public class ActivityRequest {

    private String title;
    private String description;
    private String category;
    private String certificateUrl;

    public ActivityRequest() {
    }

    public ActivityRequest(String title, String description,
                           String category, String certificateUrl) {
        this.title = title;
        this.description = description;
        this.category = category;
        this.certificateUrl = certificateUrl;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getCertificateUrl() {
        return certificateUrl;
    }

    public void setCertificateUrl(String certificateUrl) {
        this.certificateUrl = certificateUrl;
    }
}