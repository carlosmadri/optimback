package com.airbus.optim.service.EmployeeImpl;

import com.airbus.optim.entity.Employee;
import com.airbus.optim.entity.Lever;
import com.airbus.optim.utils.Constants;
import java.time.LocalDate;
import java.time.Month;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.util.MultiValueMap;

@Service
public class EmployeeWorkforceReport {

    public String getEndOfYearWorkforce(
            MultiValueMap<String, String> params,
            List<Employee> activeWorkforceReportList,
            StringBuilder csvContent) {

        List<String> filterParams = List.of(
                "SiglumHR","Siglum6","Siglum5","Siglum4","Country","Site","WorkerID",
                "ActiveWorkforce","KAPIS code","WC/BC","LastName", "FirstName","Job","AvailabilityReason",
                "Direct","ContractType","CostCenter","Actual","EndOfYear");

        List<String> paramsFiltered = new ArrayList<>();
        for (String key : params.get("fields")) {
            for (String filter : filterParams) {
                if (filter.equals(key)) {
                    paramsFiltered.add(filter);
                }
            }
        }
        paramsFiltered.add("EndOfYear");

        for (Employee e : activeWorkforceReportList) {
            csvContent.append("\"");

            // INI: valores filtros
            if (existsIn(filterParams.get(0), paramsFiltered))
                csvContent.append(e.getSiglum().getSiglumHR()).append("\",\"");
            if (existsIn(filterParams.get(1), paramsFiltered))
                csvContent.append(e.getSiglum().getSiglum6()).append("\",\"");
            if (existsIn(filterParams.get(2), paramsFiltered))
                csvContent.append(e.getSiglum().getSiglum5()).append("\",\"");
            if (existsIn(filterParams.get(3), paramsFiltered))
                csvContent.append(e.getSiglum().getSiglum4()).append("\",\"");
            if (existsIn(filterParams.get(4), paramsFiltered))
                csvContent.append((e.getCostCenter() != null && e.getCostCenter().getLocation() != null)
                                ? e.getCostCenter().getLocation().getCountry()
                                : null).append("\",\"");
            if (existsIn(filterParams.get(5), paramsFiltered))
                csvContent.append((e.getCostCenter() != null && e.getCostCenter().getLocation() != null)
                        ? e.getCostCenter().getLocation().getSite()
                        : null).append("\",\"");
            if (existsIn(filterParams.get(6), paramsFiltered))
                csvContent.append(e.getId()).append("\",\"");
            if (existsIn(filterParams.get(7), paramsFiltered))
                csvContent.append(e.getActiveWorkforce()).append("\",\"");
            if (existsIn(filterParams.get(8), paramsFiltered))
                csvContent.append((e.getCostCenter() != null && e.getCostCenter().getLocation() != null)
                        ? e.getCostCenter().getLocation().getKapisCode()
                        : null).append("\",\"");
            if (existsIn(filterParams.get(9), paramsFiltered))
                csvContent.append(e.getCollar()).append("\",\"");
            if (existsIn(filterParams.get(10), paramsFiltered))
                csvContent.append(e.getLastName()).append("\",\"");
            if (existsIn(filterParams.get(11), paramsFiltered))
                csvContent.append(e.getFirstName()).append("\",\"");
            if (existsIn(filterParams.get(12), paramsFiltered))
                csvContent.append(e.getJob()).append("\",\"");
            if (existsIn(filterParams.get(13), paramsFiltered))
                csvContent.append(e.getAvailabilityReason()).append("\",\"");
            if (existsIn(filterParams.get(14), paramsFiltered))
                csvContent.append(e.getDirect()).append("\",\"");
            if (existsIn(filterParams.get(15), paramsFiltered))
                csvContent.append(e.getContractType()).append("\",\"");
            if (existsIn(filterParams.get(16), paramsFiltered))
                csvContent.append((e.getCostCenter() != null)
                        ? e.getCostCenter().getLocation().getKapisCode()
                        : null).append("\",\"");
            // FIN: valores filtros


            // INI: fte por meses
            List<Lever> leversForMonth = new ArrayList<>();
            int yearFilter = Integer.parseInt(params.get("yearFilter").get(0));
            if(Constants.REPORTS_MONTHLY.equals(params.get("projection").get(0))) {
                for (int i=1; i<=12; i++) {
                    leversForMonth = getLeversByMonth(e, yearFilter, i);

                    double fte = leversForMonth.stream()
                            .mapToDouble(Lever::getFTE)
                            .sum();

                    csvContent.append(fte+e.getFTE()).append("\",\"");
                }
            }
            // FIN: fte por meses


            // INI: Actual (suma fte mes actual)
            else {
                LocalDate currentDate = LocalDate.now();
                leversForMonth = getLeversByMonth(e, yearFilter, currentDate.getMonthValue());
                double fte = leversForMonth.stream()
                        .mapToDouble(Lever::getFTE)
                        .sum();
                csvContent.append(fte + e.getFTE()).append("\",\"");
            }
            // FIN: Actual (suma fte mes actual)


            // INI: EndOfYear (suma fte todo el año)
            double endOfYear = e.getLevers().stream()
                    .mapToDouble(Lever::getFTE)
                    .sum();
            csvContent.append(e.getFTE()+endOfYear).append("\"\n");
            // FIN: EndOfYear (suma fte todo el año)

        }

        return csvContent.toString();
    }

    public List<Lever> getLeversByMonth(Employee e, int yearFilter, int monthNum) {
        final Month month = Month.of(monthNum);
        return e.getLevers().stream()
                        .filter(lever -> lever.getStartDate() != null && lever.getEndDate() != null)
                        .filter(lever -> {
                            LocalDate startDate = lever.getStartDate().atZone(ZoneId.systemDefault()).toLocalDate();
                            LocalDate endDate = lever.getEndDate().atZone(ZoneId.systemDefault()).toLocalDate();
                            return (startDate.getYear() <= yearFilter && endDate.getYear() >= yearFilter &&
                                    (startDate.getMonthValue() <= month.getValue() || startDate.getYear() < yearFilter) &&
                                    (endDate.getMonthValue() >= month.getValue() || endDate.getYear() > yearFilter));
                        })
                        .toList();
    }

    public boolean existsIn(String filterParam, List<String> paramsFiltered) {
        for (String str : paramsFiltered) {
            if(filterParam.equals(str))
                return true;
        }
        return false;
    }
}
