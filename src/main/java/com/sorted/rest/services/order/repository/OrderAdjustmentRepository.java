package com.sorted.rest.services.order.repository;

import com.sorted.rest.common.dbsupport.crud.BaseCrudRepository;
import com.sorted.rest.services.order.entity.OrderAdjustmentEntity;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

/**
 * The Interface OrderAdjustmentsRepository.
 */
@Repository
public interface OrderAdjustmentRepository extends BaseCrudRepository<OrderAdjustmentEntity, Integer> {

	@Modifying
	@Query("UPDATE OrderAdjustmentEntity oa SET oa.active=0 WHERE oa.txnType = :txnType AND oa.displayOrderId = :displayOrderId AND oa.status = 0 AND oa.active = 1")
	void deactivateOldAdjustment(String displayOrderId, String txnType);

}
