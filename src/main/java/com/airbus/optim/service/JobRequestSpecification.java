package com.airbus.optim.service;

import com.airbus.optim.entity.CostCenter;
import com.airbus.optim.entity.JobRequest;
import com.airbus.optim.entity.Location;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.util.MultiValueMap;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class JobRequestSpecification {

    public static Specification<JobRequest> getSpecifications(MultiValueMap<String, String> params) {
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

                if (key.equalsIgnoreCase("jobRequest.startDate")) {
                    predicate = addDatePredicate(predicate, root.get("startDate"), parsedValues, criteriaBuilder);

                } else if (key.toLowerCase().startsWith("jobrequest.")) {
                    predicate = addPredicateWithMultipleValues(predicate, root.get(key.substring("jobRequest.".length())), parsedValues, criteriaBuilder, true);

                } else if (key.startsWith("siglum.")) {
                    predicate = addPredicateWithMultipleValues(predicate, root.join("siglum").get(key.substring("siglum.".length())), parsedValues, criteriaBuilder, true);

                } else if (key.startsWith("location.")) {
                    Join<CostCenter, Location> locationJoin = root.join("costCenter").join("location");
                    predicate = addPredicateWithMultipleValues(predicate, locationJoin.get(key.substring("location.".length())), parsedValues, criteriaBuilder, false);

                } else if (key.startsWith("costCenter.")) {
                    predicate = handleJoinWithMultipleValues(root.join("costCenter"), key.substring("costCenter.".length()), parsedValues, criteriaBuilder, predicate);
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

    private static Predicate addDatePredicate(Predicate predicate, Path<Instant> path, List<String> values, CriteriaBuilder cb) {
        Predicate newPredicate = cb.disjunction();
        for (String value : values) {
            try {
                Instant instantValue = Instant.parse(value);
                newPredicate = cb.or(newPredicate, cb.equal(path, instantValue));
            } catch (DateTimeParseException e) {
                System.err.println("Formato de fecha inválido: " + value);
            }
        }
        return cb.and(predicate, newPredicate);
    }

    private static Predicate handleJoinWithMultipleValues(Join<?, ?> join, String property, List<String> values, CriteriaBuilder cb, Predicate predicate) {
        if (property.startsWith("location.")) {
            return addPredicateWithMultipleValues(predicate, join.join("location").get(property.substring("location.".length())), values, cb, false);
        }
        return addPredicateWithMultipleValues(predicate, join.get(property), values, cb, false);
    }
}