package com.sorted.rest.services.order.beans;

import com.sorted.rest.services.order.constants.FranchiseOrderConstants;
import com.sorted.rest.services.order.entity.FranchiseOrderItemEntity;
import lombok.Data;

import java.io.Serializable;
import java.sql.Date;
import java.util.List;
import java.util.UUID;

@Data
public class ChallanDataBean implements Serializable {

	private static final long serialVersionUID = -1217133370533767490L;

	private String storeName;

	private String fileName;

	private Integer isSrpStore;

	private Integer storeId;

	private UUID orderId;

	private String displayOrderId;

	private String challanUrl;

	private Date date;

	private Double finalBillAmount;

	private List<FranchiseOrderItemEntity> orderItems;

	private Double outstandingAmount;

	private Double totalSpGrossAmount;

	private Double offerDiscountAmount;

	private FranchiseOrderConstants.FranchiseOrderStatus orderStatus;

	private StoreDataResponse storeData;

	private UserServiceResponse ownerDetails;

	private UserServiceResponse amDetails;
}
