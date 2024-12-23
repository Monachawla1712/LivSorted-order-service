package com.sorted.rest.services.order.repository;

import com.sorted.rest.common.dbsupport.crud.BaseCrudRepository;
import com.sorted.rest.services.order.entity.FranchiseOrderItemLogEntity;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface FranchiseOrderItemLogRepository extends BaseCrudRepository<FranchiseOrderItemLogEntity, Integer> {

}
