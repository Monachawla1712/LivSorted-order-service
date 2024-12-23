package com.sorted.rest.services.order.beans;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.sql.Date;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WmsDsPayload implements Serializable {

	private static final long serialVersionUID = 9097728627395824014L;

	private String skuCode;

	private String grade;

	private Integer storeId;

	private Integer pieces;

	private Double orderedQty;

	private Double perPcWeight;

	private Date deliveryDate;

	private String uom;

	private String productName;

	private String suffix;

	private Double ozoneWashingQty;

	private List<String> notes = new ArrayList<>();

	public static WmsDsPayload newInstance() {
		return new WmsDsPayload();
	}

}
