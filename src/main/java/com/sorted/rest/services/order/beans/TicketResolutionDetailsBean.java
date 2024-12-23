package com.sorted.rest.services.order.beans;

import io.swagger.annotations.ApiModel;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@ApiModel(description = "Ticket Resolution Details Bean")
@Data
public class TicketResolutionDetailsBean implements Serializable {

	private static final long serialVersionUID = 2102504245219017738L;

	private String description;

	private List<String> tags;

	private String resolvedRemarks;

	private ConsumerOrderItemDetailsBean consumerOrderDetails;

	public static TicketResolutionDetailsBean newInstance() {
		return new TicketResolutionDetailsBean();
	}

	@Data
	public static class ConsumerOrderItemDetailsBean implements Serializable {

		private static final long serialVersionUID = 2102504245219017738L;

		private String skuCode;

		private String productName;

		private String imageUrl;

		private UUID orderId;

		private Double finalItemAmount;

		private Double prorataAmount;

		private Double refundableAmount;

		private String itemStatus;

		private String uom;

		private Double orderedQty;

		private Double deliveredQty;

		private Double issueQty;

		private Double refundableQty;

		private Double resolvedQty;

		private Double refundAmount;

		private Date refundedAt;

		private Boolean isReturnIssue = false;

		private Boolean isAutoRefundEligible = false;

		private OrderItemMetadata metadata;

	}
}
