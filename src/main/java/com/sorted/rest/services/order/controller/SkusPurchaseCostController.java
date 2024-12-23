package com.sorted.rest.services.order.controller;

import com.sorted.rest.common.beans.ErrorBean;
import com.sorted.rest.common.exceptions.ValidationException;
import com.sorted.rest.common.logging.AppLogger;
import com.sorted.rest.common.logging.LoggingManager;
import com.sorted.rest.common.utils.BeanValidationUtils;
import com.sorted.rest.common.utils.CollectionUtils;
import com.sorted.rest.common.utils.SessionUtils;
import com.sorted.rest.common.websupport.base.BaseController;
import com.sorted.rest.services.common.mapper.BaseMapper;
import com.sorted.rest.services.common.upload.csv.CSVBulkRequest;
import com.sorted.rest.services.common.upload.csv.CsvUploadResult;
import com.sorted.rest.services.common.upload.csv.CsvUtils;
import com.sorted.rest.services.order.beans.SkusPurchaseCostUploadBean;
import com.sorted.rest.services.order.entity.SkusPurchaseCostEntity;
import com.sorted.rest.services.order.services.SkusPurchaseCostService;
import io.swagger.annotations.ApiOperation;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.Errors;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.sql.Date;
import java.util.*;

/**
 * Created by Abhishek Rai.
 */
@RestController
@RequestMapping("")
public class SkusPurchaseCostController implements BaseController {

	private AppLogger _LOGGER = LoggingManager.getLogger(SkusPurchaseCostController.class);

	@Autowired
	private BaseMapper<?, ?> mapper;

	@Autowired
	private SkusPurchaseCostService skusPurchaseCostService;

	@ApiOperation(value = "upload Skus Purchase Cost.", nickname = "uploadSkusPurchaseCost")
	@PostMapping("/orders/skus-purchase-cost/upload")
	public CsvUploadResult<SkusPurchaseCostUploadBean> uploadSkusPurchaseCost(@RequestParam("file") MultipartFile file, @RequestParam Date date) {
		final int maxAllowedRows = 1000;
		final String module = "skus-cost";
		List<SkusPurchaseCostUploadBean> rawBeans = CsvUtils.getBeans(module, file, maxAllowedRows, SkusPurchaseCostUploadBean.newInstance());
		rawBeans.forEach(data -> {
			data.setDeliveryDate(date);
		});
		List<SkusPurchaseCostUploadBean> response = skusPurchaseCostService.preProcessSkusPurchaseCostUpload(rawBeans);
		CsvUploadResult<SkusPurchaseCostUploadBean> result = validateSkusPurchaseCostUpload(response);
		result.setHeaderMapping(response.get(0).getHeaderMapping());
		CsvUtils.saveBulkRequestData(SessionUtils.getAuthUserId(), module, result);
		return result;
	}

	private CsvUploadResult<SkusPurchaseCostUploadBean> validateSkusPurchaseCostUpload(List<SkusPurchaseCostUploadBean> beans) {
		final CsvUploadResult<SkusPurchaseCostUploadBean> result = new CsvUploadResult<>();
		if (CollectionUtils.isNotEmpty(beans)) {
			beans.forEach(bean -> {
				try {
					Errors errors = getSpringErrors(bean);
					skusPurchaseCostService.validateSkusPurchaseCostOnUpload(bean, errors);
					checkError(errors);
					result.addSuccessRow(bean);
				} catch (final Exception e) {
					if (CollectionUtils.isEmpty(bean.getErrors())) {
						_LOGGER.error(e.getMessage(), e);
						final List<ErrorBean> errors = e instanceof ValidationException ?
								BeanValidationUtils.prepareValidationResponse((ValidationException) e).getErrors() :
								Arrays.asList(ErrorBean.withError("ERROR", e.getMessage(), null));
						bean.addErrors(errors);
						_LOGGER.error("Franchise Store Inventory Uploaded data is having error =>" + errors.toString());
					}
					result.addFailedRow(bean);
				}
			});
		}
		return result;
	}

	@ApiOperation(value = "Save Skus Purchase Cost CSV", nickname = "saveSkusPurchaseCost")
	@PostMapping("/orders/skus-purchase-cost/upload/save")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void saveSkusPurchaseCost(@RequestParam(name = "key", required = true) String key, @RequestParam(name = "cancel", required = false) Integer cancel) {
		final boolean cleanup = cancel != null;
		if (cleanup) {
			cancelUpload(key);
		} else {
			saveSkusPurchaseCostBulkData(key);
		}
	}

	public void cancelUpload(String key) {
		final int deleteCount = CsvUtils.cancelUpload(key);
		_LOGGER.info(String.format("Upload Cancel called with Key = %s and delete count is = %s", key, deleteCount));
		if (deleteCount <= 0) {
			throw new ValidationException(ErrorBean.withError("UPLOAD_CANCEL_ERROR", "Unable to cancel bulk upload request.", null));
		}
	}

	@Transactional
	public void saveSkusPurchaseCostBulkData(String key) {
		final CSVBulkRequest<SkusPurchaseCostUploadBean> uploadedData = CsvUtils.getBulkRequestData(key, SkusPurchaseCostUploadBean.class);
		if (uploadedData != null && CollectionUtils.isNotEmpty(uploadedData.getData())) {
			List<SkusPurchaseCostEntity> entityList = buildSkusPurchaseCostEntity(uploadedData.getData());
			skusPurchaseCostService.bulkSave(entityList);
			CsvUtils.markUploadProcessed(key);
		} else {
			throw new ValidationException(ErrorBean.withError("UPLOAD_ERROR", "Uploaded data not found or it is expired.", null));
		}
	}

	private List<SkusPurchaseCostEntity> buildSkusPurchaseCostEntity(List<SkusPurchaseCostUploadBean> uploadedData) {
		Map<String, SkusPurchaseCostEntity> beanMap = new HashMap<>();
		uploadedData.forEach(bean -> {
			SkusPurchaseCostEntity entity = null;
			SkusPurchaseCostEntity skuEntity = buildSkusPurchaseCostEntity(bean);
			beanMap.put(skuEntity.getSkuCode(), skuEntity);
		});
		return new ArrayList<SkusPurchaseCostEntity>(beanMap.values());
	}

	private SkusPurchaseCostEntity buildSkusPurchaseCostEntity(SkusPurchaseCostUploadBean bean) {
		final SkusPurchaseCostEntity skuEntity = SkusPurchaseCostEntity.newInstance();
		skuEntity.setSkuCode(bean.getSkuCode());
		skuEntity.setDeliveryDate((bean.getDeliveryDate()));
		if (StringUtils.isNotEmpty(bean.getPurchaseQuantity())) {
			skuEntity.setPurchaseQuantity(Double.valueOf(bean.getPurchaseQuantity()));
		}
		if (StringUtils.isNotEmpty(bean.getAvgCostPrice())) {
			skuEntity.setAvgCostPrice(Double.valueOf(bean.getAvgCostPrice()));
		}
		if (StringUtils.isNotEmpty(bean.getMrp())) {
			skuEntity.setMrp(Double.valueOf(bean.getMrp()));
		}
		return skuEntity;
	}

	@Override
	public BaseMapper<?, ?> getMapper() {
		return mapper;
	}

}
