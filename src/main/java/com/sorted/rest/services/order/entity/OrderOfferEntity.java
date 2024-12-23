package com.sorted.rest.services.order.entity;

import com.sorted.rest.common.websupport.base.BaseEntity;
import com.sorted.rest.services.order.beans.*;
import com.sorted.rest.services.order.constants.OrderConstants;
import lombok.Data;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.Type;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Entity
@Table(name = OrderConstants.ORDER_OFFERS_TABLE_NAME)
@Data
@DynamicUpdate
public class OrderOfferEntity extends BaseEntity {

	private static final long serialVersionUID = 3566794617286775186L;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(updatable = false, nullable = false)
	private Integer id;

	@Column
	private String skuCode;

	@Column
	private double quantity;

	@Type(type = "jsonb")
	@Column(columnDefinition = "jsonb")
	private OfferApplicationRules offerApplicationRules;

	@Type(type = "jsonb")
	@Column(columnDefinition = "jsonb")
	private OrderOfferMetadata metadata = new OrderOfferMetadata();

	@Type(type = "date")
	@Column
	private Date validTill;

	public static OrderOfferEntity createOrderOffer(String skuCode, double quantity, Double thresholdAmount, OrderConstants.DiscountType discountType,
			Double discountValue, String operator, String fact, OrderOfferMetadata metadata, Date validTill) {
		OrderOfferEntity orderOfferEntity = new OrderOfferEntity();

		setOrderOfferApplicationRules(orderOfferEntity, thresholdAmount, operator, discountType, discountValue, fact);

		orderOfferEntity.setMetadata(metadata);
		orderOfferEntity.setSkuCode(skuCode);
		orderOfferEntity.setQuantity(quantity);
		orderOfferEntity.setValidTill(validTill);

		return orderOfferEntity;
	}

	public static void setOrderOfferApplicationRules(OrderOfferEntity orderOfferEntity, Double thresholdAmount, String operator,
			OrderConstants.DiscountType discountType, Double discountValue, String fact) {
		OfferApplicationRules offerApplicationRules = new OfferApplicationRules();

		Conditions conditions = new Conditions();
		List<Condition> conditionList = new ArrayList<>();
		Event event = new Event();
		Params params = new Params();
		Condition condition = new Condition();

		//set order offers conditions , currently we are using only all , and it contains only 1 condition.
		condition.setFact(fact);
		condition.setValue(thresholdAmount);
		condition.setOperator(operator);

		conditionList.add(condition);
		conditions.setAll(conditionList);

		// set event , currently we are using only order_level events
		params.setDiscountType(discountType);
		params.setDiscountValue(discountValue);

		event.setType(OrderConstants.ORDER_LEVEL_EVENT);
		event.setParams(params);

		offerApplicationRules.setConditions(conditions);
		offerApplicationRules.setEvent(event);

		orderOfferEntity.setOfferApplicationRules(offerApplicationRules);
	}

	public static OrderOfferEntity newInstance() {
		return new OrderOfferEntity();
	}

}
