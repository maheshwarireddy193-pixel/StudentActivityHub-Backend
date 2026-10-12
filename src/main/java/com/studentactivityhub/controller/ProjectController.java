package com.studentactivityhub.controller;

import com.studentactivityhub.entity.Project;
import com.studentactivityhub.service.ProjectService;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/projects")
public class ProjectController {

    private final ProjectService projectService;

    public ProjectController(ProjectService projectService) {
        this.projectService = projectService;
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> addProject(
            @RequestBody Project project) {

        Project savedProject = projectService.addProject(project);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(toResponse(savedProject));
    }

    @GetMapping("/my")
    public ResponseEntity<List<Map<String, Object>>> getMyProjects() {

        List<Map<String, Object>> projects =
                projectService.getMyProjects()
                        .stream()
                        .map(this::toResponse)
                        .toList();

        return ResponseEntity.ok(projects);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> updateProject(
            @PathVariable Long id,
            @RequestBody Project project) {

        Project updatedProject =
                projectService.updateProject(id, project);

        return ResponseEntity.ok(toResponse(updatedProject));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProject(
            @PathVariable Long id) {

        projectService.deleteProject(id);

        return ResponseEntity.noContent().build();
    }

    private Map<String, Object> toResponse(Project project) {

        Map<String, Object> response = new LinkedHashMap<>();

        response.put("id", project.getId());
        response.put("title", project.getTitle());
        response.put("description", project.getDescription());
        response.put("technologies", project.getTechnologies());
        response.put("githubUrl", project.getGithubUrl());
        response.put("liveDemoUrl", project.getLiveDemoUrl());
        response.put("createdAt", project.getCreatedAt());

        if (project.getStudent() != null) {
            response.put("studentId", project.getStudent().getId());
            response.put("studentName", project.getStudent().getName());
            response.put("studentEmail", project.getStudent().getEmail());
        }

        return response;
    }
}