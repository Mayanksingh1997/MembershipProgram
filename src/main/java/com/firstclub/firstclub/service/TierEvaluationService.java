package com.firstclub.firstclub.service;

import com.firstclub.firstclub.configuration.catalog.EligibilityRuleDefinition;
import com.firstclub.firstclub.configuration.catalog.MembershipCatalogProvider;
import com.firstclub.firstclub.configuration.catalog.TierDefinition;
import com.firstclub.firstclub.constants.TierCode;
import com.firstclub.firstclub.domain.factory.MembershipDomainFactory;
import com.firstclub.firstclub.domain.tier.TierEligibilityEvaluator;
import com.firstclub.firstclub.domain.tier.TierEvaluationContext;
import com.firstclub.firstclub.entity.UserAccount;
import com.firstclub.firstclub.entity.UserCohort;
import com.firstclub.firstclub.entity.UserOrderAggregate;
import com.firstclub.firstclub.exception.MembershipException;
import com.firstclub.firstclub.repository.UserCohortRepository;
import com.firstclub.firstclub.repository.UserOrderAggregateRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class TierEvaluationService {

    private final MembershipCatalogProvider catalogProvider;
    private final UserOrderAggregateRepository orderAggregateRepository;
    private final UserCohortRepository cohortRepository;
    private final MembershipDomainFactory domainFactory;

    public TierEvaluationService(
            MembershipCatalogProvider catalogProvider,
            UserOrderAggregateRepository orderAggregateRepository,
            UserCohortRepository cohortRepository,
            MembershipDomainFactory domainFactory) {
        this.catalogProvider = catalogProvider;
        this.orderAggregateRepository = orderAggregateRepository;
        this.cohortRepository = cohortRepository;
        this.domainFactory = domainFactory;
    }

    public TierEvaluationContext buildContext(UserAccount user) {
        UserOrderAggregate aggregate = orderAggregateRepository.findByUserId(user.getId()).orElse(null);
        int totalOrders = 0;
        java.math.BigDecimal monthlyOrderValue = java.math.BigDecimal.ZERO;
        if (aggregate != null) {
            totalOrders = aggregate.getTotalOrders();
            monthlyOrderValue = aggregate.getMonthlyOrderValue();
        }

        List<UserCohort> cohorts = cohortRepository.findByUserId(user.getId());
        List<String> cohortCodes = new ArrayList<>();
        for (UserCohort cohort : cohorts) {
            cohortCodes.add(cohort.getCohortCode());
        }

        return TierEvaluationContext.builder()
                .totalOrders(totalOrders)
                .monthlyOrderValue(monthlyOrderValue)
                .cohortCodes(cohortCodes)
                .build();
    }

    public TierDefinition determineEligibleTier(TierEvaluationContext context) {
        List<TierDefinition> tiers = catalogProvider.getActiveTiers();
        TierDefinition highestEligibleTier = null;

        for (int index = tiers.size() - 1; index >= 0; index--) {
            TierDefinition tier = tiers.get(index);
            if (isTierEligible(tier, context)) {
                highestEligibleTier = tier;
                break;
            }
        }

        if (highestEligibleTier != null) {
            return highestEligibleTier;
        }
        return catalogProvider.getActiveTier(TierCode.SILVER);
    }

    public void validateTierEligibility(UserAccount user, TierCode targetTierCode) {
        TierDefinition targetTier = catalogProvider.getActiveTier(targetTierCode);
        TierEvaluationContext context = buildContext(user);
        if (!isTierEligible(targetTier, context)) {
            throw new MembershipException(
                    "User is not eligible for tier: " + targetTierCode,
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "INELIGIBLE_TIER");
        }
    }

    private boolean isTierEligible(TierDefinition tier, TierEvaluationContext context) {
        List<EligibilityRuleDefinition> rules = tier.getEligibilityRules();
        if (rules.isEmpty()) {
            return false;
        }

        for (EligibilityRuleDefinition rule : rules) {
            TierEligibilityEvaluator evaluator = domainFactory.createTierEvaluator(rule.getRuleType());
            if (!evaluator.evaluate(rule, context)) {
                return false;
            }
        }
        return true;
    }
}
