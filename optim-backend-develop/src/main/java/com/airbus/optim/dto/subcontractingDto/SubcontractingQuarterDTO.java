package com.airbus.optim.dto.subcontractingDto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class SubcontractingQuarterDTO {
    List<QuartersDTO> PurchaseAproved;
    List<QuartersDTO> PurchaseNotAproved;

}