package com.sorted.rest.services.order.beans;

import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Data
public class BulkUserWalletRequest implements Serializable {

	private static final long serialVersionUID = 8453503875733187507L;

	private List<UUID> userIds = new ArrayList<>();
}
