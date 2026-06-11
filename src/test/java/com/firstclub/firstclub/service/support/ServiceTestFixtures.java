package com.firstclub.firstclub.service.support;

import com.firstclub.firstclub.configuration.auth.AuthProperties;
import com.firstclub.firstclub.configuration.catalog.BenefitDefinition;
import com.firstclub.firstclub.configuration.catalog.EligibilityRuleDefinition;
import com.firstclub.firstclub.configuration.catalog.MembershipCatalogProperties;
import com.firstclub.firstclub.configuration.catalog.MembershipCatalogProvider;
import com.firstclub.firstclub.configuration.catalog.PlanDefinition;
import com.firstclub.firstclub.configuration.catalog.TierDefinition;
import com.firstclub.firstclub.constants.BenefitCode;
import com.firstclub.firstclub.constants.ComparisonOperator;
import com.firstclub.firstclub.constants.MembershipStatus;
import com.firstclub.firstclub.constants.PlanCode;
import com.firstclub.firstclub.constants.RuleType;
import com.firstclub.firstclub.constants.TierCode;
import com.firstclub.firstclub.entity.UserAccount;
import com.firstclub.firstclub.entity.UserMembership;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public final class ServiceTestFixtures {

    public static final String DEFAULT_SIGNING_KEY =
            "Zmlyc3RjbHViLXNlY3JldC1rZXktZm9yLWhzMjU2LXNpbmluZy1taW5pbXVtLTMyLWJ5dGVz";

    private ServiceTestFixtures() {
    }

    public static AuthProperties authProperties() {
        AuthProperties properties = new AuthProperties();
        AuthProperties.Jwt jwt = new AuthProperties.Jwt();
        jwt.setSigningKey(DEFAULT_SIGNING_KEY);
        jwt.setIssuer("firstclub");
        jwt.setAudience("firstclub-api");
        jwt.setAccessTokenTtlMinutes(10);
        jwt.setRefreshTokenTtlDays(7);
        properties.setJwt(jwt);
        return properties;
    }

    public static UserAccount user(String externalUserId) {
        return UserAccount.builder()
                .id(1L)
                .externalUserId(externalUserId)
                .email(externalUserId + "@firstclub.co.in")
                .passwordHash("$2a$10$encoded")
                .name("Test User")
                .build();
    }

    public static UserMembership membership(UserAccount user, TierCode tierCode, PlanCode planCode) {
        LocalDate startDate = LocalDate.now();
        return UserMembership.builder()
                .id(10L)
                .user(user)
                .planCode(planCode)
                .tierCode(tierCode)
                .status(MembershipStatus.ACTIVE)
                .startDate(startDate)
                .endDate(startDate.plusDays(planCode == PlanCode.QUARTERLY ? 90 : 30))
                .autoRenew(true)
                .build();
    }

    public static PlanDefinition monthlyPlan() {
        PlanDefinition plan = new PlanDefinition();
        plan.setCode(PlanCode.MONTHLY);
        plan.setName("Monthly Plan");
        plan.setPrice(BigDecimal.valueOf(99));
        plan.setDurationDays(30);
        plan.setActive(true);
        return plan;
    }

    public static MembershipCatalogProvider catalogProvider() {
        MembershipCatalogProperties properties = new MembershipCatalogProperties();
        properties.setPlans(List.of(monthlyPlan()));
        properties.setTiers(List.of(silverTier(), goldTier(), platinumTier()));
        return new MembershipCatalogProvider(properties);
    }

    public static TierDefinition silverTier() {
        TierDefinition tier = baseTier(TierCode.SILVER, 1, 5);
        tier.setEligibilityRules(List.of(orderCountRule(0)));
        tier.setBenefits(List.of(
                benefit(BenefitCode.FREE_DELIVERY),
                benefit(BenefitCode.EXTRA_DISCOUNT)));
        return tier;
    }

    public static TierDefinition goldTier() {
        TierDefinition tier = baseTier(TierCode.GOLD, 2, 10);
        tier.setEligibilityRules(List.of(orderCountRule(10)));
        tier.setBenefits(List.of(
                benefit(BenefitCode.FREE_DELIVERY),
                benefit(BenefitCode.EXTRA_DISCOUNT),
                benefit(BenefitCode.EXCLUSIVE_DEALS)));
        return tier;
    }

    public static TierDefinition platinumTier() {
        TierDefinition tier = baseTier(TierCode.PLATINUM, 3, 15);
        EligibilityRuleDefinition valueRule = new EligibilityRuleDefinition();
        valueRule.setRuleType(RuleType.MONTHLY_ORDER_VALUE);
        valueRule.setOperator(ComparisonOperator.GTE);
        valueRule.setThresholdValue(BigDecimal.valueOf(5000));
        EligibilityRuleDefinition cohortRule = new EligibilityRuleDefinition();
        cohortRule.setRuleType(RuleType.COHORT);
        cohortRule.setOperator(ComparisonOperator.EQ);
        cohortRule.setCohortCode("PREMIUM_COHORT");
        tier.setEligibilityRules(List.of(valueRule, cohortRule));
        tier.setBenefits(List.of(
                benefit(BenefitCode.FREE_DELIVERY),
                benefit(BenefitCode.EXTRA_DISCOUNT),
                benefit(BenefitCode.EXCLUSIVE_DEALS),
                benefit(BenefitCode.PRIORITY_SUPPORT)));
        return tier;
    }

    private static TierDefinition baseTier(TierCode code, int rank, int discountPercent) {
        TierDefinition tier = new TierDefinition();
        tier.setCode(code);
        tier.setDisplayName(code.name());
        tier.setRank(rank);
        tier.setActive(true);
        tier.setDiscountPercent(discountPercent);
        return tier;
    }

    private static EligibilityRuleDefinition orderCountRule(int threshold) {
        EligibilityRuleDefinition rule = new EligibilityRuleDefinition();
        rule.setRuleType(RuleType.ORDER_COUNT);
        rule.setOperator(ComparisonOperator.GTE);
        rule.setThresholdValue(BigDecimal.valueOf(threshold));
        return rule;
    }

    private static BenefitDefinition benefit(BenefitCode code) {
        BenefitDefinition benefit = new BenefitDefinition();
        benefit.setCode(code);
        benefit.setName(code.name());
        return benefit;
    }
}
