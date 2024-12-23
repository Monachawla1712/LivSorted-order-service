package com.sorted.rest.services.order.constants;

public class OrderConstants {

	public enum OrderStatus {

		IN_CART(0), NEW_ORDER(1), ORDER_ACCEPTED_BY_STORE(2), ORDER_CANCELLED_BY_STORE(3), ORDER_BILLED(4), ORDER_CANCELLED_BY_CUSTOMER(5), READY_FOR_PICKUP(6),
		ORDER_OUT_FOR_DELIVERY(7), ORDER_DELIVERED(8), ORDER_REFUNDED(9), ORDER_FAILED(10);

		private int value;

		private OrderStatus(int value) {
			this.value = value;
		}

		public int getValue() {
			return value;
		}
	}

	public enum OrderItemStatus {

		PENDING(0), PACKED(1), NOT_AVAILABLE(2);

		private int value;

		private OrderItemStatus(int value) {
			this.value = value;
		}

		public int getValue() {
			return value;
		}
	}

	public enum ShippingMethod {

		STORE_PICKUP(0), HOME_DELIVERY(1);

		private int value;

		private ShippingMethod(int value) {
			this.value = value;
		}

		public int getValue() {
			return value;
		}

	}

	public enum PaymentMethod {

		CASH(0), CC(1), UPI(2), WALLET(3);

		private int value;

		private PaymentMethod(int value) {
			this.value = value;
		}

		public int getValue() {
			return value;
		}
	}

	public enum DiscountType {

		FLAT(0), PERCENT(1);

		private int value;

		private DiscountType(int value) {
			this.value = value;
		}

		public int getValue() {
			return value;
		}
	}

	public enum PaymentStatus {

		PENDING(0), IN_PROGRESS(1), SUCCESS(2), FAILED(3);

		private int value;

		private PaymentStatus(int value) {
			this.value = value;
		}

		public int getValue() {
			return value;
		}
	}

	public enum WalletStatus {

		INACTIVE(0), ACTIVE(1);

		private int value;

		private WalletStatus(int value) {
			this.value = value;
		}

		public int getValue() {
			return value;
		}
	}

	public enum RepeatOrderStatus {

		PAUSED("PAUSED"), ACTIVE("ACTIVE");

		private final String value;

		private RepeatOrderStatus(String value) {
			this.value = value;
		}

		public String getValue() {
			return value;
		}
	}

	public enum CouponType {

		COUPON_CODE(0), SIGNUP_CODE(1);

		private int value;

		private CouponType(int value) {
			this.value = value;
		}

		public int getValue() {
			return value;
		}
	}

	public enum CashCollectionStatus {
		REQUESTED, COLLECTED, CANCELLED, RECEIVED, APPROVED, REJECTED, UNCOLLECTED;
	}

	public enum PosAdminReportViewType {
		DAILY_ORDER_VIEW, STORE_VIEW, SKU_VIEW;
	}

	public enum ReportBreakdownType {
		CHANNEL, PAYMENT_MODE, STORE_IDS, SKU_CODE;
	}

	public enum OrderPageAction {
		NONE, HELP, FEEDBACK;
	}

	public enum Channel {
		BACKOFFICE, DELIVERY_APP, CONSUMER_APP, APP, WEB_APP;
	}

	public enum Fact {

		TOTAL_SP_GROSS_AMOUNT("totalSpGrossAmount");

		private final String value;

		Fact(String value) {
			this.value = value;
		}

		public String getValue() {
			return value;
		}

		public static Fact fromValue(String value) {
			for (Fact fact : values()) {
				if (fact.value.equals(value)) {
					return fact;
				}
			}
			throw new IllegalArgumentException("Unsupported operator: " + value);
		}
	}

	public enum InvoiceType {
		FRANCHISE_INVOICE, FRANCHISE_CREDIT_NOTE, FRANCHISE_DEBIT_NOTE, CONSUMER_INVOICE, CONSUMER_CREDIT_NOTE, CONSUMER_DEBIT_NOTE;
	}

	public class ClevertapConstants {

		public static final String CLEVERTAP_CRON_LAST_SYNCED_TILL = "CLEVERTAP_CRON_LAST_SYNCED_TILL";

		public static final String CLEVERTAP_PROFILE_UPDATE_CRON_INTERVAL = "CLEVERTAP_PROFILE_UPDATE_CRON_INTERVAL";
	}

	public enum OfferType {
		ONBOARDING_AMOUNT_OFFER("ONBOARDING_AMOUNT_OFFER"), DISCOUNTED_ITEM_OFFER("DISCOUNTED_ITEM_OFFER"), CASHBACK_OFFER("CASHBACK_OFFER"),
		MINIMUM_RECHARGE_OFFER("MINIMUM_RECHARGE_OFFER"), ORDER_DISCOUNT_OFFER("ORDER_DISCOUNT_OFFER");

		private final String value;

		OfferType(String value) {
			this.value = value;
		}

		public String getValue() {
			return value;
		}
	}

	public enum CashbackType {
		FREE_ITEM("ITEM_CASHBACK"), TREE_GAME("TREE_GAME");

		private final String value;

