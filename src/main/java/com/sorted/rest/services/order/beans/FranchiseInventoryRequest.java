package com.sorted.rest.services.order.beans;

import java.io.Serializable;

import lombok.Data;

@Data
public class FranchiseInventoryRequest implements Serializable {

	private static final long serialVersionUID = -5011047134575532627L;

	Integer whId;

	String skuCode;

}
