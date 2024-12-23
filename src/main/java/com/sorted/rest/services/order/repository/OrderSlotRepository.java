package com.sorted.rest.services.order.repository;

import com.sorted.rest.common.dbsupport.crud.BaseCrudRepository;
import com.sorted.rest.services.order.entity.OrderSlotEntity;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public interface OrderSlotRepository extends BaseCrudRepository<OrderSlotEntity, Integer> {
	@Modifying
	@Transactional
	@Query("UPDATE OrderSlotEntity os SET os.remainingCount = os.remainingCount - 1 WHERE os.id=:slotId AND os.active = 1")
	void reserveOrderSlot(Integer slotId);

	@Modifying
	@Transactional
	@Query("UPDATE OrderSlotEntity os SET os.remainingCount = os.remainingCount + 1 WHERE os.id=:slotId AND os.active = 1")
	void releaseOrderSlot(Integer slotId);
}
