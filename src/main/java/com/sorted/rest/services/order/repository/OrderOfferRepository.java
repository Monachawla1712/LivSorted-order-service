package com.sorted.rest.services.order.repository;

import com.sorted.rest.common.dbsupport.crud.BaseCrudRepository;
import com.sorted.rest.services.order.entity.OrderOfferEntity;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.sql.Date;
import java.util.List;

@Repository
public interface OrderOfferRepository extends BaseCrudRepository<OrderOfferEntity, Integer> {

	@Query("SELECT o FROM OrderOfferEntity o WHERE o.active = 1 AND (o.validTill IS NULL OR o.validTill > :currentDate)")
	List<OrderOfferEntity> getValidOrderOffers(Date currentDate);
}
