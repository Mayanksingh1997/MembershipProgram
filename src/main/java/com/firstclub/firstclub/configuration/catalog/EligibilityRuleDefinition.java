package com.firstclub.firstclub.configuration.catalog;

import com.firstclub.firstclub.constants.ComparisonOperator;
import com.firstclub.firstclub.constants.RuleType;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class EligibilityRuleDefinition {

    private RuleType ruleType;
    private ComparisonOperator operator;
    private BigDecimal thresholdValue;
    private String cohortCode;
}
