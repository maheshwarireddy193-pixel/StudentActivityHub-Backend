package com.studentactivityhub.controller;

import com.studentactivityhub.dto.ActivityResponse;
import com.studentactivityhub.dto.VerificationRequest;
import com.studentactivityhub.service.FacultyService;
import com.studentactivityhub.util.FileStorageService;

import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/faculty")
public class FacultyController {

    private final FacultyService facultyService;
    private final FileStorageService fileStorageService;

    public FacultyController(
            FacultyService facultyService,
            FileStorageService fileStorageService) {
        this.facultyService = facultyService;
        this.fileStorageService = fileStorageService;
    }

    @GetMapping("/activities")
    public List<ActivityResponse> getAllActivities() {
        return facultyService.getAllActivities();
    }

    @GetMapping("/stats")
    public Map<String, Long> getDashboardStats() {
        return facultyService.getDashboardStats();
    }

    @PutMapping("/verify")
    public ActivityResponse verifyActivity(
            @RequestBody VerificationRequest request) {
        return facultyService.verifyActivity(request);
    }

    // View or download an uploaded certificate
    @GetMapping("/certificates/{filename:.+}")
    public ResponseEntity<Resource> getCertificate(
            @PathVariable String filename,
            @RequestParam(defaultValue = "false") boolean download) {

        // Verify the requester is an authorized faculty member.
    	facultyService.checkFacultyAccess();

    	try {
    	    Path filePath = fileStorageService.getFilePath(filename);

    	    if (!Files.exists(filePath) || !Files.isRegularFile(filePath)) {
    	        throw new ResponseStatusException(
    	                HttpStatus.NOT_FOUND,
    	                "Certificate file not found."
    	        );
    	    }
            Resource resource = new UrlResource(filePath.toUri());

            String contentType = Files.probeContentType(filePath);

            if (contentType == null) {
                String lowerName = filename.toLowerCase();

                if (lowerName.endsWith(".pdf")) {
                    contentType = "application/pdf";
                } else if (lowerName.endsWith(".png")) {
                    contentType = "image/png";
                } else if (lowerName.endsWith(".jpg")
                        || lowerName.endsWith(".jpeg")) {
                    contentType = "image/jpeg";
                } else {
                    contentType = MediaType.APPLICATION_OCTET_STREAM_VALUE;
                }
            }

            String disposition = download ? "attachment" : "inline";

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(
                            HttpHeaders.CONTENT_DISPOSITION,
                            disposition + "; filename=\"" + filename + "\"")
                    .body(resource);

        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Unable to retrieve certificate.");
        }
    }
    @GetMapping("/student-submission-report")
    public ResponseEntity<List<Map<String, Object>>> getStudentSubmissionReport() {
        return ResponseEntity.ok(facultyService.getStudentSubmissionReport());
    }
}