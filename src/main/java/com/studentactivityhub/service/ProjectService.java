package com.studentactivityhub.service;

import com.studentactivityhub.entity.Project;
import com.studentactivityhub.entity.User;
import com.studentactivityhub.repository.ProjectRepository;
import com.studentactivityhub.repository.UserRepository;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;

    public ProjectService(
            ProjectRepository projectRepository,
            UserRepository userRepository) {
        this.projectRepository = projectRepository;
        this.userRepository = userRepository;
    }

    private User getLoggedInStudent() {

        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null
                || !authentication.isAuthenticated()) {
            throw new RuntimeException("User is not authenticated");
        }

        String email = authentication.getName();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new RuntimeException("User not found"));

        if (user.getRole() != com.studentactivityhub.entity.Role.STUDENT) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "Only students can manage projects");
        }

        return user;
    }

    @Transactional
    public Project addProject(Project project) {

        User student = getLoggedInStudent();

        validateProject(project);

        project.setId(null);
        project.setStudent(student);

        return projectRepository.save(project);
    }

    @Transactional(readOnly = true)
    public List<Project> getMyProjects() {

        User student = getLoggedInStudent();

        return projectRepository.findByStudentId(student.getId());
    }

    @Transactional
    public Project updateProject(Long projectId, Project updatedProject) {

        User student = getLoggedInStudent();

        Project existingProject = projectRepository.findById(projectId)
                .orElseThrow(() ->
                        new RuntimeException("Project not found"));

        if (!existingProject.getStudent().getId().equals(student.getId())) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "You cannot update another student's project");
        }

        validateProject(updatedProject);

        existingProject.setTitle(updatedProject.getTitle());
        existingProject.setDescription(updatedProject.getDescription());
        existingProject.setTechnologies(updatedProject.getTechnologies());
        existingProject.setGithubUrl(updatedProject.getGithubUrl());
        existingProject.setLiveDemoUrl(updatedProject.getLiveDemoUrl());

        return projectRepository.save(existingProject);
    }

    @Transactional
    public void deleteProject(Long projectId) {

        User student = getLoggedInStudent();

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() ->
                        new RuntimeException("Project not found"));

        if (!project.getStudent().getId().equals(student.getId())) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "You cannot delete another student's project");
        }

        projectRepository.delete(project);
    }

    private void validateProject(Project project) {

        if (project.getTitle() == null
                || project.getTitle().isBlank()) {
            throw new IllegalArgumentException(
                    "Project title is required");
        }

        if (project.getTitle().length() > 255) {
            throw new IllegalArgumentException(
                    "Project title cannot exceed 255 characters");
        }

        validateUrl(project.getGithubUrl(), "GitHub URL");
        validateUrl(project.getLiveDemoUrl(), "Live demo URL");
    }

    private void validateUrl(String value, String field) {

        if (value == null || value.isBlank()) {
            return;
        }

        try {
            java.net.URI uri = java.net.URI.create(value.trim());

            if (!("http".equalsIgnoreCase(uri.getScheme())
                    || "https".equalsIgnoreCase(uri.getScheme()))
                    || uri.getHost() == null) {
                throw new IllegalArgumentException(
                        field + " must be a valid HTTP or HTTPS URL");
            }

        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException(
                    field + " must be a valid HTTP or HTTPS URL");
        }
    }
}