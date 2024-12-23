package com.sorted.rest.services.order.beans;

import lombok.Data;

@Data
public class SkuPerPcsWeightDto {

	String skuCode;

	Integer totalFinalPcs;

	Double totalFinalQty;

	Double perPcsWt;

}
