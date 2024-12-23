package com.sorted.rest.services.order.beans;

import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Data
public class BulkStoreWalletRequest implements Serializable {

	private static final long serialVersionUID = 8453503875733187507L;

	private List<String> storeIds = new ArrayList<>();
}