package com.firstclub.firstclub.configuration.catalog;

import com.firstclub.firstclub.constants.TierCode;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class TierDefinition {

    private TierCode code;
    private String displayName;
    private int rank;
    private boolean active;
    private int discountPercent;
    private List<EligibilityRuleDefinition> eligibilityRules = new ArrayList<>();
    private List<BenefitDefinition> benefits = new ArrayList<>();
}
