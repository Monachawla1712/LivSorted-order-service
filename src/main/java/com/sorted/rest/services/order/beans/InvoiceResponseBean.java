package com.sorted.rest.services.order.beans;

import lombok.Data;

import javax.persistence.Column;
import java.io.Serializable;

@Data
public class InvoiceResponseBean implements Serializable {

	private Integer id;

	private String invoiceName;

	private String invoiceUrl;

	private String invoiceType;

	private Double amount;

	private PaymentNoteDetails paymentNotes;

}
