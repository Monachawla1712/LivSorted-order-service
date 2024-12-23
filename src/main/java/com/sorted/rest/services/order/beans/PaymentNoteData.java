package com.sorted.rest.services.order.beans;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Data
public class PaymentNoteData implements Serializable {

	private String name;

	private String type;

	private String url;

	private Date date;

	private Double amount;

	private String displayOrderId;

	public static PaymentNoteData newInstance() {
		return new PaymentNoteData();
	}

	public static PaymentNoteData createPaymentNoteData(String name, String invoiceType, String invoiceUrl, Date date, Double invoiceAmount, String displayOrderId) {
		PaymentNoteData paymentNoteData = newInstance();
		paymentNoteData.setName(name);
		paymentNoteData.setUrl(invoiceUrl);
		paymentNoteData.setDate(date);
		paymentNoteData.setType(invoiceType);
		paymentNoteData.setAmount(invoiceAmount);
		paymentNoteData.setDisplayOrderId(displayOrderId);
		return paymentNoteData;
	}
}
