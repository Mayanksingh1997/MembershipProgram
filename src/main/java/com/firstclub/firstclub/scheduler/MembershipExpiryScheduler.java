package com.firstclub.firstclub.scheduler;

import com.firstclub.firstclub.service.SubscriptionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class MembershipExpiryScheduler {

    private static final Logger log = LoggerFactory.getLogger(MembershipExpiryScheduler.class);

    private final SubscriptionService subscriptionService;

    public MembershipExpiryScheduler(SubscriptionService subscriptionService) {
        this.subscriptionService = subscriptionService;
    }

    @Scheduled(cron = "${membership.expiry-cron}")
    public void expireMemberships() {
        int expiredCount = subscriptionService.expireMemberships();
        if (expiredCount > 0) {
            log.info("Expired {} memberships", expiredCount);
        }
    }
}
