package com.innowise.order.dao.repository;

import com.innowise.order.dao.model.ItemModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ItemRepository extends JpaRepository<ItemModel, UUID> {

}
