package com.airbus.optim.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SiglumStatusResponseDTO {
    private List<String> pendingList;
    private List<String> approvedList;
    private List<String> rejectedList;
}
