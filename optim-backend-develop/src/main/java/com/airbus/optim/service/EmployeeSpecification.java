package com.airbus.optim.service;

import com.airbus.optim.entity.*;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.CriteriaBuilder;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.util.MultiValueMap;

@Service
public class EmployeeSpecification {

    public static Specification<Employee> getSpecifications(MultiValueMap<String, String> params) {
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

                if (key.equals("employee.contractType")) {
                    predicate = addPredicateWithMultipleValues(predicate, root.get("contractType"), parsedValues, criteriaBuilder, true);

                } else if (key.equals("employee.firstName")) {
                    predicate = addPredicateWithMultipleValues(predicate, root.get("firstName"), parsedValues, criteriaBuilder, false);

                } else if (key.equals("employee.lastName")) {
                    predicate = addPredicateWithMultipleValues(predicate, root.get("lastName"), parsedValues, criteriaBuilder, false);

                } else if (key.equals("employee.job")) {
                    predicate = addPredicateWithMultipleValues(predicate, root.get("job"), parsedValues, criteriaBuilder, false);

                } else if (key.equals("hasLevers")) {
                    Join<Employee, Lever> leverJoin = root.join("levers", JoinType.LEFT);
                    predicate = criteriaBuilder.and(predicate, criteriaBuilder.isNotNull(leverJoin.get("id")));
                    query.distinct(true);

                } else if (key.startsWith("lever.")) {
                    predicate = handleJoinWithMultipleValues(root.join("lever"), key.substring("lever.".length()), parsedValues, criteriaBuilder, predicate);

                } else if (key.startsWith("siglum.")) {
                    predicate = addPredicateWithMultipleValues(predicate, root.join("siglum").get(key.substring("siglum.".length())), parsedValues, criteriaBuilder, true);

                } else if (key.startsWith("location.")) {
                    predicate = addPredicateWithMultipleValues(predicate, root.join("location").get(key.substring("location.".length())), parsedValues, criteriaBuilder, false);

                } else if (key.startsWith("costCenter.")) {
                    predicate = handleJoinWithMultipleValues(root.join("costCenter"), key.substring("costCenter.".length()), parsedValues, criteriaBuilder, predicate);

                } else if (key.startsWith("jobRequest.")) {
                    predicate = addPredicateWithMultipleValues(predicate, root.join("jobRequest").get(key.substring("jobRequest.".length())), parsedValues, criteriaBuilder, false);

                } else if (key.startsWith("employee.")) {
                    predicate = addPredicateWithMultipleValues(predicate, root.get(key.substring("employee.".length())), parsedValues, criteriaBuilder, true);

                }
            }

            return predicate;
        };
    }

    private static Predicate addPredicateWithMultipleValues(Predicate predicate, Path<String> path, List<String> values, CriteriaBuilder cb, boolean exactMatch) {
        Predicate newPredicate = cb.disjunction();
        for (String value : values) {
            if (exactMatch) {
                newPredicate = cb.or(newPredicate, cb.equal(cb.lower(path), value.toLowerCase()));
            } else {
                newPredicate = cb.or(newPredicate, cb.like(cb.lower(path), "%" + value.toLowerCase() + "%"));
            }
        }
        return cb.and(predicate, newPredicate);
    }

    private static Predicate handleJoinWithMultipleValues(Join<?, ?> join, String property, List<String> values, CriteriaBuilder cb, Predicate predicate) {
        if (property.startsWith("location.")) {
            Join<?, ?> locationJoin = join.join("location");
            String nestedProperty = property.substring("location.".length());
            return addPredicateWithMultipleValues(predicate, locationJoin.get(nestedProperty), values, cb, false);
        } else if (property.startsWith("siglumDestination.")) {
            return addPredicateWithMultipleValues(predicate, join.join("siglumDestination").get(property.substring("siglumDestination.".length())), values, cb, false);
        } else if (property.startsWith("headCount.")) {
            return addPredicateWithMultipleValues(predicate, join.join("headCount").get(property.substring("headCount.".length())), values, cb, false);
        } else if (property.startsWith("purchaseOrders.")) {
            return addPredicateWithMultipleValues(predicate, join.join("purchaseOrders").get(property.substring("purchaseOrders.".length())), values, cb, false);
        } else if (property.startsWith("workload.")) {
            if (property.startsWith("ppsids.", "workload.".length())) {
                return addPredicateWithMultipleValues(predicate, join.join("ppsids").get(property.substring("ppsids.".length())), values, cb, false);
            }
            return addPredicateWithMultipleValues(predicate, join.join("workload").get(property.substring("workload.".length())), values, cb, false);
        }
        return addPredicateWithMultipleValues(predicate, join.get(property), values, cb, false);
    }
}
