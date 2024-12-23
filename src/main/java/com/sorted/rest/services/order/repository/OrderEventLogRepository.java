package com.sorted.rest.services.order.repository;

import org.springframework.stereotype.Repository;

import com.sorted.rest.common.dbsupport.crud.BaseCrudRepository;
import com.sorted.rest.services.order.entity.OrderEventLogEntity;

/**
 * The Interface OrderEntityRepository.
 */
@Repository
public interface OrderEventLogRepository extends BaseCrudRepository<OrderEventLogEntity, Long> {

}
