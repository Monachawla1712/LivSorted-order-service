package com.sorted.rest.services.order.controller;

import com.sorted.rest.common.dbsupport.pagination.PageAndSortRequest;
import com.sorted.rest.common.dbsupport.pagination.PageAndSortResult;
import com.sorted.rest.common.websupport.base.BaseController;
import com.sorted.rest.services.common.mapper.BaseMapper;
import com.sorted.rest.services.order.beans.OrderOfferBean;
import com.sorted.rest.services.order.constants.OrderConstants;
import com.sorted.rest.services.order.entity.OrderOfferEntity;
import com.sorted.rest.services.order.services.OrderOfferService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
public class OrderOfferController implements BaseController {

	@Autowired
	private BaseMapper<?, ?> mapper;

	@Autowired
	private OrderOfferService orderOfferService;

	@PostMapping("/orders/offers")

	@Override
	public BaseMapper<?, ?> getMapper() {
		return mapper;
	}

	@PostMapping("/orders/order-offer")
	public ResponseEntity<OrderOfferBean> createUpdateOrderOffer(@Valid @RequestBody OrderOfferBean request) {
		OrderOfferEntity entity = orderOfferService.createUpdateOrderOffer(request);
		buildOrderOfferResponse(entity, request);
		return ResponseEntity.ok().body(request);
	}

	public void buildOrderOfferResponse(OrderOfferEntity offerEntity, OrderOfferBean orderOfferBean) {
		getMapper().mapSrcToDest(offerEntity, orderOfferBean);

		orderOfferBean.setFact(OrderConstants.Fact.fromValue(offerEntity.getOfferApplicationRules().getConditions().getAll().get(0).getFact()));
		orderOfferBean.setOperator(
				OrderConstants.ComparisonOperator.fromValue(offerEntity.getOfferApplicationRules().getConditions().getAll().get(0).getOperator()));
		orderOfferBean.setThresholdAmount(offerEntity.getOfferApplicationRules().getConditions().getAll().get(0).getValue());
		orderOfferBean.setDiscountValue(offerEntity.getOfferApplicationRules().getEvent().getParams().getDiscountValue());
		orderOfferBean.setDiscountType(offerEntity.getOfferApplicationRules().getEvent().getParams().getDiscountType());
	}

	@GetMapping("/orders/order-offer")
	public ResponseEntity<PageAndSortResult<OrderOfferBean>> getAllOrderOffers(@RequestParam(defaultValue = "1") Integer pageNo,
			@RequestParam(defaultValue = "10") Integer pageSize, @RequestParam(required = false) String skuCode) {
		Map<String, PageAndSortRequest.SortDirection> sort = buildSortMap("active", PageAndSortRequest.SortDirection.DESC);
		Map<String, Object> filters = new HashMap<>();
		if (skuCode != null) {
			filters.put("skuCode", skuCode);
		}
		PageAndSortResult<OrderOfferEntity> orderOfferEntitiesPage = orderOfferService.findAllPagedRecords(filters, sort, pageSize, pageNo);
		PageAndSortResult<OrderOfferBean> orderOfferBeans = prepareOrderOfferResponsePageData(orderOfferEntitiesPage);
		return ResponseEntity.ok().body(orderOfferBeans);
	}

	public PageAndSortResult<OrderOfferBean> prepareOrderOfferResponsePageData(PageAndSortResult<OrderOfferEntity> sourceEntity) {
		PageAndSortResult<OrderOfferBean> result = new PageAndSortResult<>();
		result.setPageNo(sourceEntity.getPageNo());
		result.setPages(sourceEntity.getPages());
		result.setPageSize(sourceEntity.getPageSize());
		result.setTotal(sourceEntity.getTotal());
		List<OrderOfferBean> resultList = getMapper().mapAsList(sourceEntity.getData(), OrderOfferBean.class);
		for (int i = 0; i < resultList.size(); i++) {
			buildOrderOfferResponse(sourceEntity.getData().get(i), resultList.get(i));
		}
		result.setData(resultList);
		return result;
	}

}
