package com.airbus.optim.dto.ReportEndOfYear;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class EmployeeReportDTO {
    String siglumHR;
    String siglum6;
    String siglum5;
    String siglum4;
    String country;
    String site;
    Long workerID;
    String firstName;
    String lastName;
    String job;
    String availabilityReason;
    String activeWorkforce;
    String direct;
    String contractType;
    String KAPIScode;
    Long costCenter;
    String collar; //WC/BC
    double fTE;


}
