package com.studentactivityhub.service;

import com.studentactivityhub.dto.ActivityResponse;
import com.studentactivityhub.dto.VerificationRequest;
import com.studentactivityhub.entity.Activity;
import com.studentactivityhub.entity.ActivityStatus;
import com.studentactivityhub.entity.Role;
import com.studentactivityhub.entity.User;
import com.studentactivityhub.repository.ActivityRepository;
import com.studentactivityhub.repository.UserRepository;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class FacultyService {

    private final ActivityRepository activityRepository;
    private final UserRepository userRepository;

    public FacultyService(
            ActivityRepository activityRepository,
            UserRepository userRepository) {

        this.activityRepository = activityRepository;
        this.userRepository = userRepository;
    }

    // Get the authenticated user.
    // Only Faculty and Admin can access the read operations in this service.
    private User getLoggedInFaculty() {

        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication.getName() == null) {

            throw new AccessDeniedException(
                    "Authentication is required.");
        }

        User user = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() ->
                        new RuntimeException("User not found."));

        if (user.getRole() != Role.FACULTY
                && user.getRole() != Role.ADMIN) {

            throw new AccessDeniedException(
                    "Only Faculty and Admin can access this operation.");
        }

        return user;
    }

    // Public access check used by the controller where required.
    public void checkFacultyAccess() {
        getLoggedInFaculty();
    }

    // View all activity submissions.
    // Accessible to Faculty and Admin.
    @Transactional(readOnly = true)
    public List<ActivityResponse> getAllActivities() {

        getLoggedInFaculty();

        return activityRepository.findAll()
                .stream()
                .map(this::convertToResponse)
                .toList();
    }

    // Get activity dashboard statistics.
    // Accessible to Faculty and Admin.
    @Transactional(readOnly = true)
    public Map<String, Long> getDashboardStats() {

        getLoggedInFaculty();

        Map<String, Long> stats = new LinkedHashMap<>();

        stats.put("totalSubmissions", activityRepository.count());

        stats.put(
                "pending",
                activityRepository.countByStatus(ActivityStatus.PENDING)
        );

        stats.put(
                "verified",
                activityRepository.countByStatus(ActivityStatus.VERIFIED)
        );

        stats.put(
                "rejected",
                activityRepository.countByStatus(ActivityStatus.REJECTED)
        );

        return stats;
    }

    // Verify or reject an activity.
    // Only Faculty can perform this operation.
    @Transactional
    public ActivityResponse verifyActivity(VerificationRequest request) {

        User loggedInUser = getLoggedInFaculty();

        if (loggedInUser.getRole() != Role.FACULTY) {
            throw new AccessDeniedException(
                    "Only Faculty members can verify or reject activities.");
        }

        if (request == null || request.getActivityId() == null) {
            throw new IllegalArgumentException(
                    "Activity ID is required.");
        }

        Activity activity = activityRepository.findById(
                        request.getActivityId())
                .orElseThrow(() ->
                        new RuntimeException("Activity not found."));

        if (request.getStatus() == null
                || request.getStatus().isBlank()) {

            throw new IllegalArgumentException(
                    "Activity status is required.");
        }

        ActivityStatus status;

        try {
            status = ActivityStatus.valueOf(
                    request.getStatus()
                            .trim()
                            .toUpperCase(Locale.ROOT)
            );
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                    "Invalid status. Use VERIFIED or REJECTED.");
        }

        if (status == ActivityStatus.PENDING) {
            throw new IllegalArgumentException(
                    "Faculty can only verify or reject activities.");
        }

        activity.setStatus(status);
        activity.setReviewComment(request.getRemarks());
        activity.setReviewedBy(loggedInUser);

        Activity savedActivity = activityRepository.save(activity);

        return convertToResponse(savedActivity);
    }

    // Convert activity entity to a safe response DTO.
    private ActivityResponse convertToResponse(Activity activity) {

        ActivityResponse response = new ActivityResponse();

        response.setId(activity.getId());
        response.setTitle(activity.getTitle());
        response.setDescription(activity.getDescription());
        response.setCategory(activity.getCategory());
        response.setStatus(activity.getStatus().name());
        response.setCreatedAt(activity.getCreatedAt());
        response.setActivityDate(activity.getActivityDate());
        response.setOrganizer(activity.getOrganizer());

        // Student information.
        if (activity.getStudent() != null) {

            response.setStudentId(activity.getStudent().getId());
            response.setStudentName(activity.getStudent().getName());
            response.setStudentEmail(activity.getStudent().getEmail());

            response.setStudentRollNumber(
                    activity.getStudent().getRollNumber());
        }

        // Certificate filename, not a public file URL.
        response.setCertificateUrl(activity.getCertificatePath());

        // Review information.
        response.setReviewComment(activity.getReviewComment());

        if (activity.getReviewedBy() != null) {
            response.setReviewedBy(
                    activity.getReviewedBy().getName());
        }

        return response;
    }

    // Student-wise activity submission report.
    // Accessible to Faculty and Admin.
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getStudentSubmissionReport() {

        getLoggedInFaculty();

        List<Activity> activities = activityRepository.findAll();

        Map<Long, Map<String, Object>> studentReports =
                new LinkedHashMap<>();

        for (Activity activity : activities) {

            User student = activity.getStudent();

            if (student == null) {
                continue;
            }

            Long studentId = student.getId();

            Map<String, Object> report =
                    studentReports.get(studentId);

            if (report == null) {

                report = new LinkedHashMap<>();

                report.put("studentId", studentId);
                report.put("studentName", student.getName());
                report.put("studentEmail", student.getEmail());
                report.put("rollNumber", student.getRollNumber());

                report.put("totalSubmissions", 0L);
                report.put("pending", 0L);
                report.put("verified", 0L);
                report.put("rejected", 0L);

                studentReports.put(studentId, report);
            }

            report.put(
                    "totalSubmissions",
                    (Long) report.get("totalSubmissions") + 1L
            );

            if (activity.getStatus() == ActivityStatus.PENDING) {

                report.put(
                        "pending",
                        (Long) report.get("pending") + 1L
                );

            } else if (activity.getStatus() == ActivityStatus.VERIFIED) {

                report.put(
                        "verified",
                        (Long) report.get("verified") + 1L
                );

            } else if (activity.getStatus() == ActivityStatus.REJECTED) {

                report.put(
                        "rejected",
                        (Long) report.get("rejected") + 1L
                );
            }
        }

        return new ArrayList<>(studentReports.values());
    }
}