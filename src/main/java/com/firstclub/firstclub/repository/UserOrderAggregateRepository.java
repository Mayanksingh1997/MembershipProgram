package com.firstclub.firstclub.repository;

import com.firstclub.firstclub.entity.UserOrderAggregate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserOrderAggregateRepository extends JpaRepository<UserOrderAggregate, Long> {

    Optional<UserOrderAggregate> findByUserId(Long userId);
}
