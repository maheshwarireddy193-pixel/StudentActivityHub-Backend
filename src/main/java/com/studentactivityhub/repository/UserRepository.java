package com.studentactivityhub.repository;

import com.studentactivityhub.entity.User;


import org.springframework.data.jpa.repository.JpaRepository;
import com.studentactivityhub.entity.Role;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);
    
    List<User> findByRole(Role role);
}