package com.sorted.rest.services.order.beans;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import javax.validation.constraints.NotNull;

@JsonIgnoreProperties(ignoreUnknown = true)
@Data
public class OrderMetadata implements Serializable {

	private static final long serialVersionUID = 7649495743589360269L;

	@NotNull
	private Location location;

	private OrderContactDetail contactDetail;

	private OrderEta eta = new OrderEta();

	private List<OrderProductMetadata> productMetadata;

	private List<OrderProductMetadata> alsoContains;

	private BigDecimal calories;

	private Map<String, Long> categoryCount;

	private Double storeDistance;

	private Location storeLocation;

	private String zoneId;

	private String orderSlot;

	private Integer orderCount;

	private String eligibleOfferMsg;

	private List<String> failureReason;

	private Boolean gracePeriodAllowed = false;

	private BigDecimal orderPlacedAmount = null;

	private DeliveryDetails deliveryDetails;

	private Long societyId;

	private String billUrl;

	private String ppdRemarks;

	private Boolean isVip = false;

	private Boolean isPrepaid = Boolean.FALSE;

	private String appVersion;

	private Boolean isBottomSheetShown = Boolean.FALSE;

	private Boolean isCod;

	private Boolean isFirstOrder = Boolean.FALSE;

	private Boolean isCashbackApplicable = Boolean.FALSE;

	private Double cashbackAmount;

	private Boolean isCashbackProcessed = Boolean.FALSE;

	private Integer ozoneWashedItemCount;

}