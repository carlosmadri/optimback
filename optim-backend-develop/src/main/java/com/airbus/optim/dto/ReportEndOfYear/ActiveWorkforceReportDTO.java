package com.airbus.optim.dto.ReportEndOfYear;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ActiveWorkforceReportDTO {
    String report;
    String activeWorkforce;
    double fTE;


}
