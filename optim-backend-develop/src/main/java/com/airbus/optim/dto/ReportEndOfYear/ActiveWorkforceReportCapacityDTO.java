package com.airbus.optim.dto.ReportEndOfYear;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ActiveWorkforceReportCapacityDTO {
    String report;
    double awfFTE;
    double TempFTE;
    double capacity;
}
