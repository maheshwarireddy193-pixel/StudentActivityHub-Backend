package com.studentactivityhub.controller;

import com.studentactivityhub.service.FacultyPortfolioService;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/faculty/portfolios")
public class FacultyPortfolioController {

    private final FacultyPortfolioService facultyPortfolioService;

    public FacultyPortfolioController(
            FacultyPortfolioService facultyPortfolioService) {
        this.facultyPortfolioService = facultyPortfolioService;
    }

    // Get all student portfolios
    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> getAllStudentPortfolios() {
        return ResponseEntity.ok(
                facultyPortfolioService.getAllStudentPortfolios()
        );
    }

    // Get an individual student's portfolio
    @GetMapping("/{studentId}")
    public ResponseEntity<Map<String, Object>> getStudentPortfolio(
            @PathVariable Long studentId) {
        return ResponseEntity.ok(
                facultyPortfolioService.getStudentPortfolio(studentId)
        );
    }
}