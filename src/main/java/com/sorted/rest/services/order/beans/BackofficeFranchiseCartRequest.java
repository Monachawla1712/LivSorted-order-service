package com.sorted.rest.services.order.beans;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.sorted.rest.common.beans.ErrorBean;
import com.sorted.rest.services.common.upload.csv.CSVMapping;
import io.swagger.annotations.ApiModel;
import lombok.Data;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Data
@ApiModel(description = "Backoffice Franchise Store Cart Request Bean")
public class BackofficeFranchiseCartRequest implements Serializable, CSVMapping {

	private static final long serialVersionUID = -8927594493563844997L;

	@NotEmpty
	private String skuCode;

	@NotNull
	@Min(value = 0)
	private Double quantity;

	@Min(value = 0)
	private Double moq;

	private Integer whId;

	private Double permissibleRefundQuantity;

	private List<ErrorBean> errors = new ArrayList<>();

	public static BackofficeFranchiseCartRequest newInstance() {
		return new BackofficeFranchiseCartRequest();
	}

	public String computedKey() {
		return getSkuCode();
	}

	@Override
	public BackofficeFranchiseCartRequest newBean() {
		return newInstance();
	}

	@Override
	public List<ErrorBean> getErrors() {
		return errors;
	}

	@Override
	@JsonIgnore
	public String getHeaderMapping() {
		return "skuCode:Sku Code,quantity:Quantity";
	}
}
