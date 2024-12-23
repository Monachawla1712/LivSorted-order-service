package com.sorted.rest.services.order.services;

import com.sorted.rest.common.beans.ErrorBean;
import com.sorted.rest.common.dbsupport.crud.BaseCrudRepository;
import com.sorted.rest.common.properties.Errors;
import com.sorted.rest.common.utils.CollectionUtils;
import com.sorted.rest.common.websupport.base.BaseService;
import com.sorted.rest.services.order.beans.MasterSkuBean;
import com.sorted.rest.services.order.beans.SkusPurchaseCostUploadBean;
import com.sorted.rest.services.order.clients.ClientService;
import com.sorted.rest.services.order.entity.OrderItemEntity;
import com.sorted.rest.services.order.entity.SkusPurchaseCostEntity;
import com.sorted.rest.services.order.repository.OrderItemRepository;
import com.sorted.rest.services.order.repository.SkusPurchaseCostRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Created by Abhishek Rai.
 */
@Service
public class SkusPurchaseCostService implements BaseService<SkusPurchaseCostEntity> {

	@Autowired
	private SkusPurchaseCostRepository skusPurchaseCostRepository;

	@Autowired
	private ClientService clientService;

	public List<SkusPurchaseCostUploadBean> preProcessSkusPurchaseCostUpload(List<SkusPurchaseCostUploadBean> rawBeans) {
		return sanitizeSkusPurchaseCostUpload(rawBeans);
	}

	private List<SkusPurchaseCostUploadBean> sanitizeSkusPurchaseCostUpload(List<SkusPurchaseCostUploadBean> rawBeans) {
		Set<String> skuCodes = rawBeans.stream().map(SkusPurchaseCostUploadBean::getSkuCode).collect(Collectors.toSet());
		List<MasterSkuBean> skus = clientService.getMasterSkus(skuCodes);
		Set<String> wmsSkus = skus.stream().map(MasterSkuBean::getSkuCode).collect(Collectors.toSet());

		Set<String> processedKey = new HashSet<>();
		rawBeans.forEach(bean -> {
			if (!wmsSkus.contains(bean.getSkuCode())) {
				bean.getErrors().add(ErrorBean.withError(Errors.NO_DATA_FOUND, String.format("SkuCode - %s not found", bean.getSkuCode()), "skuCode"));
			} else if (processedKey.contains(bean.computedKey())) {
				bean.getErrors().add(ErrorBean.withError(Errors.UNIQUE_VALUE, String.format("Duplicate - %s", bean.getSkuCode()), "skuCode"));
			} else {
				processedKey.add(bean.computedKey());
			}
		});
		return rawBeans;
	}

	public void validateSkusPurchaseCostOnUpload(SkusPurchaseCostUploadBean bean, org.springframework.validation.Errors errors) {
		if (!errors.hasErrors()) {
			if (Objects.isNull(bean.getSkuCode())) {
				bean.getErrors().add(ErrorBean.withError(Errors.MANDATORY, "Sku not found", "skuCode"));
			}
//			if (!Objects.isNull(bean.getAvgCostPrice()) && bean.getAvgCostPrice() <= 0) {
//				bean.getErrors().add(ErrorBean.withError(Errors.MIN, "Cost can't be negative and zero", "cost"));
//			}
//			if (!Objects.isNull(bean.getPurchaseQuantity()) && bean.getPurchaseQuantity() <= 0) {
//				bean.getErrors().add(ErrorBean.withError(Errors.MIN, "Quantity can't be negative and zero", "cost"));
//			}
			if (CollectionUtils.isNotEmpty(bean.getErrors())) {
				errors.reject("_ERRORS", "Uploaded Data Error(s)");
			}
		}
	}

	@Transactional(propagation = Propagation.REQUIRED)
	public void bulkSave(List<SkusPurchaseCostEntity> entityList) {
		List<SkusPurchaseCostEntity> skusPurchaseCostEntities = skusPurchaseCostRepository.findByDeliveryDate(entityList.get(0).getDeliveryDate());
		Map<String, SkusPurchaseCostEntity> skusPurchaseCostEntityMap = skusPurchaseCostEntities.stream()
				.collect(Collectors.toMap(SkusPurchaseCostEntity::getSkuCode, Function.identity()));
		if (CollectionUtils.isEmpty(skusPurchaseCostEntities)) {
			skusPurchaseCostRepository.saveAll(entityList);
			return;
		}
		List<SkusPurchaseCostEntity> skusPurchaseCost = new ArrayList<>();
		for (SkusPurchaseCostEntity entity : entityList) {
			if (skusPurchaseCostEntityMap.containsKey(entity.getSkuCode())) {
				SkusPurchaseCostEntity existingDb = skusPurchaseCostEntityMap.get(entity.getSkuCode());
				if (entity.getAvgCostPrice() != null && entity.getAvgCostPrice() > 0) {
					existingDb.setAvgCostPrice(entity.getAvgCostPrice());
				}
				if (entity.getPurchaseQuantity() != null && entity.getPurchaseQuantity() > 0) {
					existingDb.setPurchaseQuantity(entity.getPurchaseQuantity());
				}
				if (entity.getMrp() != null && entity.getMrp() > 0) {
					existingDb.setMrp(entity.getMrp());
				}
				skusPurchaseCost.add(existingDb);
			} else {
				skusPurchaseCost.add(entity);
			}
		}
		skusPurchaseCostRepository.saveAll(skusPurchaseCost);
	}

	@Override
	public Class<SkusPurchaseCostEntity> getEntity() {
		return SkusPurchaseCostEntity.class;
	}

	@Override
	public BaseCrudRepository getRepository() {
		return skusPurchaseCostRepository;
	}

}