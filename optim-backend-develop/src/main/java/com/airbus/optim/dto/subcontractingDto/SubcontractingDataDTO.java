package com.airbus.optim.dto.subcontractingDto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class SubcontractingDataDTO {
    private String siglum;
    private String site;
    private String description;
    private String provider;
    private String approved;
    private String quarter;
    private String year;
    private Double kEur;
    
    private String orderRequest;
    private Long OrderId;
    private String hmg;
    private String pep;

}
