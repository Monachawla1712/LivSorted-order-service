package com.sorted.rest.services.order.repository;

import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.sorted.rest.common.dbsupport.crud.BaseCrudRepository;
import com.sorted.rest.services.order.entity.OrderItemEntity;

import java.util.UUID;

/**
 * The Interface OrderItemRepository.
 */
@Repository
public interface OrderItemRepository extends BaseCrudRepository<OrderItemEntity, UUID> {
}
