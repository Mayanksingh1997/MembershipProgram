package com.firstclub.firstclub.repository;

import com.firstclub.firstclub.constants.MembershipStatus;
import com.firstclub.firstclub.entity.UserMembership;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface UserMembershipRepository extends JpaRepository<UserMembership, Long> {

    Optional<UserMembership> findByUserIdAndStatus(Long userId, MembershipStatus status);

    @Query("""
            SELECT m FROM UserMembership m
            JOIN FETCH m.user
            WHERE m.user.externalUserId = :externalUserId AND m.status = :status
            """)
    Optional<UserMembership> findActiveByExternalUserId(
            @Param("externalUserId") String externalUserId,
            @Param("status") MembershipStatus status);

    List<UserMembership> findByStatusAndEndDateBefore(MembershipStatus status, LocalDate date);
}
