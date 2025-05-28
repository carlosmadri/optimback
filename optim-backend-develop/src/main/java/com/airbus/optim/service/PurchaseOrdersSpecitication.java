package com.airbus.optim.service;

import com.airbus.optim.entity.CostCenter;
import com.airbus.optim.entity.Location;
import com.airbus.optim.entity.PurchaseOrders;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.util.MultiValueMap;

@Service
public class PurchaseOrdersSpecitication {

    public static Specification<PurchaseOrders> getSpecifications(MultiValueMap<String, String> params) {
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

                if (key.startsWith("siglum.")) {
                    predicate = addPredicateWithMultipleValues(predicate, root.join("siglum").get(key.substring("siglum.".length())), parsedValues, criteriaBuilder, true);

                } else if (key.startsWith("subcontracting.siglum")) {
                    predicate = addPredicateWithMultipleValues(predicate, root.join("siglum").get("siglumHR"), parsedValues, criteriaBuilder, true);

                } else if (key.startsWith("subcontracting.site")) {
                    predicate = addPredicateWithMultipleValues(predicate, root.join("locations").get("site"), parsedValues, criteriaBuilder, true);

                } else if (key.startsWith("subcontracting.description")) {
                    predicate = addPredicateWithMultipleValues(predicate, root.get("description"), parsedValues, criteriaBuilder, true);

                } else if (key.startsWith("subcontracting.provider")) {
                    predicate = addPredicateWithMultipleValues(predicate, root.get("provider"), parsedValues, criteriaBuilder, true);

                } else if (key.startsWith("subcontracting.approved")) {
                    predicate = addPredicateWithMultipleValues(predicate, root.get("approved"), parsedValues, criteriaBuilder, true);

                } else if (key.startsWith("subcontracting.quarter")) {
                    predicate = addPredicateWithMultipleValues(predicate, root.get("quarter"), parsedValues, criteriaBuilder, true);

                } else if (key.startsWith("subcontracting.year")) {
                    predicate = addPredicateWithMultipleValues(predicate, root.get("year"), parsedValues, criteriaBuilder, true);

                } else if (key.startsWith("subcontracting.orderRequest")) {
                    predicate = addPredicateWithMultipleValues(predicate, root.get("orderRequest"), parsedValues, criteriaBuilder, true);

                } else if (key.startsWith("subcontracting.hmg")) {
                    predicate = addPredicateWithMultipleValues(predicate, root.get("hmg"), parsedValues, criteriaBuilder, true);

                } else if (key.startsWith("subcontracting.pep")) {
                    predicate = addPredicateWithMultipleValues(predicate, root.get("pep"), parsedValues, criteriaBuilder, true);

                } else if (key.startsWith("subcontracting.keur")) {
                    predicate = addPredicateWithMultipleValues(predicate, root.get("kEur"), parsedValues, criteriaBuilder, true);

                } else if (key.startsWith("subcontracting.orderId")) {
                    predicate = addPredicateWithMultipleValues(predicate, root.get("id"), parsedValues, criteriaBuilder, true);

                } else if (key.startsWith("subcontracting.location.")) {
                    predicate = addPredicateWithMultipleValues(predicate, root.join("location").get(key.substring("location.".length())), parsedValues, criteriaBuilder, true);

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
