package com.innowise.order.dao.repository.specification;

import com.innowise.order.dao.enums.Status;
import com.innowise.order.dao.model.OrderModel;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class OrderSpecification {

    public static Specification<OrderModel> filterByDateRangeAndStatus(
            LocalDate startDate,
            LocalDate endDate,
            List<Status> statuses) {

        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (startDate != null && endDate != null) {
                LocalDateTime start = startDate.atStartOfDay();
                LocalDateTime end = endDate.atTime(23, 59, 59);
                predicates.add(cb.between(root.get("createdAt"), start, end));
            } else if (startDate != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), startDate.atStartOfDay()));
            } else if (endDate != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), endDate.atTime(23, 59, 59)));
            }

            if (statuses != null && !statuses.isEmpty()) {
                CriteriaBuilder.In<Status> inClause = cb.in(root.get("status"));
                for (Status status : statuses) {
                    inClause.value(status);
                }
                predicates.add(inClause);
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}