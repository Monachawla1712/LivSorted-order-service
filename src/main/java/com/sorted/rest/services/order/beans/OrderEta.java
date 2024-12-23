package com.sorted.rest.services.order.beans;

import java.io.Serializable;

import lombok.Data;

@Data
public class OrderEta implements Serializable {

	private static final long serialVersionUID = 4988909671286229233L;

	private String delayTime;

	private String delayReason;

	private String eta;
}
