package com.firstclub.firstclub.configuration.catalog;

import com.firstclub.firstclub.constants.BenefitCode;
import lombok.Data;

@Data
public class BenefitDefinition {

    private BenefitCode code;
    private String name;
}
