package com.studentactivityhub.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.studentactivityhub.entity.Activity;
import com.studentactivityhub.entity.ActivityStatus;

public interface ActivityRepository extends JpaRepository<Activity, Long> {

    List<Activity> findByStudentId(Long studentId);
    
    List<Activity> findByStudentIdAndStatus(
            Long studentId, ActivityStatus status);

    List<Activity> findByStatus(ActivityStatus status);

    List<Activity> findByCategoryIgnoreCase(String category);

    long countByStatus(ActivityStatus status);
    
    long countByStudentId(Long studentId);
    
    
 
    
}