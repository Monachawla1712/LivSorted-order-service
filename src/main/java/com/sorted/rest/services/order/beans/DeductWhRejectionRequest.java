package com.sorted.rest.services.order.beans;

import com.sorted.rest.services.order.constants.RejectionOrderConstants;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

@Data
@AllArgsConstructor
public class DeductWhRejectionRequest implements Serializable {

	private static final long serialVersionUID = 8949302740350182245L;

	private Integer storeId;

	private Integer whId;

	private RejectionOrderConstants.WhInventoryUpdateType inventoryUpdateType;

	private List<VerifyAndDeductRejectionData> inventoryList;

	@Data
	@NoArgsConstructor
	public static class VerifyAndDeductRejectionData {

		private String skuCode;

		private Double quantity;

	}
}
