package com.sorted.rest.services.order.repository;

import com.sorted.rest.common.dbsupport.crud.BaseCrudRepository;
import com.sorted.rest.services.order.beans.*;
import com.sorted.rest.services.order.entity.OrderEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * The Interface OrderRepository.
 */
@Repository
public interface OrderRepository extends BaseCrudRepository<OrderEntity, UUID> {

	@Query(value = OrderEntity.Query.GET_CUSTOMER_CURRENT_CART)
	List<OrderEntity> findCustomerCurrentCart(@Param(value = "customerId") UUID customerId, java.sql.Date deliveryDate);

	@Query(value = OrderEntity.Query.GET_CUSTOMER_ORDER_LIST)
	List<OrderEntity> findCustomerOrderList(@Param(value = "customerId") UUID customerId);

	Optional<OrderEntity> findByDisplayOrderId(String displayOrderId);

	@Query(value = "SELECT count(distinct deliveryDate) FROM OrderEntity o WHERE o.active = 1 AND o.status = 8 AND o.channel in (:validChannelList)  AND o.customerId = :customerId")
	Long getDeliveredOrderCount(UUID customerId, List<String> validChannelList);

	@Query("SELECT new com.sorted.rest.services.order.beans.StoreOrderTotalInfo(o.storeId, count(o), sum(o.finalBillAmount)) from OrderEntity o WHERE o.active = 1 AND o.status = 8 AND o.storeId in (:storeIds) AND o.deliveryDate >= :fromDate AND o.deliveryDate <= :toDate group by o.storeId order by sum(o.finalBillAmount) desc")
	List<StoreOrderTotalInfo> findStoreOrderTotalDetailsByStoreId(List<String> storeIds, java.sql.Date fromDate, java.sql.Date toDate);

	@Query(value = "SELECT sum(o.finalBillAmount) FROM OrderEntity o WHERE o.active = 1 AND o.status = 8 AND o.storeId in (:storeIds) AND o.deliveryDate >= :fromDate AND o.deliveryDate <= :toDate")
	Double getStoresTotalSalesBetweenDates(List<String> storeIds, java.sql.Date fromDate, java.sql.Date toDate);

	@Query("SELECT new com.sorted.rest.services.order.beans.SkuOrderTotalInfo(oi.skuCode, count(oi), sum(oi.finalAmount)) from OrderEntity o join OrderItemEntity oi on oi.orderId=o.id WHERE o.active = 1 AND o.status = 8 AND o.storeId in (:storeIds) AND o.deliveryDate >= :fromDate AND o.deliveryDate <= :toDate and oi.active=1 group by oi.skuCode order by sum(oi.finalAmount) desc")
	List<SkuOrderTotalInfo> findSkuTotalDetailsByStoreId(List<String> storeIds, java.sql.Date fromDate, java.sql.Date toDate);

	@Query(value = "SELECT sum(o.finalBillAmount) FROM OrderEntity o WHERE o.active = 1 AND o.status = 8 AND o.storeId in (:storeIds) AND o.deliveryDate = :date")
	Double getStoresTotalSalesOnDate(List<String> storeIds, java.sql.Date date);

	@Query("SELECT new com.sorted.rest.services.order.beans.OrderBreakdownItemBean(o.storeId,o.channel,sum(o.finalBillAmount)) from OrderEntity o where o.active = 1 AND o.status = 8 AND o.storeId in (:storeIds) AND o.deliveryDate >= :fromDate AND o.deliveryDate <= :toDate group by o.storeId,o.channel")
	List<OrderBreakdownItemBean> findStoreTotalDetailsByStoreIdAndChannel(List<String> storeIds, java.sql.Date fromDate, java.sql.Date toDate);

	@Query("SELECT new com.sorted.rest.services.order.beans.OrderBreakdownItemBean(o.storeId,o.paymentMethod,sum(o.finalBillAmount)) from OrderEntity o where o.active = 1 AND o.status = 8 AND o.storeId in (:storeIds) AND o.deliveryDate >= :fromDate AND o.deliveryDate <= :toDate group by o.storeId,o.paymentMethod")
	List<OrderBreakdownItemBean> findStoreTotalDetailsByStoreIdAndPaymentMode(List<String> storeIds, java.sql.Date fromDate, java.sql.Date toDate);

	@Query("SELECT new com.sorted.rest.services.order.beans.OrderBreakdownItemBean(oi.skuCode,o.channel,sum(oi.finalAmount)) from OrderEntity o join OrderItemEntity oi on oi.orderId=o.id where o.active = 1 AND oi.active = 1 AND o.status = 8 AND o.storeId in (:storeIds) AND o.deliveryDate >= :fromDate AND o.deliveryDate <= :toDate group by oi.skuCode,o.channel")
	List<OrderBreakdownItemBean> findSkuTotalBreakdownByChannel(List<String> storeIds, java.sql.Date fromDate, java.sql.Date toDate);

	@Query("SELECT new com.sorted.rest.services.order.beans.OrderBreakdownItemBean(oi.skuCode,o.paymentMethod,sum(oi.finalAmount)) from OrderEntity o join OrderItemEntity oi on oi.orderId=o.id where o.active = 1 AND oi.active = 1 AND o.status = 8 AND o.storeId in (:storeIds) AND o.deliveryDate >= :fromDate AND o.deliveryDate <= :toDate group by oi.skuCode,o.paymentMethod")
	List<OrderBreakdownItemBean> findSkuTotalBreakdownByPaymentMode(List<String> storeIds, java.sql.Date fromDate, java.sql.Date toDate);

