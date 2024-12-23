package com.sorted.rest.services.order.beans;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sorted.rest.services.order.constants.OrderConstants;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class OrderItemMetadata implements Serializable {

	private static final long serialVersionUID = 7649495743589360269L;

	private String productName;

	private Integer pieces;

	private String suffix;

	private Double perPiecesWeight;

	private List<CartRequest.OrderItemGradeBean> grades;

	private String notes;

	private Double itemCashbackMaxQty;

	private Double itemCashbackQty;

	private Double itemCashbackAmount;

	private Boolean isCashbackItem = false;

	private OrderConstants.CashbackType cashbackType;

	private Boolean isItemCashbackProcessed = false;

	private Boolean isComplimentary = false;

	private String cashBackErrorMsg;

	private Integer discountPercentage;

	private String packetDescription;

	private Boolean isOrderOfferItem = false;

	private Boolean isPrebooked = false;

	private String prebookDeliveryDate;

	private Double orderedFinalAmount;

	private Integer finalPieces;

	private Boolean isShowOnOrderSummary = Boolean.TRUE;

	private Boolean isOzoneWashedItem = Boolean.FALSE;

	private Double ozoneWashingCharge = 0d;
}