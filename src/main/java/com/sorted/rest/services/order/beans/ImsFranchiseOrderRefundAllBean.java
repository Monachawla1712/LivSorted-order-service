package com.sorted.rest.services.order.beans;

import lombok.Data;

import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.UUID;

@Data
public class ImsFranchiseOrderRefundAllBean implements Serializable {

	private static final long serialVersionUID = 4143679869385600722L;

	@NotNull
	private UUID parentOrderId;

	@NotNull
	private Long ticketId;

	@NotNull
	private Long ticketItemId;

	private Boolean warehouseReturnCheck;

	private String refundRemarks;

	public static ImsFranchiseOrderRefundAllBean newInstance() {
		return new ImsFranchiseOrderRefundAllBean();
	}
}