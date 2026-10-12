package com.studentactivityhub.controller;

import com.studentactivityhub.dto.ActivityRequest;
import com.studentactivityhub.dto.ActivityResponse;
import com.studentactivityhub.service.ActivityService;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/activities")
public class ActivityController {

    private final ActivityService activityService;

    public ActivityController(ActivityService activityService) {
        this.activityService = activityService;
    }

    // Existing JSON endpoint
    @PostMapping
    public ResponseEntity<ActivityResponse> createActivity(
            @RequestBody ActivityRequest request) {

        ActivityResponse response =
                activityService.createActivity(request);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // New endpoint for actual certificate uploads
    @PostMapping(
    			value = "/upload",
    		    consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    		public ResponseEntity<ActivityResponse> uploadActivity(
    		        @RequestParam("title") String title,
    		        @RequestParam(value = "description", required = false)
    		                String description,
    		        @RequestParam("category") String category,
    		        @RequestParam(value = "organizer", required = false)
    		                String organizer,
    		        @RequestParam(value = "date", required = false)
    		                String date,
    		        @RequestParam("certificate") MultipartFile certificate) {

    		    ActivityResponse response =
    		            activityService.createActivityWithCertificate(
    		                    title,
    		                    description,
    		                    category,
    		                    organizer,
    		                    date,
    		                    certificate);

    		    return ResponseEntity.status(HttpStatus.CREATED).body(response);
    		}
    		

    @GetMapping
    public ResponseEntity<List<ActivityResponse>> getAllActivities() {

        return ResponseEntity.ok(activityService.getAllActivities());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ActivityResponse> getActivityById(
            @PathVariable Long id) {

        return ResponseEntity.ok(
                activityService.getActivityById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ActivityResponse> updateActivity(
            @PathVariable Long id,
            @RequestBody ActivityRequest request) {

        return ResponseEntity.ok(
                activityService.updateActivity(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteActivity(
            @PathVariable Long id) {

        activityService.deleteActivity(id);

        return ResponseEntity.noContent().build();
    }
}