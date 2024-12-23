package com.sorted.rest.services.order.repository;

import com.sorted.rest.common.dbsupport.crud.BaseCrudRepository;
import com.sorted.rest.services.order.entity.FranchiseOrderItemEntity;
import com.sorted.rest.services.order.entity.SkusPurchaseCostEntity;
import org.springframework.stereotype.Repository;

import java.sql.Date;
import java.util.List;
import java.util.UUID;

@Repository
public interface SkusPurchaseCostRepository extends BaseCrudRepository<SkusPurchaseCostEntity, Long> {

	List<SkusPurchaseCostEntity> findByDeliveryDate(Date deliveryDate);
}
