package com.sorted.rest.services.order.beans;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.sorted.rest.services.order.constants.OrderConstants.InvoiceType;
import com.sorted.rest.services.order.entity.InvoiceEntity;
import com.sorted.rest.services.order.entity.OrderEntity;
import com.sorted.rest.services.order.entity.OrderItemEntity;

import lombok.Data;

@Data
public class ConsumerInvoiceDataBean implements Serializable {

	private static final long serialVersionUID = -1919372447416754929L;

	private String recipientName;

	private String storeGstNumber;

	private String storePanNumber;

	private String address;

	private String storeType;

	private Integer isSrpStore;

	private Integer storeId;

	private UUID orderId;

	private String displayOrderId;

	private Date deliveryDate;

	private List<OrderItemEntity> orderItems;

	private List<OrderEntity> refundOrders;

	private Map<UUID, InvoiceRefundQtyAmountBean> refundQtyTotals;

	private BigDecimal totalMrpGrossAmount;

	private BigDecimal totalSpGrossAmount;

	private BigDecimal totalDiscountAmount;

	private BigDecimal finalBillAmount;

	private BigDecimal offerDiscountAmount;

	private String offerType;

	private BigDecimal totalAmount;

	private String invoiceName;

	private String invoiceId;

	private BigDecimal displayFinalAmount;

	private BigDecimal displayFinalQty;

	private InvoiceType invoiceType;

	private Integer isRefundInvoice = 0;

	private InvoiceEntity parentInvoice;

	private BigDecimal orderDeliveryCharge = new BigDecimal(0);
}
