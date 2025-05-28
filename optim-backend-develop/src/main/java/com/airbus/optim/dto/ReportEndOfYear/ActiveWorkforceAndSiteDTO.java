package com.airbus.optim.dto.ReportEndOfYear;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ActiveWorkforceAndSiteDTO {
    String site;
    String activeWorkforce;
    double fTE;
}
