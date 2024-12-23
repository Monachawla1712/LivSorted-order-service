package com.sorted.rest.services.order.beans;

import java.io.Serializable;
import java.util.List;
import java.util.UUID;

import com.sorted.rest.services.order.constants.OrderConstants.PaymentStatus;

import lombok.Data;

@Data
public class PaymentDetail implements Serializable {

	private static final long serialVersionUID = 2989920741806143595L;

	private PaymentStatus paymentStatus = PaymentStatus.PENDING;

	private String paymentGateway;

	private List<UUID> transactions;

	private Double pendingAmount;
}
