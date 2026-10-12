package com.studentactivityhub.service;

import com.studentactivityhub.entity.User;
import com.studentactivityhub.repository.UserRepository;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProfileService {

    private final UserRepository userRepository;

    public ProfileService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    private User getLoggedInUser() {

        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null ||
                !authentication.isAuthenticated()) {
            throw new RuntimeException("User is not authenticated");
        }

        String email = authentication.getName();

        return userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new RuntimeException("User not found"));
    }

    @Transactional(readOnly = true)
    public User getMyProfile() {

        return getLoggedInUser();
    }

    @Transactional
    public User updateMyProfile(
            String githubUrl,
            String linkedinUrl,
            String leetcodeUrl,
            String portfolioUrl) {

        User user = getLoggedInUser();

        user.setGithubUrl(githubUrl);
        user.setLinkedinUrl(linkedinUrl);
        user.setLeetcodeUrl(leetcodeUrl);
        user.setPortfolioUrl(portfolioUrl);

        return userRepository.save(user);
    }
}