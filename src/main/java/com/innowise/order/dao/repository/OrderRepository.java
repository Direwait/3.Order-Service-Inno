package com.innowise.order.dao.repository;

import com.innowise.order.dao.model.OrderModel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;
import java.util.UUID;

@Repository
public interface OrderRepository extends JpaRepository<OrderModel, UUID>, JpaSpecificationExecutor<OrderModel> {
    Page<OrderModel> findAllByUserId(UUID userId, Pageable pageable);
}
