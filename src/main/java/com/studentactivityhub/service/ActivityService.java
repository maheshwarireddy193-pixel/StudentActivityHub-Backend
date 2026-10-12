package com.studentactivityhub.service;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.databind.JsonNode;
import com.studentactivityhub.dto.ActivityRequest;
import com.studentactivityhub.dto.ActivityResponse;
import com.studentactivityhub.entity.Activity;
import com.studentactivityhub.entity.ActivityStatus;
import com.studentactivityhub.entity.Role;
import com.studentactivityhub.entity.User;
import com.studentactivityhub.repository.ActivityRepository;
import com.studentactivityhub.repository.UserRepository;
import com.studentactivityhub.util.FileStorageService;

@Service
public class ActivityService {

    private final ActivityRepository activityRepository;
    private final UserRepository userRepository;
    private final FileStorageService fileStorageService;
    private final AIService aiService;

    public ActivityService(
            ActivityRepository activityRepository,
            UserRepository userRepository,
            FileStorageService fileStorageService,
            AIService aiService) {

        this.activityRepository = activityRepository;
        this.userRepository = userRepository;
        this.fileStorageService = fileStorageService;
        this.aiService = aiService;
    }

    // Get the currently logged-in user
    private User getLoggedInUser() {

        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication.getName() == null) {

            throw new IllegalStateException(
                    "Please log in before accessing activities.");
        }

