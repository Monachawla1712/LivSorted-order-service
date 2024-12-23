package com.sorted.rest.services.order.beans;

import lombok.Data;

import java.util.List;

@Data
public class AuthServiceStoreDetailsBean {

	private List<UserMappedStoreDetails> whData;

	private boolean isApprovalRequested;

}
