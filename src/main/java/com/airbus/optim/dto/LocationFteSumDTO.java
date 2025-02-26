package com.airbus.optim.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class LocationFteSumDTO {
    private String country;
    private String site;
    private double fteSum;
    private double longitude;
    private double latitude;
}
