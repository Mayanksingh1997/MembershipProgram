package com.firstclub.firstclub.controller;

import com.firstclub.firstclub.constants.ApiConstants;
import com.firstclub.firstclub.dto.request.CancelMembershipRequest;
import com.firstclub.firstclub.dto.request.ChangeTierRequest;
import com.firstclub.firstclub.dto.request.SubscribeRequest;
import com.firstclub.firstclub.dto.request.UpdateOrderStatsRequest;
import com.firstclub.firstclub.dto.response.MembershipResponse;
import com.firstclub.firstclub.dto.response.OrderStatsResponse;
import com.firstclub.firstclub.dto.response.ResolvedBenefitsResponse;
import com.firstclub.firstclub.helper.RequestContextHelper;
import com.firstclub.firstclub.service.BenefitResolutionService;
import com.firstclub.firstclub.service.OrderStatsService;
import com.firstclub.firstclub.service.SubscriptionService;
import com.firstclub.firstclub.service.UserMembershipLockManager;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(ApiConstants.BASE_URL)
public class MembershipSubscriptionController {

    @Autowired
    private SubscriptionService subscriptionService;

    @Autowired
    private BenefitResolutionService benefitResolutionService;

    @Autowired
    private OrderStatsService orderStatsService;

    @Autowired
    private UserMembershipLockManager lockManager;

    @Autowired
    private RequestContextHelper requestContextHelper;

    @PostMapping("/subscribe")
    public ResponseEntity<MembershipResponse> subscribe(
            HttpServletRequest httpRequest,
            @Valid @RequestBody SubscribeRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        String userId = requestContextHelper.getAuthenticatedUserId(httpRequest);
        lockManager.lock(userId);
        try {
            return subscriptionService.subscribe(userId, request, idempotencyKey);
        } finally {
            lockManager.unlock(userId);
        }
    }

    @GetMapping("/me")
    public ResponseEntity<MembershipResponse> getMembership(HttpServletRequest httpRequest) {
        String userId = requestContextHelper.getAuthenticatedUserId(httpRequest);
        return subscriptionService.getMembership(userId);
    }

    @PatchMapping("/tier")
    public ResponseEntity<MembershipResponse> changeTier(
            HttpServletRequest httpRequest,
            @Valid @RequestBody ChangeTierRequest request) {
        String userId = requestContextHelper.getAuthenticatedUserId(httpRequest);
        lockManager.lock(userId);
        try {
            return subscriptionService.changeTier(userId, request);
        } finally {
            lockManager.unlock(userId);
        }
    }

    @PostMapping("/cancel")
    public ResponseEntity<MembershipResponse> cancel(
            HttpServletRequest httpRequest,
            @Valid @RequestBody(required = false) CancelMembershipRequest request) {
        String userId = requestContextHelper.getAuthenticatedUserId(httpRequest);
        CancelMembershipRequest cancelRequest = request != null
                ? request
                : CancelMembershipRequest.builder().build();
        lockManager.lock(userId);
        try {
            return subscriptionService.cancel(userId, cancelRequest);
        } finally {
            lockManager.unlock(userId);
        }
    }

    @PostMapping("/evaluate-tier")
    public ResponseEntity<MembershipResponse> evaluateTier(HttpServletRequest httpRequest) {
        String userId = requestContextHelper.getAuthenticatedUserId(httpRequest);
        lockManager.lock(userId);
        try {
            return subscriptionService.evaluateTier(userId);
        } finally {
            lockManager.unlock(userId);
        }
    }

    @GetMapping("/benefits")
    public ResponseEntity<ResolvedBenefitsResponse> getBenefits(HttpServletRequest httpRequest) {
        String userId = requestContextHelper.getAuthenticatedUserId(httpRequest);
        return benefitResolutionService.resolveBenefits(userId);
    }

    @GetMapping("/order-stats")
    public ResponseEntity<OrderStatsResponse> getOrderStats(HttpServletRequest httpRequest) {
        String userId = requestContextHelper.getAuthenticatedUserId(httpRequest);
        return orderStatsService.getOrderStats(userId);
    }

    @PostMapping("/order-stats")
    public ResponseEntity<Void> updateOrderStats(
            HttpServletRequest httpRequest,
            @Valid @RequestBody UpdateOrderStatsRequest request) {
        String userId = requestContextHelper.getAuthenticatedUserId(httpRequest);
        lockManager.lock(userId);
        try {
            return orderStatsService.updateOrderStats(userId, request);
        } finally {
            lockManager.unlock(userId);
        }
    }
}
