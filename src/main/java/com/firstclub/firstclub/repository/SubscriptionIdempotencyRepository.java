package com.firstclub.firstclub.repository;

import com.firstclub.firstclub.entity.SubscriptionIdempotency;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface SubscriptionIdempotencyRepository extends JpaRepository<SubscriptionIdempotency, Long> {

    Optional<SubscriptionIdempotency> findByIdempotencyKey(String idempotencyKey);

    @Query("""
            SELECT si FROM SubscriptionIdempotency si
            JOIN FETCH si.membership m
            JOIN FETCH m.user
            WHERE si.idempotencyKey = :idempotencyKey
            """)
    Optional<SubscriptionIdempotency> findByIdempotencyKeyWithMembership(
            @Param("idempotencyKey") String idempotencyKey);
}