		CashbackType(String value) {
			this.value = value;
		}

		public String getValue() {
			return value;
		}
	}

	public enum OrderChannel {
		BACKOFFICE("Backoffice"), CONSUMER_APP("App");

		private final String value;

		OrderChannel(String value) {
			this.value = value;
		}

		public String getValue() {
			return value;
		}
	}

	public enum UserEventType {
		NOTIFICATION_PERMISSION("NOTIFICATION_PERMISSION"), OOS_ITEM_TAPPED("OOS_ITEM_TAPPED");

		private final String value;

		UserEventType(String value) {
			this.value = value;
		}

		public String getValue() {
			return value;
		}
	}

	public enum ComparisonOperator {
		EQUALS("equals"), GREATER_THAN("greaterThan"), LESS_THAN("lessThan"), GREATER_THAN_INCLUSIVE("greaterThanInclusive"),
		LESS_THAN_INCLUSIVE("lessThanInclusive");

		private final String value;

		ComparisonOperator(String value) {
			this.value = value;
		}

		public String getValue() {
			return value;
		}

		public static ComparisonOperator fromValue(String value) {
			for (ComparisonOperator operator : values()) {
				if (operator.value.equals(value)) {
					return operator;
				}
			}
			throw new IllegalArgumentException("Unsupported operator: " + value);
		}
	}

	public static final Integer CONSUMER_ORDER_DAYS_FACTOR = 1;

	public static final Integer POS_ORDER_DAYS_FACTOR = 0;

	public static final String ORDER_ITEMS_TABLE_NAME = "order_items";

	public static final String ORDERS_TABLE_NAME = "orders";

	public static final String REPEAT_ORDER_TABLE_NAME = "repeat_orders";

	public static final String DISPLAY_ORDER_ID = "display_order_ids";

	public static final String ORDER_EVENT_LOGS_TABLE_NAME = "order_event_logs";

	public static final String FRANCHISE_ORDER_ITEMS_TABLE_NAME = "franchise_order_items";

	public static final String FRANCHISE_ORDER_ITEM_LOGS_TABLE_NAME = "franchise_order_item_logs";

	public static final String FRANCHISE_ORDERS_TABLE_NAME = "franchise_orders";

	public static final String ORDER_SLOT_TABLE_NAME = "order_slot";

	public static final String ORDER_DELIVERED_PN_TEMPLATE_NAME = "ORDER_DELIVERED_PN";

	public static final String ENABLE_AUTO_CHECKOUT_TEMPLATE_NAME = "ENABLE_AUTO_CHECKOUT";

	public static final String RECHARGE_WALLET_BEFORE_6_PM_TEMPLATE_NAME = "RECHARGE_WALLET_BEFORE_6_PM";

	public static final String RECHARGE_WALLET_AFTER_6_PM_TEMPLATE_NAME = "RECHARGE_WALLET_AFTER_6_PM";

	public static final String RECHARGE_WALLET_BEFORE_12_PM_TEMPLATE_NAME = "RECHARGE_WALLET_BEFORE_12_PM";

	public static final String INVOICES_TABLE_NAME = "invoices";

	public static final String ORDER_ADJUSTMENTS_TABLE_NAME = "order_adjustments";

	public static final String SKUS_PURCHASE_COST_TABLE_NAME = "skus_purchase_cost";

	public static final String PARTNER_APP_APP_ID = "com.sorted.partnerflutterapp";

	public static final String BACKOFFICE_APP_ID = "com.sorted.admin_portal";

	public static final String CONSUMER_APP_ID = "com.sorted.consumerflutterapp";

	public static final String IMS_REFUND_CONSUMER_ORDER_TXN_TYPE = "ORDER-ITEM-REFUND";

	public static final String IMS_REFUND_ORDER_REFUND_TYPE = "IMS_RETURN";

	public static final String IMS_REFUND_CONSUMER_ALL_ORDER_TXN_TYPE = "FULL-ORDER-REFUND";

	public static final String CASHBACK_CREDITED_TEMPLATE_NAME = "CASHBACK_CREDITED";

	public static final String COD_ORDER_TEMPLATE_NAME = "COD_ORDER";

	public static final String CASHBACK_FREE_TOMATO_TEMPLATE_NAME = "CASHBACK_FREE_TOMATO";

	public static final String CASHBACK_ORDER = "CASHBACK_ORDER";

	public static final String DEFAULT_SKU_GRADE = "Default";

	public static final String ORDER_OFFERS_TABLE_NAME = "order_offers";

	public static final String PREBOOKED_ORDERS = "prebooked_orders";

	public static final String USER_ORDER_SETTING_TABLE_NAME = "user_order_settings";

	public static final String PROMO_CASHBACK_TXN_TYPE = "PROMO-CB";

	public static final String ITEM_OUT_OF_STOCK = "ITEMS_OOS";

	public static final String STORE_PS_2 = "10003";

	public static final String STORE_PS_3 = "10004";

	public static final String ORDER_LEVEL_EVENT = "ORDER_LEVEL";

	public static final String DISPLAY_ORDER_ID_QUEUE = "DISPLAY_ORDER_ID_QUEUE";

}