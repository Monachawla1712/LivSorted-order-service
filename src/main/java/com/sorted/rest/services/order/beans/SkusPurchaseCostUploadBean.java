package com.sorted.rest.services.order.beans;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.sorted.rest.common.beans.ErrorBean;
import com.sorted.rest.services.common.upload.csv.CSVMapping;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.sql.Date;
import java.util.ArrayList;
import java.util.List;

@Data
public class SkusPurchaseCostUploadBean implements Serializable, CSVMapping {

	private static final long serialVersionUID = -6602537424526371257L;

	@NotNull
	private String skuCode;

	private Date deliveryDate;

	private String avgCostPrice;

	private String purchaseQuantity;

	private String mrp;

	public static SkusPurchaseCostUploadBean newInstance() {
		return new SkusPurchaseCostUploadBean();
	}

	public SkusPurchaseCostUploadBean newBean() {
		return newInstance();
	}

	public String computedKey(){
		return getSkuCode();
	}

	@Override
	@JsonIgnore
	public String getHeaderMapping() {
		return "skuCode:Sku Code,avgCostPrice:Cost,purchaseQuantity:Quantity,mrp:MRP";
	}

	private List<ErrorBean> errors = new ArrayList<>();

}
