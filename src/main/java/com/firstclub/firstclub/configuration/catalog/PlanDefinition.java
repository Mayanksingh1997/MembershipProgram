package com.firstclub.firstclub.configuration.catalog;

import com.firstclub.firstclub.constants.PlanCode;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class PlanDefinition {

    private PlanCode code;
    private String name;
    private BigDecimal price;
    private int durationDays;
    private boolean active;
}
