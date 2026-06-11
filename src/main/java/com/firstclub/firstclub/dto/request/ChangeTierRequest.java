package com.firstclub.firstclub.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChangeTierRequest {

    @NotNull
    private TierAction action;

    @NotBlank
    private String targetTierCode;

    public enum TierAction {
        UPGRADE,
        DOWNGRADE
    }
}
