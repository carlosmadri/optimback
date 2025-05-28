package com.airbus.optim.dto.subcontractingDto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class SubcontractingDTO {
    Double purchaseOrders;
    Double purchaseRequest;
    Double baseline;
    SubcontractingQuarterDTO kHrsPerQuarter;
}
