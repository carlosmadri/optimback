package com.airbus.optim.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class SiglumStatusRequestDTO {
    private List<String> approvedList;
    private List<String> rejectedList;
}
