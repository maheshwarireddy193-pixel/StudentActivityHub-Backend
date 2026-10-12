package com.studentactivityhub.service;

import com.studentactivityhub.entity.Project;
import com.studentactivityhub.entity.Role;
import com.studentactivityhub.entity.User;
import com.studentactivityhub.repository.ProjectRepository;
import com.studentactivityhub.repository.UserRepository;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class FacultyPortfolioService {

    private final UserRepository userRepository;
    private final ProjectRepository projectRepository;

    public FacultyPortfolioService(
            UserRepository userRepository,
            ProjectRepository projectRepository) {

        this.userRepository = userRepository;
        this.projectRepository = projectRepository;
    }

    private void checkFacultyAccess() {

        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null
                || !authentication.isAuthenticated()) {

            throw new AccessDeniedException(
                    "Authentication is required");
        }

        User faculty = userRepository.findByEmail(
                authentication.getName()
        ).orElseThrow(() ->
                new AccessDeniedException("User not found"));
        if (faculty.getRole() != Role.FACULTY
                && faculty.getRole() != Role.ADMIN) {
            throw new AccessDeniedException(
                "Only faculty or admin can view student portfolios"
            );
        }
        

            }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getAllStudentPortfolios() {

        checkFacultyAccess();

        List<User> students =
                userRepository.findByRole(Role.STUDENT);

        return students.stream()
                .map(this::buildStudentPortfolio)
                .toList();
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getStudentPortfolio(Long studentId) {

        checkFacultyAccess();

        User student = userRepository.findById(studentId)
                .orElseThrow(() ->
                        new RuntimeException("Student not found"));

        if (student.getRole() != Role.STUDENT) {
            throw new RuntimeException(
                    "The requested user is not a student");
        }

        return buildStudentPortfolio(student);
    }

    private Map<String, Object> buildStudentPortfolio(User student) {

        Map<String, Object> portfolio = new LinkedHashMap<>();

        portfolio.put("studentId", student.getId());
        portfolio.put("name", student.getName());
        portfolio.put("email", student.getEmail());
        portfolio.put("rollNumber", student.getRollNumber());

        portfolio.put("githubUrl", student.getGithubUrl());
        portfolio.put("linkedinUrl", student.getLinkedinUrl());
        portfolio.put("leetcodeUrl", student.getLeetcodeUrl());
        portfolio.put("portfolioUrl", student.getPortfolioUrl());

        List<Map<String, Object>> projects =
                projectRepository.findByStudentId(student.getId())
                        .stream()
                        .map(this::buildProjectResponse)
                        .toList();

        portfolio.put("projects", projects);

        return portfolio;
    }

    private Map<String, Object> buildProjectResponse(Project project) {

        Map<String, Object> projectData = new LinkedHashMap<>();

        projectData.put("id", project.getId());
        projectData.put("title", project.getTitle());
        projectData.put("description", project.getDescription());
        projectData.put("technologies", project.getTechnologies());
        projectData.put("githubUrl", project.getGithubUrl());
        projectData.put("liveDemoUrl", project.getLiveDemoUrl());
        projectData.put("createdAt", project.getCreatedAt());

        return projectData;
    }
}