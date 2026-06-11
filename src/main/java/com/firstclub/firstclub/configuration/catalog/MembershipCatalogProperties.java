package com.firstclub.firstclub.configuration.catalog;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

@Data
@ConfigurationProperties(prefix = "membership.catalog")
public class MembershipCatalogProperties {

    private List<PlanDefinition> plans = new ArrayList<>();
    private List<TierDefinition> tiers = new ArrayList<>();
}
