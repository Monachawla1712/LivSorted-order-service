package com.sorted.rest.services.order.beans;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Data
public class OrderAdjustmentDetails implements Serializable {

	private static final long serialVersionUID = 7649495743589360269L;

	private OrderAdjustmentUserDetails requesterData;

	private OrderAdjustmentUserDetails approverData;

	private String requestDate;

	private String approvalDate;

	private String paymentNoteName;

}