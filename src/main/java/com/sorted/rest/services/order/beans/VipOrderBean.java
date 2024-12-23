package com.sorted.rest.services.order.beans;

import lombok.Data;

import java.io.Serializable;
import java.util.UUID;

@Data
public class VipOrderBean implements Serializable {

	private static final long serialVersionUID = 5683867250042401137L;

	private Integer vipOrderNum;

	private UUID customerId;

}
