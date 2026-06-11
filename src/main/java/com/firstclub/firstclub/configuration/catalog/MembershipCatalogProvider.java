package com.firstclub.firstclub.configuration.catalog;

import com.firstclub.firstclub.constants.PlanCode;
import com.firstclub.firstclub.constants.TierCode;
import com.firstclub.firstclub.exception.MembershipException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class MembershipCatalogProvider {

    private final MembershipCatalogProperties catalogProperties;

    public MembershipCatalogProvider(MembershipCatalogProperties catalogProperties) {
        this.catalogProperties = catalogProperties;
    }

    public List<PlanDefinition> getActivePlans() {
        List<PlanDefinition> activePlans = new ArrayList<>();
        for (PlanDefinition plan : catalogProperties.getPlans()) {
            if (plan.isActive()) {
                activePlans.add(plan);
            }
        }
        sortPlansByPrice(activePlans);
        return activePlans;
    }

    public PlanCode resolvePlanCode(String planCode) {
        try {
            return PlanCode.valueOf(planCode.trim());
        } catch (IllegalArgumentException ex) {
            throw new MembershipException(
                    "Plan not found: " + planCode,
                    HttpStatus.NOT_FOUND,
                    "PLAN_NOT_FOUND");
        }
    }

    public TierCode resolveTierCode(String tierCode) {
        try {
            return TierCode.valueOf(tierCode.trim());
        } catch (IllegalArgumentException ex) {
            throw new MembershipException(
                    "Tier not found: " + tierCode,
                    HttpStatus.NOT_FOUND,
                    "TIER_NOT_FOUND");
        }
    }

    public PlanDefinition getActivePlan(PlanCode planCode) {
        for (PlanDefinition plan : catalogProperties.getPlans()) {
            if (plan.isActive() && plan.getCode() == planCode) {
                return plan;
            }
        }
        throw new MembershipException(
                "Plan not found: " + planCode,
                HttpStatus.NOT_FOUND,
                "PLAN_NOT_FOUND");
    }

    public List<TierDefinition> getActiveTiers() {
        List<TierDefinition> activeTiers = new ArrayList<>();
        for (TierDefinition tier : catalogProperties.getTiers()) {
            if (tier.isActive()) {
                activeTiers.add(tier);
            }
        }
        sortTiersByRank(activeTiers);
        return activeTiers;
    }

    public TierDefinition getActiveTier(TierCode tierCode) {
        for (TierDefinition tier : catalogProperties.getTiers()) {
            if (tier.isActive() && tier.getCode() == tierCode) {
                return tier;
            }
        }
        throw new MembershipException(
                "Tier not found: " + tierCode,
                HttpStatus.NOT_FOUND,
                "TIER_NOT_FOUND");
    }

    private void sortPlansByPrice(List<PlanDefinition> plans) {
        for (int i = 0; i < plans.size(); i++) {
            for (int j = i + 1; j < plans.size(); j++) {
                if (plans.get(i).getPrice().compareTo(plans.get(j).getPrice()) > 0) {
                    PlanDefinition temp = plans.get(i);
                    plans.set(i, plans.get(j));
                    plans.set(j, temp);
                }
            }
        }
    }

    private void sortTiersByRank(List<TierDefinition> tiers) {
        for (int i = 0; i < tiers.size(); i++) {
            for (int j = i + 1; j < tiers.size(); j++) {
                if (tiers.get(i).getRank() > tiers.get(j).getRank()) {
                    TierDefinition temp = tiers.get(i);
                    tiers.set(i, tiers.get(j));
                    tiers.set(j, temp);
                }
            }
        }
    }
}
