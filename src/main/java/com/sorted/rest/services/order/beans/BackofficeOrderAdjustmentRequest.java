package com.sorted.rest.services.order.beans;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.sorted.rest.common.beans.ErrorBean;
import com.sorted.rest.services.common.upload.csv.CSVMapping;
import io.swagger.annotations.ApiModel;
import lombok.Data;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Data
@ApiModel(description = "Backoffice Franchise Store Cart Request Bean")
public class BackofficeOrderAdjustmentRequest implements Serializable, CSVMapping {

	private static final long serialVersionUID = -8927594493563844997L;

	@NotEmpty
	private String displayOrderId;

	@NotEmpty
	private String txnType;

	@NotNull
	private Double amount;

	@NotEmpty
	private String remarks;

	private List<ErrorBean> errors = new ArrayList<>();

	public static BackofficeOrderAdjustmentRequest newInstance() {
		return new BackofficeOrderAdjustmentRequest();
	}

	@Override
	public BackofficeOrderAdjustmentRequest newBean() {
		return newInstance();
	}

	@Override
	public List<ErrorBean> getErrors() {
		return errors;
	}

	@Override
	@JsonIgnore
	public String getHeaderMapping() {
		return "displayOrderId:Order Id,txnType:Txn Type,amount:amount,remarks:remarks";
	}

}

