package com.sorted.rest.services.order.beans;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

/**
 * Bean to be returned with Contains Orders
 *
 * @author Mohit
 * @version $Id: $Id
 */
@ApiModel(description = "Order Response Bean extending the List Bean")
@Data
public class OrderResponseBean extends OrderListBean implements Serializable {

	private static final long serialVersionUID = 2102504245219017738L;

	@ApiModelProperty(value = "total mrp gross amount", allowEmptyValue = false)
	@NotNull
	private Double totalMrpGrossAmount;

	@ApiModelProperty(value = "total Sp Gross Amount", allowEmptyValue = false)
	@NotNull
	private Double totalSpGrossAmount;

	@ApiModelProperty(value = "total Discount Amount", allowEmptyValue = false)
	@NotNull
	private Double totalDiscountAmount;

	@ApiModelProperty(value = "total Tax Amount", allowEmptyValue = false)
	@NotNull
	private Double totalTaxAmount;

	@ApiModelProperty(value = "total Extra Fee Amount", allowEmptyValue = false)
	@NotNull
	private Double totalExtraFeeAmount;

	@ApiModelProperty(value = "refund Amount", allowEmptyValue = false)
	@NotNull
	private Double refundAmount;

	@ApiModelProperty(value = "Tax Details", allowEmptyValue = false)
	@NotNull
	private TaxDetails taxDetails;

	@ApiModelProperty(value = "total Additional Discount", allowEmptyValue = false)
	@NotNull
	private AdditionalDiscount totalAdditionalDiscount;

	@ApiModelProperty(value = "extra Fee Details", allowEmptyValue = false)
	@NotNull
	private ExtraFeeDetail extraFeeDetails;

	@ApiModelProperty(value = "channel", allowEmptyValue = false)
	@NotNull
	private String channel;

	@ApiModelProperty(value = "Order Items", allowEmptyValue = false)
	@NotNull
	private List<OrderItemResponseBean> orderItems;

	@ApiModelProperty(value = "Order Charges", allowEmptyValue = true)
	private List<OrderCharges> orderCharges;

	@ApiModelProperty(value = "Order Notes", allowEmptyValue = true)
	private String notes;

	@ApiModelProperty(value = "Offer Data", allowEmptyValue = true)
	private OfferData offerData;

	@ApiModelProperty(value = "Checkout Message", allowEmptyValue = true)
	private String checkoutMessage;

	@ApiModelProperty(value = "ETA Message", allowEmptyValue = true)
	private String etaMessage;

	@ApiModelProperty(value = "Additional Buffer Time", allowEmptyValue = true)
	private String bufferETAMessage;

	@ApiModelProperty(value = "Wallet check", allowEmptyValue = false)
	private Boolean walletError = false;

	@ApiModelProperty(value = "Wallet check", allowEmptyValue = true)
	private Double walletBalance;

	@ApiModelProperty(value = "Wallet Loyalty Coins", allowEmptyValue = true)
	private Double walletLoyaltyCoins;

	@ApiModelProperty(value = "Final Bill Coins", allowEmptyValue = true)
	private Double finalBillCoins;

	@ApiModelProperty(value = "Total coins given to the user based on cart", allowEmptyValue = true)
	private Double coinsReceived;

	@ApiModelProperty(value = "Total coins given to the user based on cart", allowEmptyValue = true)
	private Double cartCoinsEarned;

	@ApiModelProperty(value = "Total coins given to the user based on cart", allowEmptyValue = true)
	private BigDecimal coinsAfterDeduction;

	@ApiModelProperty(value = "Order Count", allowEmptyValue = true)
	private Long orderCount;

	@ApiModelProperty(value = "Cart Images", allowEmptyValue = true)
	private List<String> cartImages;

	@ApiModelProperty(value = "Cart Images", allowEmptyValue = true)
	private String storeName = "Sorted Store";

	@ApiModelProperty(value = "Slot ID", allowEmptyValue = true)
	private Integer slotId;

	@ApiModelProperty(value = "Address Error", allowEmptyValue = false)
	private String addressError;

	@ApiModelProperty(value = "Wallet Error Message", allowEmptyValue = false)
	private String walletErrorMessage;

	@ApiModelProperty(value = "Deliver Fee Message", allowEmptyValue = true)
	private String deliveryFeeMessage;
  
	@ApiModelProperty(value = "Delivery Date", allowEmptyValue = true)
	private Date deliveryDate;

	private GameBean gameDetails;

	@ApiModelProperty(value = "invoice", allowEmptyValue = true)
	private InvoiceResponseBean invoice;
	
	private Double totalCashbackAmount;

	private String cashbackLabel;

	private String cashbackMessage;

	private Double itemCashBackAmount;

	public static OrderResponseBean newInstance() {
		return new OrderResponseBean();
	}

}
