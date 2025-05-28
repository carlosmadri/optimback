package com.airbus.optim.service;

import com.airbus.optim.entity.CostCenter;
import com.airbus.optim.entity.Location;
import com.airbus.optim.entity.Workload;
import com.airbus.optim.entity.Siglum;
import com.airbus.optim.entity.PPSID;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.util.MultiValueMap;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class WorkloadSpecification {

    public static Specification<Workload> getSpecifications(MultiValueMap<String, String> params) {
        return (root, query, criteriaBuilder) -> {
            Predicate predicate = criteriaBuilder.conjunction();

            for (Map.Entry<String, List<String>> entry : params.entrySet()) {
                String key = entry.getKey();
                List<String> values = entry.getValue();

                List<String> parsedValues = new ArrayList<>();
                for (String value : values) {
                    if (value.contains(",")) {
                        parsedValues.addAll(List.of(value.split(",")));
                    } else {
                        parsedValues.add(value);
                    }
                }
                //workload.khrs
                if (key.equalsIgnoreCase("workload.khrs")) {
                    predicate = criteriaBuilder.and(predicate, criteriaBuilder.equal(root.get("kHrs"), Double.parseDouble(parsedValues.get(0))));
                } else if (key.equalsIgnoreCase("workload.keur")) {
                    predicate = criteriaBuilder.and(predicate, criteriaBuilder.equal(root.get("kEur"), Double.parseDouble(parsedValues.get(0))));
                } else if (key.equalsIgnoreCase("siglum.siglumhr")) {
                    Join<Workload, Siglum> siglumJoin = root.join("siglum");
                    Predicate siglumPredicate = criteriaBuilder.disjunction();
                    for (String value : parsedValues) {
                        siglumPredicate = criteriaBuilder.or(siglumPredicate, criteriaBuilder.equal(siglumJoin.get("siglumHR"), value));
                    }
                    predicate = criteriaBuilder.and(predicate, siglumPredicate);
                } else if (key.equalsIgnoreCase("workload.description")) {
                    predicate = criteriaBuilder.and(predicate, criteriaBuilder.like(criteriaBuilder.lower(root.get("description")), "%" + parsedValues.get(0).toLowerCase() + "%"));
                } else if (key.equalsIgnoreCase("workload.own")) {
                    predicate = criteriaBuilder.and(predicate, criteriaBuilder.equal(root.get("own"), parsedValues.get(0)));
                } else if (key.equalsIgnoreCase("costcenter.location.country")) {
                    Join<Workload, CostCenter> costCenterJoin = root.join("costCenter");
                    Join<CostCenter, Location> locationJoin = costCenterJoin.join("location");
                    Predicate countryPredicate = criteriaBuilder.disjunction();
                    for (String value : parsedValues) {
                        countryPredicate = criteriaBuilder.or(countryPredicate, criteriaBuilder.equal(locationJoin.get("country"), value));
                    }
                    predicate = criteriaBuilder.and(predicate, countryPredicate);  
                } else if (key.equalsIgnoreCase("costcenter.location.site")) {
                    Join<Workload, CostCenter> costCenterJoin = root.join("costCenter");
                    Join<CostCenter, Location> locationJoin = costCenterJoin.join("location");
                    Predicate sitePredicate = criteriaBuilder.disjunction();
                    for (String value : parsedValues) {
                        sitePredicate = criteriaBuilder.or(sitePredicate, criteriaBuilder.equal(locationJoin.get("site"), value));
                    }
                    predicate = criteriaBuilder.and(predicate, sitePredicate);
                } else if (key.equalsIgnoreCase("costcenter.costcentercode")) {
                    Join<Workload, CostCenter> costCenterJoin = root.join("costCenter");
                    Predicate costCenterCodePredicate = criteriaBuilder.disjunction();
                    for (String value : parsedValues) {
                        costCenterCodePredicate = criteriaBuilder.or(costCenterCodePredicate, criteriaBuilder.equal(costCenterJoin.get("costCenterCode"), value));
                    }
                    predicate = criteriaBuilder.and(predicate, costCenterCodePredicate);
                } else if (key.equalsIgnoreCase("workload.direct")) {
                    predicate = criteriaBuilder.and(predicate, criteriaBuilder.equal(root.get("direct"), parsedValues.get(0)));
                } else if (key.equalsIgnoreCase("workload.core")) {
                    predicate = criteriaBuilder.and(predicate, criteriaBuilder.equal(root.get("core"), parsedValues.get(0)));
                } else if (key.equalsIgnoreCase("workload.collar")) {
                    predicate = criteriaBuilder.and(predicate, criteriaBuilder.equal(root.get("collar"), parsedValues.get(0)));
                } else if (key.equalsIgnoreCase("workload.fte")) {
                    predicate = criteriaBuilder.and(predicate, criteriaBuilder.equal(root.get("fTE"), Double.parseDouble(parsedValues.get(0))));
                } else if (key.equalsIgnoreCase("workload.eac")) {
                    boolean eacValue = Boolean.parseBoolean(parsedValues.get(0));
                    predicate = criteriaBuilder.and(predicate, criteriaBuilder.equal(root.get("eac"), eacValue));
                } else if (key.equalsIgnoreCase("ppsid.ppsid")) {
                    Join<Workload, PPSID> ppsidJoin2 = root.join("ppsid");
                    predicate = criteriaBuilder.and(predicate, criteriaBuilder.equal(ppsidJoin2.get("ppsid"), parsedValues.get(0)));
                } else if (key.equalsIgnoreCase("workload.scenario")) {
                    Join<Workload, PPSID> ppsidJoin = root.join("ppsid");
                    predicate = criteriaBuilder.and(predicate, criteriaBuilder.equal(ppsidJoin.get("scenario"), values.get(0)));
                } else if (key.equalsIgnoreCase("workload.efficiency")) {
                    Join<Workload, CostCenter> costCenterJoin = root.join("costCenter");
                    predicate = criteriaBuilder.and(predicate, criteriaBuilder.equal(costCenterJoin.get("efficiency"), Double.parseDouble(parsedValues.get(0))));
                } else if (key.equalsIgnoreCase("workload.rateown")) {
                    Join<Workload, CostCenter> costCenterJoin = root.join("costCenter");
                    predicate = criteriaBuilder.and(predicate, criteriaBuilder.equal(costCenterJoin.get("rateOwn"), Double.parseDouble(parsedValues.get(0))));
                }
            }

            return predicate;
        };
    }
}
