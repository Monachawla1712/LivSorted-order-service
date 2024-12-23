package com.sorted.rest.services.order.constants;

public class RejectionOrderConstants {

	public enum RejectionOrderStatus {

		IN_CART(0), NEW_ORDER(1), ORDER_BILLED(2), ORDER_DELIVERED(3), CANCELLED(4);

		private int value;

		private RejectionOrderStatus(int value) {
			this.value = value;
		}

		public int getValue() {
			return value;
		}
	}

	public enum RejectionOrderItemStatus {
		PENDING, PACKED, NOT_AVAILABLE;
	}

	public enum WhInventoryUpdateType {
		REGULAR, ADJUSTMENT, FRESH_STOCK, OLD_STOCK, STORE_RETURN, STOCK_TRANSFER, SECONDARY_ORDER, DUMP;
	}

	public enum RejectionOrderType {
		SECONDARY, DUMP;
	}

	public static final String REJECTION_ORDER_ITEMS_TABLE_NAME = "rejection_order_items";

	public static final String REJECTION_ORDER_TABLE_NAME = "rejection_orders";

}
