package com.sorted.rest.services.order.beans;

import com.sorted.rest.services.order.constants.OrderConstants.InvoiceType;
import com.sorted.rest.services.order.entity.FranchiseOrderEntity;
import com.sorted.rest.services.order.entity.FranchiseOrderItemEntity;
import com.sorted.rest.services.order.entity.InvoiceEntity;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.*;

@Data
public class InvoiceDataBean implements Serializable {

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

	private List<FranchiseOrderItemEntity> orderItems;

	private List<FranchiseOrderEntity> refundOrders;

	private Map<UUID, InvoiceRefundQtyAmountBean> refundQtyTotals;

	private Set<String> excludingSkus;

	private BigDecimal totalSpGrossAmount;

	private BigDecimal totalMrpGrossAmount;

	private BigDecimal totalDiscountAmount;

	private BigDecimal finalBillAmount;

	private BigDecimal subQtyTotal;

	private BigDecimal subAmtTotal;

	private BigDecimal offerDiscountAmount;

	private String offerType;

	private Map<String, InvoiceAdjustmentsBean> adjustmentsMap;

	private BigDecimal totalAdjustments;

	private BigDecimal totalAmount;

	private String invoiceName;

	private String invoiceId;

	private Integer showPackingDetails = 0;

	private List<FranchiseOrderItemEntity> packingSkusDetails;

	private BigDecimal packingTotalCharges = new BigDecimal(0);

	private BigDecimal packingTotalDiscount = new BigDecimal(0);

	private BigDecimal packingTotalQty = new BigDecimal(0);

	private BigDecimal displayFinalAmount;

	private BigDecimal displayFinalQty;

	private InvoiceType invoiceType;

	private Integer isRefundInvoice = 0;

	private InvoiceEntity parentInvoice;

	private BigDecimal orderDeliveryCharge = new BigDecimal(0);
}
