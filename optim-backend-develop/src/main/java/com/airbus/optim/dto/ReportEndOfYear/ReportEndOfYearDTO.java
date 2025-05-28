package com.airbus.optim.dto.ReportEndOfYear;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ReportEndOfYearDTO {
    List<ActiveWorkforceReportCapacityDTO> actuals;
    List<ActiveWorkforceReportCapacityDTO> realistic;
    List<ActiveWorkforceReportCapacityDTO> validation;
    List<ActiveWorkforceReportCapacityDTO> optimistic;
}
