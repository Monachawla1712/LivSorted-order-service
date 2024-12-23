package com.sorted.rest.services.order.entity;

/**
 * Column field mapping as constants for table OrderProducts
 *
 * @author mohit
 */
public interface OrderEntityConstants {

	interface Query {

		String GET_CUSTOMER_CURRENT_CART = "SELECT o FROM OrderEntity o WHERE o.active = 1 AND o.customerId =:customerId AND o.status = 0 AND o.deliveryDate=:deliveryDate";

		String GET_CUSTOMER_ORDER_LIST = "SELECT o FROM OrderEntity o WHERE o.active = 1 AND o.customerId =:customerId AND o.status not in (0, 9, 10) order by o.deliveryDate DESC";

		String GET_STORE_CURRENT_CART = "SELECT o FROM FranchiseOrderEntity o WHERE o.active = 1 AND o.storeId =:storeId AND o.status = 0";
	}
}