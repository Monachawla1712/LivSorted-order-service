package com.sorted.rest.services.order.repository;

import com.sorted.rest.common.dbsupport.crud.BaseCrudRepository;
import com.sorted.rest.services.order.constants.OrderConstants;
import com.sorted.rest.services.order.entity.RepeatOrderEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.sql.Date;
import java.util.List;
import java.util.UUID;

@Repository
public interface RepeatOrderRepository extends BaseCrudRepository<RepeatOrderEntity, Long> {

	@Query("SELECT r FROM RepeatOrderEntity r WHERE r.customerId = :customerId AND r.skuCode = :skuCode AND r.active = 1")
	RepeatOrderEntity findByCustomerIdAndSkuCode(UUID customerId, String skuCode);

	@Query("SELECT r FROM RepeatOrderEntity r WHERE r.customerId = :customerId AND r.active = 1")
	List<RepeatOrderEntity> findByCustomerId(UUID customerId);

	@Query("SELECT r FROM RepeatOrderEntity r WHERE r.nextDeliveryDate = :nextDeliveryDate AND r.status = :status AND r.active = 1")
	Page<RepeatOrderEntity> findByNextDeliveryDateAndStatus(@Param("nextDeliveryDate") Date nextDeliveryDate,
			@Param("status") OrderConstants.RepeatOrderStatus status, Pageable pageable);
}
