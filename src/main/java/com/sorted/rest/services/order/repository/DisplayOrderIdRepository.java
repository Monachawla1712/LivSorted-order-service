package com.sorted.rest.services.order.repository;

import com.sorted.rest.common.dbsupport.crud.BaseCrudRepository;
import com.sorted.rest.services.order.entity.DisplayOrderIdEntity;

import java.util.List;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * The Interface DisplayOrderIdRepository.
 */
@Repository
public interface DisplayOrderIdRepository extends BaseCrudRepository<DisplayOrderIdEntity, String> {

	@Modifying
	@Transactional
	@Query("UPDATE DisplayOrderIdEntity d SET d.active = 0 WHERE d.displayOrderId = :displayOrderId AND d.active = 1")
	Integer inactivateDisplayOrderId(String displayOrderId);

	@Modifying
	@Transactional
	@Query(value = "WITH cte AS (select display_order_id from oms_trans_.display_order_ids where active=1 limit :limit) update oms_trans_.display_order_ids d set active = 0 from cte where d.display_order_id=cte.display_order_id and active=1 returning d.display_order_id", nativeQuery = true)
	List<String> getBulkDisplayOrderIds(Integer limit);

}