        return userRepository.findByEmail(authentication.getName())
                .orElseThrow(() ->
                        new RuntimeException("Logged-in user not found."));
    }

    // Ensure the user is a student
    private User getLoggedInStudent() {

        User user = getLoggedInUser();

        if (user.getRole() != Role.STUDENT) {
            throw new IllegalStateException(
                    "Only students can perform this operation.");
        }

        return user;
    }

    // Find an activity or return an error
    private Activity findActivity(Long id) {

        return activityRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Activity not found with ID: " + id));
    }

    // Ensure the activity belongs to the logged-in student
    private Activity findOwnActivity(Long id) {

        User student = getLoggedInStudent();
        Activity activity = findActivity(id);

        if (!activity.getStudent().getId().equals(student.getId())) {

            throw new AccessDeniedException(
                    "You cannot access another student's activity.");
        }

        return activity;
    }

    // Create an activity using certificate upload
    @Transactional
    public ActivityResponse createActivityWithCertificate(
            String title,
            String description,
            String category,
            String organizer,
            String date,
            MultipartFile certificate) {

        User student = getLoggedInStudent();

        // Validate the submitted details
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException(
                    "Activity title is required.");
        }

        if (category == null || category.isBlank()) {
            throw new IllegalArgumentException(
                    "Activity category is required.");
        }

        if (certificate == null || certificate.isEmpty()) {
            throw new IllegalArgumentException(
                    "Please upload your certificate.");
        }

        // Step 1: Store the uploaded certificate
        String storedFilename = fileStorageService.storeFile(certificate);

        // Step 2: Create an activity using the student's details
        Activity activity = new Activity();

        activity.setTitle(title.trim());
        activity.setDescription(description);
        activity.setCategory(category.trim());
        activity.setOrganizer(organizer);

        if (date != null && !date.isBlank()) {
            activity.setActivityDate(LocalDate.parse(date));
        }
        activity.setCertificatePath(storedFilename);
        activity.setStudent(student);
        activity.setStatus(ActivityStatus.PENDING);

        // Step 3: Save the activity before calling Gemini
        Activity savedActivity = activityRepository.save(activity);

        // Step 4: Attempt AI analysis for PDF certificates
        String originalFilename = certificate.getOriginalFilename();

        if (originalFilename != null
                && originalFilename.toLowerCase().endsWith(".pdf")) {

            try {
                byte[] pdfBytes = certificate.getBytes();

                try (PDDocument document = Loader.loadPDF(pdfBytes)) {

                    PDFTextStripper textStripper = new PDFTextStripper();
                    String extractedText = textStripper.getText(document);

                    // Continue without AI if the PDF has no readable text
                    if (extractedText != null && !extractedText.isBlank()) {

                        try {
                            JsonNode analysis =
                                    aiService.generateCertificateAnalysis(
                                            extractedText);

                            if (analysis != null
                                    && analysis.hasNonNull("title")
                                    && !analysis.get("title")
                                            .asText().isBlank()) {

                                savedActivity.setTitle(
                                        analysis.get("title").asText().trim());
                            }

                            if (analysis != null
                                    && analysis.hasNonNull("description")
                                    && !analysis.get("description")
                                            .asText().isBlank()) {

                                savedActivity.setDescription(
                                        analysis.get("description").asText());
                            }

                            // Update the activity if AI returns improvements
                            savedActivity =
                                    activityRepository.save(savedActivity);

                        } catch (Exception aiException) {

                            // Keep the student's original details
                            System.err.println(
                                    "Gemini analysis failed for activity "
                                    + savedActivity.getId()
                                    + ". Keeping the student's details. "
                                    + aiException.getMessage());
                        }
                    }
                }

            } catch (IOException pdfException) {

                // Keep the saved activity even if PDF analysis fails
                System.err.println(
                        "PDF analysis failed for activity "
                        + savedActivity.getId()
                        + ". " + pdfException.getMessage());
            }
        }

        // Step 5: Return the saved activity
        return convertToResponse(savedActivity);
    }

    // Create an activity using JSON
    @Transactional
    public ActivityResponse createActivity(ActivityRequest request) {

        User student = getLoggedInStudent();

        if (request.getTitle() == null
                || request.getTitle().isBlank()) {

            throw new IllegalArgumentException(
                    "Activity title is required.");
        }

        if (request.getCategory() == null
                || request.getCategory().isBlank()) {

            throw new IllegalArgumentException(
                    "Activity category is required.");
        }

        Activity activity = new Activity();

        activity.setTitle(request.getTitle().trim());
        activity.setDescription(request.getDescription());
        activity.setCategory(request.getCategory().trim());
        activity.setCertificatePath(request.getCertificateUrl());
        activity.setStudent(student);

        Activity savedActivity = activityRepository.save(activity);

        return convertToResponse(savedActivity);
    }

    // Students see their own activities.
    // Faculty and admins can see all activities.
    @Transactional(readOnly = true)
    public List<ActivityResponse> getAllActivities() {

        User user = getLoggedInUser();

        if (user.getRole() == Role.STUDENT) {

            return activityRepository.findByStudentId(user.getId())
                    .stream()
                    .map(this::convertToResponse)
                    .toList();
        }

        if (user.getRole() == Role.FACULTY
                || user.getRole() == Role.ADMIN) {

            return activityRepository.findAll()
                    .stream()
                    .map(this::convertToResponse)
                    .toList();
        }

        throw new AccessDeniedException(
                "You are not allowed to view activities.");
    }

    // Get an activity by ID
    @Transactional(readOnly = true)
    public ActivityResponse getActivityById(Long id) {

        User user = getLoggedInUser();
        Activity activity = findActivity(id);

        if (user.getRole() == Role.STUDENT
                && !activity.getStudent().getId().equals(user.getId())) {

            throw new AccessDeniedException(
                    "You cannot access another student's activity.");
        }

        if (user.getRole() != Role.STUDENT
                && user.getRole() != Role.FACULTY
                && user.getRole() != Role.ADMIN) {

            throw new AccessDeniedException(
                    "You are not allowed to view this activity.");
        }

        return convertToResponse(activity);
    }

    // Update only your own pending activity
    @Transactional
    public ActivityResponse updateActivity(
            Long id,
            ActivityRequest request) {

        Activity activity = findOwnActivity(id);

        if (activity.getStatus() != ActivityStatus.PENDING) {
            throw new IllegalStateException(
                    "Only pending activities can be updated.");
        }

        if (request.getTitle() == null
                || request.getTitle().isBlank()) {

            throw new IllegalArgumentException(
                    "Activity title is required.");
        }

        if (request.getCategory() == null
                || request.getCategory().isBlank()) {

            throw new IllegalArgumentException(
                    "Activity category is required.");
        }

        activity.setTitle(request.getTitle().trim());
        activity.setDescription(request.getDescription());
        activity.setCategory(request.getCategory().trim());
        activity.setCertificatePath(request.getCertificateUrl());

        Activity updatedActivity = activityRepository.save(activity);

        return convertToResponse(updatedActivity);
    }

    // Delete only your own pending activity
    @Transactional
    public void deleteActivity(Long id) {

        Activity activity = findOwnActivity(id);

        if (activity.getStatus() != ActivityStatus.PENDING) {
            throw new IllegalStateException(
                    "Only pending activities can be deleted.");
        }

        activityRepository.delete(activity);
    }

    // Convert entity to response DTO
    private ActivityResponse convertToResponse(Activity activity) {

        ActivityResponse response = new ActivityResponse();

        // Activity details
        response.setId(activity.getId());
        response.setTitle(activity.getTitle());
        response.setDescription(activity.getDescription());
        response.setCategory(activity.getCategory());
        response.setStatus(activity.getStatus().name());
        response.setCreatedAt(activity.getCreatedAt());
        response.setActivityDate(activity.getActivityDate());
        response.setOrganizer(activity.getOrganizer());

        // Student details
        if (activity.getStudent() != null) {
            response.setStudentId(activity.getStudent().getId());
            response.setStudentName(activity.getStudent().getName());
            response.setStudentEmail(activity.getStudent().getEmail());
            response.setStudentRollNumber(
                    activity.getStudent().getRollNumber());
        }

        // Certificate information
        response.setCertificateUrl(activity.getCertificatePath());

        // Faculty review details
        response.setReviewComment(activity.getReviewComment());

        if (activity.getReviewedBy() != null) {
            response.setReviewedBy(activity.getReviewedBy().getName());
        }

        return response;
    }
    // Get verified achievements for a specific student
    @Transactional(readOnly = true)
    public List<ActivityResponse> getVerifiedAchievements(Long studentId) {

        return activityRepository.findByStudentIdAndStatus(
                        studentId,
                        ActivityStatus.VERIFIED)
                .stream()
                .map(this::convertToResponse)
                .toList();
    }

    // Get verified and pending achievements for a resume
    @Transactional(readOnly = true)
    public List<ActivityResponse> getResumeAchievements(Long studentId) {

        return activityRepository.findByStudentId(studentId)
                .stream()
                .filter(activity ->
                        activity.getStatus() == ActivityStatus.VERIFIED
                        || activity.getStatus() == ActivityStatus.PENDING)
                .map(this::convertToResponse)
                .toList();
    }
}