	@Query("SELECT new com.sorted.rest.services.order.beans.OrderBreakdownItemBean(oi.skuCode,o.storeId,sum(oi.finalAmount)) from OrderEntity o join OrderItemEntity oi on oi.orderId=o.id where o.active = 1 AND oi.active = 1 AND o.status = 8 AND o.storeId in (:storeIds) AND o.deliveryDate >= :fromDate AND o.deliveryDate <= :toDate group by oi.skuCode,o.storeId")
	List<OrderBreakdownItemBean> findSkuTotalBreakdownByStoreIds(List<String> storeIds, java.sql.Date fromDate, java.sql.Date toDate);

	@Query("select o from OrderEntity o WHERE o.status = 0 and o.active = 1 and o.deliveryDate = :deliveryDate")
	Optional<List<OrderEntity>> findAllCartsInActiveState(java.sql.Date deliveryDate);

	@Query(value = "SELECT DISTINCT oi.sku_code, oi.product_name, oi.uom, oi.image_url, oi.ordered_qty, oi.metadata->>'pieces' pieces,oi.metadata->>'grades' grades, o.delivery_date - 1 as ordered_date, oi.category_name, oi.metadata->>'isOzoneWashedItem' FROM oms_trans_.order_items oi JOIN oms_trans_.orders o ON oi.order_id = o.id AND o.delivery_date > current_date - 30 JOIN (SELECT oi.sku_code, o.customer_id, MAX(o.delivery_date - 1)AS latest_created_at FROM oms_trans_.order_items oi JOIN oms_trans_.orders o ON oi.order_id = o.id AND o.delivery_date > current_date - 30 WHERE o.customer_id = :userId AND o.is_refund_order = 0 AND o.active = 1 AND o.status > 0 AND oi.active = 1 AND CASE WHEN oi.metadata->>'isComplimentary' IS NULL THEN FALSE WHEN oi.metadata->>'isComplimentary' = 'true' THEN TRUE ELSE FALSE END = FALSE GROUP BY oi.sku_code, o.customer_id) o2 ON oi.sku_code = o2.sku_code AND o.customer_id = o2.customer_id AND o.delivery_date - 1 = o2.latest_created_at AND oi.active = 1 AND o.active = 1 ORDER BY ordered_date DESC", nativeQuery = true)
	List<List<Object>> getPreviouslyOrderedSkus(UUID userId);

	@Query("select o from OrderEntity o WHERE o.parentOrderId = :parentOrderId and (o.status = 9 or o.status = 11) and o.active = 1")
	List<OrderEntity> findRefundOrdersFromParentId(UUID parentOrderId);

	@Query("select m from OrderEntity m WHERE m.parentOrderId = :parentOrderId and m.status = 9 and m.active = 1")
	List<OrderEntity> findRefundConfirmedOrdersWithParentOrderId(UUID parentOrderId);

	@Query(value = "select o.*  from oms_trans_.orders o join oms_trans_.order_items oi on o.id = oi.order_id and oi.active =1 where o.active =1 and o.status = 8 and o.delivery_date >= :fromDeliveryDate and oi.metadata ->>'isCashbackItem' ='true' and oi.metadata ->> 'isItemCashbackProcessed' = 'false' or (o.metadata ->>'isCashbackApplicable' ='true' and o.metadata ->> 'isCashbackProcessed' = 'false')", nativeQuery = true)
	List<OrderEntity> findOrdersWithCashbackItems(Date fromDeliveryDate);

	@Query("select o from OrderEntity o WHERE o.status in (0, 1) and o.active = 1 and o.deliveryDate = :deliveryDate")
	Optional<List<OrderEntity>> findAllOrdersForDSR(java.sql.Date deliveryDate);

	@Query("select o from OrderEntity o WHERE o.status = 8 and o.active = 1 and o.deliveryDate = :deliveryDate")
	Optional<List<OrderEntity>> findAllDeliveredOrdersOfDate(java.sql.Date deliveryDate);

	@Query("select o from OrderEntity o join UserOrderSettingEntity u on o.customerId = u.customerId where o.status = 0 and o.active = 1 and o.channel = 'App' and   o.deliveryDate = :deliveryDate and u.key = 'isAutoCheckoutEnabled' and u.value = 'false' and o.createdAt <= :createdAt")
	Optional<List<OrderEntity>> findInactiveAutoCheckoutOrdersAndCreatedAt(java.sql.Date deliveryDate, Date createdAt);

	@Query("SELECT new com.sorted.rest.services.order.beans.CustomerCartInfo(o.customerId, CASE WHEN o.active = 1 THEN o.finalBillAmount ELSE 0 END, o.active) FROM OrderEntity o WHERE o.modifiedAt >= :startTime AND o.modifiedAt <= :endTime AND o.deliveryDate = :deliveryDate AND (o.active = 1 OR (o.active = 0 AND NOT EXISTS (SELECT 1 FROM OrderEntity o2 WHERE o2.customerId = o.customerId AND o2.active = 1 AND o2.deliveryDate = :deliveryDate)))")
	Page<CustomerCartInfo> findCustomerCartInfoBetween(@Param("startTime") Date startTime, @Param("endTime") Date endTime, @Param("deliveryDate") Date deliveryDate, Pageable pageable);

	@Query("SELECT new com.sorted.rest.services.order.beans.OrderWithRepeatItemBean(o.id, o.customerId, o.finalBillAmount,o.metadata) FROM OrderEntity o JOIN OrderItemEntity oi ON oi.orderId = o.id LEFT JOIN UserOrderSettingEntity uos ON uos.customerId = o.customerId WHERE o.deliveryDate = :deliveryDate AND o.active = 1 AND o.status = 0 AND oi.isRepeatItem = true AND (uos IS NULL OR (uos.key = 'isAutoCheckoutEnabled' AND uos.value = 'true'))")
	Page<OrderWithRepeatItemBean> findOrdersWithRepeatItem(@Param("deliveryDate") Date deliveryDate, Pageable pageable);
}