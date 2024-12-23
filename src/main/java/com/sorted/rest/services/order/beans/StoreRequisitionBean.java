package com.sorted.rest.services.order.beans;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Type;

import java.io.Serializable;
import java.sql.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StoreRequisitionBean implements Serializable {

	private static final long serialVersionUID = -4867441878454480153L;

	private String skuCode;

	private String storeId;

	private String uom;

	private String productName;

	private Double orderedQty;

	private Date deliveryDate;

	@Type(type = "jsonb")
	private OrderItemMetadata metadata;

	public StoreRequisitionBean(String skuCode, String storeId, String uom, String productName, Double orderedQty, Object metadata) {
		this.skuCode = skuCode;
		this.storeId = storeId;
		this.uom = uom;
		this.productName = productName;
		this.orderedQty = orderedQty;
		this.metadata = (OrderItemMetadata) metadata;
	}

	public static StoreRequisitionBean newInstance(){
		return new StoreRequisitionBean();
	}
}
