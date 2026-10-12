package com.studentactivityhub.service;

import com.studentactivityhub.dto.UserResponse;
import com.studentactivityhub.entity.Role;
import com.studentactivityhub.entity.User;
import com.studentactivityhub.entity.ActivityStatus;
import com.studentactivityhub.repository.UserRepository;
import com.studentactivityhub.repository.ActivityRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class AdminService {

    private final UserRepository userRepository;
    private final ActivityRepository activityRepository;

    public AdminService(
            UserRepository userRepository,
            ActivityRepository activityRepository) {

        this.userRepository = userRepository;
        this.activityRepository = activityRepository;
    }

    private UserResponse convertToResponse(User user) {
        return new UserResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole()
        );
    }

    // Get all users
    public List<UserResponse> getAllUsers() {
        return userRepository.findAll()
                .stream()
                .map(this::convertToResponse)
                .toList();
    }

    // Get user by ID
    public UserResponse getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException(
                                "User not found with ID: " + id));

        return convertToResponse(user);
    }

    // Dashboard statistics
    public Map<String, Long> getDashboardStats() {

        Map<String, Long> stats = new LinkedHashMap<>();

        stats.put("totalUsers", userRepository.count());
        stats.put("totalStudents",
                (long) userRepository.findByRole(Role.STUDENT).size());
        stats.put("totalFaculty",
                (long) userRepository.findByRole(Role.FACULTY).size());
        stats.put("totalAdmins",
                (long) userRepository.findByRole(Role.ADMIN).size());

        stats.put("totalActivities", activityRepository.count());
        stats.put("pendingActivities",
                activityRepository.countByStatus(ActivityStatus.PENDING));

        return stats;
    }

    // Delete user only if they have no activities
    @Transactional
    public void deleteUser(Long id) {

        User user = userRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException(
                                "User not found with ID: " + id));

        if (user.getRole() == Role.ADMIN) {
            throw new IllegalStateException(
                    "Admin accounts cannot be deleted here.");
        }

        if (!activityRepository.findByStudentId(id).isEmpty()) {
            throw new IllegalStateException(
                    "Cannot delete this user because they have activities.");
        }

        userRepository.delete(user);
    }
}