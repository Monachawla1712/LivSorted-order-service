package com.sorted.rest.services.order.constants;

public class FranchiseOrderConstants {

	public enum FranchiseOrderStatus {

		IN_CART(0), NEW_ORDER(1), ORDER_BILLED(2), OUT_FOR_DELIVERY(3), ORDER_DELIVERED(4), REFUND_REQUESTED(5), ORDER_REFUNDED(6), CANCELLED(7), FAILED(
				8), PARTIALLY_DISPATCHED(9), CANCELLED_POST_BILLING(10);

		private int value;

		private FranchiseOrderStatus(int value) {
			this.value = value;
		}

		public int getValue() {
			return value;
		}
	}

	public enum SRType {

		NORMAL(0), PARTIAL_BULK(1), BULK(2);

		private int value;

		private SRType(int value) {
			this.value = value;
		}

		public int getValue() {
			return value;
		}
	}

	public enum DeliverySlot {
		MORNING_7_AM, EVENING_5_PM;
	}

	public enum FranchiseOrderItemStatus {
		PENDING, PACKED, NOT_AVAILABLE, DELIVERED;
	}

	public enum OrderAdjustmentStatus {
		PENDING(0), APPROVED(1), REJECTED(2);

		private int value;

		private OrderAdjustmentStatus(int value) {
			this.value = value;
		}

		public int getValue() {
			return value;
		}
	}

	public enum OrderAdjustmentTransactionMode {
		DEBIT, CREDIT;
	}

	public enum FranchiseOrderItemLogType {
		USER_CHANGE, AUTO_REMOVAL;
	}

	public static final String IMS_REFUND_ORDER_TXN_TYPE = "FO-ITEM-REFUND";

	public static final String IMS_REFUND_ORDER_REFUND_TYPE = "IMS_RETURN";

	public static final String IMS_REFUND_ALL_ORDER_TXN_TYPE = "FO-FULL-ORDER-REFUND";
}
