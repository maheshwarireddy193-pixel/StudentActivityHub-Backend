package com.studentactivityhub.controller;

import com.studentactivityhub.entity.User;
import com.studentactivityhub.service.ProfileService;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/profile")
public class ProfileController {

    private final ProfileService profileService;

    public ProfileController(ProfileService profileService) {
        this.profileService = profileService;
    }

    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> getMyProfile() {

        User user = profileService.getMyProfile();

        return ResponseEntity.ok(toProfileResponse(user));
    }

    @PutMapping("/me")
    public ResponseEntity<Map<String, Object>> updateMyProfile(
            @RequestBody Map<String, String> request) {

        String githubUrl = request.get("githubUrl");
        String linkedinUrl = request.get("linkedinUrl");
        String leetcodeUrl = request.get("leetcodeUrl");
        String portfolioUrl = request.get("portfolioUrl");

        validateUrl(githubUrl, "githubUrl", "github.com");
        validateUrl(linkedinUrl, "linkedinUrl", "linkedin.com");
        validateUrl(leetcodeUrl, "leetcodeUrl", "leetcode.com");

        User user = profileService.updateMyProfile(
                githubUrl,
                linkedinUrl,
                leetcodeUrl,
                portfolioUrl
        );

        return ResponseEntity.ok(toProfileResponse(user));
    }

    private Map<String, Object> toProfileResponse(User user) {

        Map<String, Object> response = new LinkedHashMap<>();

        response.put("id", user.getId());
        response.put("name", user.getName());
        response.put("email", user.getEmail());
        response.put("role", user.getRole());
        response.put("rollNumber", user.getRollNumber());

        response.put("githubUrl", user.getGithubUrl());
        response.put("linkedinUrl", user.getLinkedinUrl());
        response.put("leetcodeUrl", user.getLeetcodeUrl());
        response.put("portfolioUrl", user.getPortfolioUrl());

        return response;
    }

    private void validateUrl(
            String value,
            String field,
            String allowedDomain) {

        if (value == null || value.isBlank()) {
            return;
        }

        String url = value.trim();

        if (!url.matches("(?i)^https?://.+")) {
            throw new IllegalArgumentException(
                    field + " must start with https:// or http://");
        }

        try {
            java.net.URI uri = java.net.URI.create(url);

            String host = uri.getHost();

            if (host == null ||
                    !(host.equalsIgnoreCase(allowedDomain) ||
                      host.toLowerCase().endsWith("." + allowedDomain))) {

                throw new IllegalArgumentException(
                        "Please provide a valid " + allowedDomain + " URL");
            }
        } catch (IllegalArgumentException exception) {
            throw exception;
        }
    }
}