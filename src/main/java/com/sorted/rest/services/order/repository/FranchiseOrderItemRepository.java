package com.sorted.rest.services.order.repository;

import com.sorted.rest.common.dbsupport.crud.BaseCrudRepository;
import com.sorted.rest.services.order.entity.FranchiseOrderItemEntity;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface FranchiseOrderItemRepository extends BaseCrudRepository<FranchiseOrderItemEntity, UUID> {
}
