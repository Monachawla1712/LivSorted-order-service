package com.sorted.rest.services.order.repository;

import com.sorted.rest.common.dbsupport.crud.BaseCrudRepository;
import com.sorted.rest.services.order.beans.StoreOrderInfo;
import com.sorted.rest.services.order.beans.StoreRevenueBean;
import com.sorted.rest.services.order.beans.StoreSkuOrderCount;
import com.sorted.rest.services.order.constants.FranchiseOrderConstants.FranchiseOrderStatus;
import com.sorted.rest.services.order.entity.FranchiseOrderEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface FranchiseOrderRepository extends BaseCrudRepository<FranchiseOrderEntity, UUID> {

	@Query(value = FranchiseOrderEntity.Query.GET_STORE_CURRENT_CART)
	List<FranchiseOrderEntity> findStoreCurrentCart(@Param(value = "storeId") String storeId);

	@Query("select m from FranchiseOrderEntity m WHERE m.deliveryDate >= :fromDate and m.storeId = :storeId and m.status > 0 and m.status <> 5 and m.status <> 6 and m.active = 1 order by m.deliveryDate DESC")
	List<FranchiseOrderEntity> findOrdersWithDateAfter(String storeId, Date fromDate);

	@Query("select s from FranchiseOrderEntity s WHERE s.status = 0 and s.active = 1")
	Optional<List<FranchiseOrderEntity>> findAllCartWithActiveState();

	@Query("select s from FranchiseOrderEntity s WHERE s.status = 1 and s.active = 1 and s.isSrUploaded = 0")
	Optional<List<FranchiseOrderEntity>> findAllCartWithActiveStateAndSrNotUploaded();

	FranchiseOrderEntity findByStoreIdAndDeliveryDateAndActive(String storeId, Date date, Integer active);

	@Query("select m from FranchiseOrderEntity m WHERE m.parentOrderId = :parentOrderId and (m.status = 5 or m.status = 6) and m.active = 1")
	List<FranchiseOrderEntity> findRefundInitiatedOrdersWithParentOrderId(UUID parentOrderId);

	@Query("select m from FranchiseOrderEntity m WHERE m.parentOrderId = :parentOrderId and  m.status = 6 and m.active = 1")
	List<FranchiseOrderEntity> findRefundConfirmedOrdersWithParentOrderId(UUID parentOrderId);

	FranchiseOrderEntity findByStoreIdAndDeliveryDateAndActiveAndSlot(String storeId, Date date, Integer active, String slot);

	FranchiseOrderEntity findByDisplayOrderId(String id);

	@Query("select m from FranchiseOrderEntity m WHERE m.displayOrderId in :displayOrderIdList and m.active = 1")
	List<FranchiseOrderEntity> findOrdersByDisplayOrderId(List<String> displayOrderIdList);

	@Query(value = "SELECT o.storeId, COUNT(o) FROM FranchiseOrderEntity o WHERE o.active = 1 AND o.status > 0 AND o.status <> 7 AND o.storeId IN :storeIds GROUP BY o.storeId")
	List<Object[]> getOrderCountMap(List<String> storeIds);

	@Query(value = "SELECT count(o) FROM FranchiseOrderEntity o WHERE o.active = 1 AND (o.status = 2 or o.status = 4) AND o.storeId = :storeId")
	Long getDeliveredOrderCount(String storeId);

	@Query("select m from FranchiseOrderEntity m WHERE m.deliveryDate = :fromDate and m.storeId = :storeId AND (m.status = 2 or m.status = 4) and m.active = 1")
	List<FranchiseOrderEntity> findOrdersForToday(String storeId, Date fromDate);

	@Query("select m from FranchiseOrderEntity m WHERE m.storeId = :storeId AND (m.status = 2 or m.status = 4) and m.active = 1 ORDER BY m.createdAt asc")
	List<FranchiseOrderEntity> findFirstOrderByStoreId(String storeId, Pageable pageable);

	@Query(value = "SELECT o.id FROM FranchiseOrderEntity o WHERE o.active = 1 AND o.status in :statuses AND o.storeId = :storeId AND o.deliveryDate = :deliveryDate AND o.slot in :slots")
	UUID getDeliveredOrderByStoreIdAndDateAndSlotIn(List<FranchiseOrderStatus> statuses, String storeId, Date deliveryDate, List<String> slots);

	@Query("select o from FranchiseOrderEntity o WHERE o.active = 1 AND o.status in :statuses AND o.storeId = :storeId AND o.deliveryDate >= :fromDate AND o.slot in :slots order by o.deliveryDate DESC")
	List<FranchiseOrderEntity> findOrdersForTicketsWithDateAfter(List<FranchiseOrderStatus> statuses, String storeId, Date fromDate, List<String> slots);

	@Query(value = "select exists(select 1 from oms_trans_.franchise_orders where key = ?1)", nativeQuery = true)
	Boolean keyExists(String key);

	@Query(value = "SELECT new com.sorted.rest.services.order.beans.StoreRevenueBean(fo.storeId, sum(foi.mrpGrossAmount)) from FranchiseOrderEntity fo join FranchiseOrderItemEntity foi on fo.id= foi.orderId where fo.deliveryDate >= :startDate and fo.status in (:statuses) and fo.active = 1 and foi.active =1 and cast(fo.storeId as int) > 20000 group by fo.storeId")
	List<StoreRevenueBean> fetchStoreRevenue(Date startDate, List<FranchiseOrderStatus> statuses);

	@Query(value = "SELECT new com.sorted.rest.services.order.beans.StoreRevenueBean(fo.storeId, sum(foi.mrpGrossAmount)) from FranchiseOrderEntity fo join FranchiseOrderItemEntity foi on fo.id= foi.orderId where fo.deliveryDate >= :startDate and fo.status in (:statuses) and fo.storeId in (:storeIds) and fo.active = 1 and foi.active =1 and cast(fo.storeId as int) > 20000 group by fo.storeId")
	List<StoreRevenueBean> fetchStoreRevenueByStoreIds(Date startDate, List<FranchiseOrderStatus> statuses, List<String> storeIds);

	@Query(value = "SELECT new com.sorted.rest.services.order.beans.StoreSkuOrderCount(fo.storeId, foi.skuCode, sum(foi.orderedCrateQty)) from FranchiseOrderEntity fo join FranchiseOrderItemEntity foi on fo.id = foi.orderId where fo.deliveryDate = :deliveryDate and fo.slot = :slot  and fo.active = 1  and foi.active = 1 and fo.storeId in (:storeIds) group by fo.storeId, foi.skuCode")
	List<StoreSkuOrderCount> fetchStoreLevelSkuOrderCount(List<String> storeIds, Date deliveryDate, String slot);

	@Query("SELECT new com.sorted.rest.services.order.beans.StoreOrderInfo(o.storeId, count(o), max(o.deliveryDate)) from FranchiseOrderEntity o WHERE o.active = 1 AND o.status > 0 AND o.status <> 7 AND o.status <> 10 AND o.storeId in (:storeIds) group by o.storeId")
	List<StoreOrderInfo> findActiveOrderCountByStoreId(List<String> storeIds);
}
