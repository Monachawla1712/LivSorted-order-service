package com.sorted.rest.services.order.beans;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.sorted.rest.common.beans.ErrorBean;
import com.sorted.rest.services.common.upload.csv.CSVMapping;

import lombok.Data;

@Data
public class FranchiseReBillUploadBean implements Serializable, CSVMapping {

	private static final long serialVersionUID = -6602537424526371257L;

	@NotNull
	private String skuCode;

	@NotNull
	private Double finalQty;

	@NotNull
	private Integer crateQty;

	public static FranchiseReBillUploadBean newInstance() {
		return new FranchiseReBillUploadBean();
	}

	public FranchiseReBillUploadBean newBean() {
		return newInstance();
	}

	@Override
	@JsonIgnore
	public String getHeaderMapping() {
		return "skuCode:Sku Code,finalQty:Final Quantity,crateQty:Total Crates";
	}

	private List<ErrorBean> errors = new ArrayList<>();

}
