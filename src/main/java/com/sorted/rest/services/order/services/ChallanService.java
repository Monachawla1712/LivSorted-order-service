package com.sorted.rest.services.order.services;

import com.sorted.rest.common.beans.ErrorBean;
import com.sorted.rest.common.exceptions.ServerException;
import com.sorted.rest.common.logging.AppLogger;
import com.sorted.rest.common.logging.LoggingManager;
import com.sorted.rest.common.properties.Errors;
import com.sorted.rest.common.utils.ParamsUtils;
import com.sorted.rest.services.common.upload.AWSUploadService;
import com.sorted.rest.services.order.beans.*;
import com.sorted.rest.services.order.clients.ClientService;
import com.sorted.rest.services.order.constants.FranchiseOrderConstants;
import com.sorted.rest.services.order.entity.FranchiseOrderEntity;
import com.sorted.rest.services.order.entity.FranchiseOrderItemEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;

@Service
public class ChallanService {

	private AppLogger _LOGGER = LoggingManager.getLogger(ChallanService.class);

	@Autowired
	private AWSUploadService awsUploadService;

	@Autowired
	private ClientService clientService;

	public ChallanDataBean buildChallanBeanData(FranchiseOrderEntity franchiseOrderEntity, StoreResponse store) {
		ChallanDataBean challanDataBean = new ChallanDataBean();
		challanDataBean.setOrderId(franchiseOrderEntity.getId());
		challanDataBean.setStoreName(store.getName());
		challanDataBean.setStoreId(Integer.valueOf(franchiseOrderEntity.getStoreId()));
		StoreDataResponse storeResponse = clientService.getStoreDataFromId(franchiseOrderEntity.getStoreId());
		if (storeResponse != null) {
			challanDataBean.setStoreData(storeResponse);
			UserServiceResponse ownerDetails = clientService.getUserDetailsFromCustomerId(storeResponse.getOwnerId());
			AmStoreDetailsResponse amDetails = clientService.getStoreDetailsFromStoreId(franchiseOrderEntity.getStoreId());
			challanDataBean.setOwnerDetails(ownerDetails);
			if (amDetails != null) {
				challanDataBean.setAmDetails(amDetails.getUser());
			}
		}
		challanDataBean.setDate(new java.sql.Date(franchiseOrderEntity.getDeliveryDate().getTime()));
		challanDataBean.setIsSrpStore(franchiseOrderEntity.getIsSrpStore());
		sortOrderItemsByStatus(franchiseOrderEntity);
		challanDataBean.setOrderItems(franchiseOrderEntity.getOrderItems());
		challanDataBean.setDisplayOrderId(franchiseOrderEntity.getDisplayOrderId());
		challanDataBean.setFileName(getPdfNameWithDate(challanDataBean));
		challanDataBean.setOrderStatus(franchiseOrderEntity.getStatus());
		challanDataBean.setFinalBillAmount(franchiseOrderEntity.getFinalBillAmount());
		challanDataBean.setTotalSpGrossAmount(franchiseOrderEntity.getTotalSpGrossAmount());
		if (franchiseOrderEntity.getOfferData() != null && franchiseOrderEntity.getOfferData()
				.getIsOfferApplied() != null && franchiseOrderEntity.getOfferData().getAmount() != null) {
			challanDataBean.setOfferDiscountAmount(franchiseOrderEntity.getOfferData().getAmount());
		} else {
			challanDataBean.setOfferDiscountAmount(0d);
		}
		WalletBean walletBean = clientService.getStoreWallet(franchiseOrderEntity.getStoreId());
		challanDataBean.setOutstandingAmount(walletBean.getAmount() < 0 ? walletBean.getAmount() * -1 : 0);
		return challanDataBean;
	}

	private void sortOrderItemsByStatus(FranchiseOrderEntity franchiseOrder) {
		franchiseOrder.getOrderItems().sort((o1, o2) -> {
			if (o1.getStatus() == FranchiseOrderConstants.FranchiseOrderItemStatus.PENDING && o2.getStatus() != FranchiseOrderConstants.FranchiseOrderItemStatus.PENDING) {
				return 1;
			} else if (o1.getStatus() != FranchiseOrderConstants.FranchiseOrderItemStatus.PENDING && o2.getStatus() == FranchiseOrderConstants.FranchiseOrderItemStatus.PENDING) {
				return -1;
			}
			return 0;
		});
	}

	private void setPackedItemsAmount(ChallanDataBean challanDataBean, FranchiseOrderEntity franchiseOrderEntity) {
		Double packedFinalBillAmount = 0d;
		Double packedTotalSpGrossAmount = 0d;
		for (FranchiseOrderItemEntity franchiseOrderItem : franchiseOrderEntity.getOrderItems()) {
			packedFinalBillAmount += franchiseOrderItem.getFinalAmount();
			packedTotalSpGrossAmount += franchiseOrderItem.getSpGrossAmount();
		}
		challanDataBean.setFinalBillAmount(packedFinalBillAmount);
		challanDataBean.setTotalSpGrossAmount(packedTotalSpGrossAmount);
	}

	private String getPdfNameWithDate(ChallanDataBean challanDataBean) {
		String filename = challanDataBean.getStoreId().toString().concat("-").concat(challanDataBean.getDisplayOrderId()).concat("-")
				.concat(String.valueOf(Instant.now().toEpochMilli())).concat(".pdf");
		return filename;
	}

	public void uploadChallan(ChallanDataBean challanDataBean) {
		String bucketName = ParamsUtils.getParam("SORTED_FILES_BUCKET_NAME");
		String subDirectory = ParamsUtils.getParam("CHALLAN_FILES_SUBDIRECTORY");
		File directoryPath = new File(System.getProperty("user.dir"));
		File files[] = directoryPath.listFiles();
		try {
			for (File file : files) {
			if (file.getName().endsWith(".pdf")) {
					_LOGGER.info(
							String.format("Uploading Challan - %s to s3 for order - %s", challanDataBean.getFileName(), challanDataBean.getDisplayOrderId()));
				byte[] fileBytes = Files.readAllBytes(Path.of(directoryPath.getPath().concat("/").concat(file.getName())));
				Object response = awsUploadService.uploadFile(bucketName, subDirectory, fileBytes, challanDataBean.getFileName());
				challanDataBean.setChallanUrl(ParamsUtils.getParam("CLOUDFRONT_URL").concat("/").concat(response.toString()));
				file.delete();
				}
			}
		} catch (IOException err) {
			_LOGGER.error("challan Upload error", err);
			throw new ServerException(new ErrorBean(Errors.UPDATE_FAILED, "Error while uploading challan", "challan"));
		}
	}
}
