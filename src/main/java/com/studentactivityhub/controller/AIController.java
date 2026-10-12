package com.studentactivityhub.controller;

import com.studentactivityhub.dto.AIResumeRequest;
import com.studentactivityhub.dto.ActivityResponse;
import com.studentactivityhub.entity.Role;
import com.studentactivityhub.entity.User;
import com.studentactivityhub.repository.UserRepository;
import com.studentactivityhub.service.AIService;
import com.studentactivityhub.service.ActivityService;
import com.studentactivityhub.service.ResumePdfService;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/ai")
public class AIController {

    private final AIService aiService;
    private final ActivityService activityService;
    private final ResumePdfService resumePdfService;
    private final UserRepository userRepository;

    public AIController(
            AIService aiService,
            ActivityService activityService,
            ResumePdfService resumePdfService,
            UserRepository userRepository) {

        this.aiService = aiService;
        this.activityService = activityService;
        this.resumePdfService = resumePdfService;
        this.userRepository = userRepository;
    }

    // Get the authenticated user's ID
    private Long getAuthorizedStudentId(Long requestedStudentId) {

        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null
                || !authentication.isAuthenticated()) {

            throw new AccessDeniedException(
                    "Please log in to generate your resume.");
        }

        User loggedInUser = userRepository
                .findByEmail(authentication.getName())
                .orElseThrow(() ->
                        new RuntimeException("Logged-in user not found."));

        // Students can generate resumes only using their own achievements
        if (loggedInUser.getRole() == Role.STUDENT) {

            if (requestedStudentId != null
                    && !requestedStudentId.equals(loggedInUser.getId())) {

                throw new AccessDeniedException(
                        "You cannot access another student's achievements.");
            }

            return loggedInUser.getId();
        }

        // Faculty and admins may request a specific student's achievements
        if (loggedInUser.getRole() == Role.FACULTY
                || loggedInUser.getRole() == Role.ADMIN) {

            if (requestedStudentId == null) {
                throw new IllegalArgumentException(
                        "Student ID is required.");
            }

            return requestedStudentId;
        }

        throw new AccessDeniedException(
                "You are not allowed to generate this resume.");
    }

    // Generate resume using student details and saved achievements
    @PostMapping("/resume")
    public String generateResume(
            @RequestParam(required = false) Long studentId,
            @RequestBody AIResumeRequest request) {

        Long authorizedStudentId =
                getAuthorizedStudentId(studentId);

        List<ActivityResponse> achievements =
                activityService.getResumeAchievements(authorizedStudentId);

        StringBuilder achievementDetails = new StringBuilder();

        for (ActivityResponse achievement : achievements) {

            achievementDetails
                    .append("Title: ")
                    .append(achievement.getTitle())
                    .append("\nDescription: ")
                    .append(achievement.getDescription())
                    .append("\nCategory: ")
                    .append(achievement.getCategory())
                    .append("\nStatus: ")
                    .append(achievement.getStatus())
                    .append("\n\n");
        }

        request.setAchievements(
                achievementDetails.length() == 0
                        ? "No saved achievements available."
                        : achievementDetails.toString());

        return aiService.generateResume(request);
    }

    // Generate and download resume as PDF
    @PostMapping(
            value = "/resume/pdf",
            produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> downloadResumePdf(
            @RequestParam(required = false) Long studentId,
            @RequestBody AIResumeRequest request) {

        String resumeText = generateResume(studentId, request);

        byte[] pdfBytes = resumePdfService.generatePdf(resumeText);

        return ResponseEntity.ok()
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"Student_Resume.pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .contentLength(pdfBytes.length)
                .body(pdfBytes);
    }
}