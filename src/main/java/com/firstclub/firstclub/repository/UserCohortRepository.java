package com.firstclub.firstclub.repository;

import com.firstclub.firstclub.entity.UserCohort;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserCohortRepository extends JpaRepository<UserCohort, Long> {

    List<UserCohort> findByUserId(Long userId);
}
