package com.sorted.rest.services.order.beans;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;
import java.util.UUID;

@Data
public class UserEventRequest implements Serializable {

	private static final long serialVersionUID = -8927594493563844997L;

	private String skuCode;

	private Date eventTime;

	private UUID userId;
}
