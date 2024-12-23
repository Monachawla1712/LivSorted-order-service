package com.sorted.rest.services.order.beans;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.sorted.rest.services.order.constants.FranchiseOrderConstants;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
@AllArgsConstructor
public class ConfirmOrderAdjustmentRequest implements Serializable {

	private static final long serialVersionUID = 8949302740350182245L;

	private String action;

	private List<Long> adjustmentIds;

}
