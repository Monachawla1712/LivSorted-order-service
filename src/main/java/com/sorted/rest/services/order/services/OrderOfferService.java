package com.sorted.rest.services.order.services;

import com.sorted.rest.common.dbsupport.crud.BaseCrudRepository;
import com.sorted.rest.common.websupport.base.BaseService;
import com.sorted.rest.services.order.beans.OrderOfferBean;
import com.sorted.rest.services.order.entity.OrderOfferEntity;
import com.sorted.rest.services.order.repository.OrderOfferRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class OrderOfferService implements BaseService<OrderOfferEntity> {

	@Autowired
	private OrderOfferRepository orderOfferRepository;

	@Override
	public Class getEntity() {
		return OrderOfferEntity.class;
	}

	@Override
	public BaseCrudRepository getRepository() {
		return orderOfferRepository;
	}

	public OrderOfferEntity createUpdateOrderOffer(OrderOfferBean request) {
		OrderOfferEntity entity = null;
		if (request.getId() != null) {
			entity = orderOfferRepository.findById(request.getId()).orElse(null);
		}

		if (entity == null) {
			entity = OrderOfferEntity.createOrderOffer(request.getSkuCode(), request.getQuantity(), request.getThresholdAmount(), request.getDiscountType(),
					request.getDiscountValue(), request.getOperator().getValue(), request.getFact().getValue(), request.getMetadata(), request.getValidTill());
		} else {
			OrderOfferEntity.setOrderOfferApplicationRules(entity, request.getThresholdAmount(), request.getOperator().getValue(), request.getDiscountType(),
					request.getDiscountValue(), request.getFact().getValue());
			entity.setMetadata(request.getMetadata());
			entity.setValidTill(request.getValidTill());
			entity.setQuantity(request.getQuantity());
			entity.setSkuCode(request.getSkuCode());
			entity.setQuantity(request.getQuantity());
			entity.setActive(request.getActive());
		}

		return save(entity);
	}

	public OrderOfferEntity save(OrderOfferEntity entity) {
		return orderOfferRepository.save(entity);
	}

}
