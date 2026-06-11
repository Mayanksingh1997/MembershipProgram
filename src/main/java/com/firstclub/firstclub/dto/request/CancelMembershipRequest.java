package com.firstclub.firstclub.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CancelMembershipRequest {

    @Builder.Default
    private Boolean immediate = false;
}
