package com.sorted.rest.services.order.beans;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class PaymentNoteDetails implements Serializable {

	private static final long serialVersionUID = 7649495743589360269L;

	private List<PaymentNoteData> notesList;

}