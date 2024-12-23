package com.sorted.rest.services.order.beans;

import lombok.Data;

import java.io.Serializable;
import java.util.UUID;

@Data
public class FranchiseRefundDetails implements Serializable {

	private static final long serialVersionUID = 7649495743589360269L;

	private String remarks;

	private Boolean warehouseReturnCheck;

	private String requestedBy;

	private String approvedBy;

	private String paymentNoteName;

}