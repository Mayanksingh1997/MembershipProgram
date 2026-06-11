package com.firstclub.firstclub.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.Instant;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class BaseResponse {

    private String status;
    private String message;

    @Builder.Default
    private Instant timestamp = Instant.now();
}
