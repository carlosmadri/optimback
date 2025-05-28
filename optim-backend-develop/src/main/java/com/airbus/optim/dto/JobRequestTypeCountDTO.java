package com.airbus.optim.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class JobRequestTypeCountDTO {
    private String type;
    private Long count;
}
