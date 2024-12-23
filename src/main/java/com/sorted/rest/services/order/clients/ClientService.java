package com.sorted.rest.services.order.clients;

import com.sorted.rest.common.beans.ErrorBean;
import com.sorted.rest.common.exceptions.ServerException;
import com.sorted.rest.common.exceptions.ValidationException;
import com.sorted.rest.common.logging.AppLogger;
import com.sorted.rest.common.logging.LoggingManager;
import com.sorted.rest.common.properties.Errors;
import com.sorted.rest.common.utils.DateUtils;
import com.sorted.rest.services.order.beans.*;
import com.sorted.rest.services.order.entity.OrderEntity;
import feign.FeignException;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import javax.validation.Valid;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author mohitaggarwal
 */
@Service
public class ClientService {

	AppLogger _LOGGER = LoggingManager.getLogger(ClientService.class);

	@Autowired
	private StoreInventoryClient storeInventoryClient;

	@Autowired
	private StoreClient storeClient;

	@Autowired
	private AuthConsumerClient authConsumerClient;

	@Autowired
	private OfferClient offerClient;

	@Autowired
	private OrderIntegrationClient orderIntegrationClient;

	@Autowired
	private PaymentClient paymentClient;

	@Autowired
	private WmsClient wmsClient;

	@Autowired
	private NotificationClient notificationClient;

	@Autowired
	private TicketClient ticketsClient;

	@Autowired
	private ClevertapClient clevertapClient;

	@Value("${client.clevertap.account-id}")
	@Getter
	private String cleverTapAccountId;

	@Value("${client.clevertap.passcode}")
	@Getter
	private String cleverTapPasscode;

	@Value("${client.wms.auth_key}")
	@Getter
	private String RZ_AUTH_VALUE;

	public StoreInventoryResponse getStoreInventory(String storeId, String skuCodes, UUID userId) {
		StoreInventoryResponse storeItem = null;
		try {
			storeItem = storeInventoryClient.getStoreInventory(storeId, skuCodes, userId, null);
		} catch (Exception e) {
			_LOGGER.error("Error while fetching StoreInventory", e);
			throw new ValidationException(new ErrorBean(Errors.SERVER_EXCEPTION, "Something went wrong. We are trying to reload your cart. Kindly try again."));
		}
		return storeItem;
	}

	public StoreInventoryResponse getStoreInventory(String storeId, String skuCodes, UUID userId, String appVersion) {
		StoreInventoryResponse storeItem = null;
		try {
			storeItem = storeInventoryClient.getStoreInventory(storeId, skuCodes, userId, appVersion);
		} catch (Exception e) {
			_LOGGER.error("Error while fetching StoreInventory", e);
			throw new ValidationException(new ErrorBean(Errors.SERVER_EXCEPTION, "Something went wrong. We are trying to reload your cart. Kindly try again."));
		}
		return storeItem;
	}

	public ConsumerAddressResponse getConsumerAddressById(Long addressId) {
		ConsumerAddressResponse address = null;
		try {
			address = authConsumerClient.getAddressById(addressId);
		} catch (Exception e) {
			_LOGGER.error("Error while fetching Consumer Address", e);
			throw new ValidationException(new ErrorBean(Errors.SERVER_EXCEPTION, "Something went wrong while fetching Consumer Address."));
		}
		return address;
	}

	public StoreInventoryUpdateResponse verifyAndDeductStoreInventory(String storeId, StoreInventoryUpdateRequest request) {
		StoreInventoryUpdateResponse response = null;
		try {
			response = storeInventoryClient.verifyAndDeductStoreInventory(storeId, request);
		} catch (Exception e) {
			_LOGGER.error("Error while fetching StoreInventory", e);
			throw new ValidationException(new ErrorBean(Errors.SERVER_EXCEPTION, "Something went wrong. We are trying to reload your cart. Kindly try again."));
		}
		return response;
	}

	public StoreInventoryUpdateResponse addOrDeductStoreInventory(String storeId, StoreInventoryAddOrDeductRequest request) {
		StoreInventoryUpdateResponse response = null;
		try {
			response = storeInventoryClient.addOrDeductStoreInventory(storeId, request);
		} catch (Exception e) {
			_LOGGER.error("Error while fetching StoreInventory", e);
			throw new ValidationException(new ErrorBean(Errors.SERVER_EXCEPTION, "Something went wrong. We are trying to reload your cart. Kindly try again."));
		}
		return response;
	}

	public StoreDataResponse getStoreDataFromId(@Valid String storeId) {
		StoreDataResponse response = null;
		try {
			response = storeClient.getStoreDataFromId(storeId).get(0);
			if (response == null || response.getStoreId() == null) {
				throw new ValidationException(new ErrorBean("store_not_found", "We are unable to locate the store"));
			}
		} catch (Exception e) {
			_LOGGER.error("Error while fetching StoreFromId", e);
			if (e instanceof ValidationException) {
				throw e;
			} else {
				throw new ServerException(new ErrorBean(Errors.SERVER_EXCEPTION, "Something went wrong while fetching store details"));
			}
		}
		return response;
	}

	public OfferResponse getOfferResponse(OfferClientApplyOfferRequest req) {
		OfferResponse response = null;
		try {
			response = offerClient.getOfferResponse(req);
		} catch (Exception e) {
			_LOGGER.error("Error while working on offers", e);
		}
		return response;
	}

	public FranchiseOfferResponse getFranchiseOfferResponse(OfferClientApplyFranchiseOfferRequest req) {
		FranchiseOfferResponse response = null;
		try {
			response = offerClient.getFranchiseOfferResponse(req);
		} catch (Exception e) {
			_LOGGER.error("Error while working on offers", e);
		}
		return response;
	}

	public Boolean sendOrderToLithos(LithosOrderBean lithosOrder) {
		try {
			return orderIntegrationClient.sendOrderToLithos(lithosOrder).getSuccess();
		} catch (Exception e) {
			_LOGGER.error("Error while sending Order To Lithos", e);
		}
		return false;
	}

	public Boolean sendPaymentUpdateToLithos(LithosOrderBean lithosOrder) {
		try {
			return orderIntegrationClient.sendPaymentUpdateToLithos(lithosOrder).getSuccess();
		} catch (Exception e) {
			_LOGGER.error("Error while making payment update", e);
		}
		return false;
	}

	public void sendLithosPaymentUpdate(PaymentNotifyBean payment) {
		try {
			paymentClient.sendLithosPaymentUpdate(payment);
		} catch (Exception e) {
			_LOGGER.error("Error while sending Lithos Payment Update", e);
			throw new ServerException(new ErrorBean(Errors.SERVER_EXCEPTION, "Something went wrong while sending Lithos Payment Update."));
		}
	}

	public void sendPaymentUpdate(PaymentNotifyBean payment) {
		try {
			paymentClient.sendPaymentUpdate(payment);
		} catch (Exception e) {
			_LOGGER.error("Error while sending Lithos Payment Update", e);
		}
	}

	public WalletBean getUserWallet(String customerId) {
		try {
			return paymentClient.getUserWallet(customerId);
		} catch (Exception e) {
			_LOGGER.error("Error while getting wallet detail", e);
			throw new ServerException(new ErrorBean(Errors.SERVER_EXCEPTION, "Something went wrong while getting wallet."));
		}
	}

	public WalletAddOrDeductBean addOrDeductFromWallet(String customerId, WalletAddOrDeductBean payload, String key) {
		try {
			return paymentClient.addOrDeductFromWallet(customerId, payload, key);
		} catch (Exception e) {
			_LOGGER.error("Error while addOrDeduct money", e);
			throw new ServerException(new ErrorBean(Errors.SERVER_EXCEPTION, "Something went wrong while adding or deducting money."));
		}
	}

	public void updateLithosOrderAndStatusToPPD(String orderId, PpdLithosUpdateBean payload) {
		try {
			wmsClient.updateLithosOrderAndStatusToPPD(getWmsHeaderMap(), orderId, payload);
		} catch (Exception e) {
			_LOGGER.error("Error while updating Lithos orderId and status in PPD", e);
			// throw new ServerException(new ErrorBean(Errors.SERVER_EXCEPTION, "Something
			// went wrong while updating Lithos orderId and status."));
		}
	}

	public FranchiseStoreInventoryResponse getFranchiseStoreInventory(String storeId, String skuCode) {
		FranchiseStoreInventoryResponse storeItem = null;
		try {
			storeItem = wmsClient.getFranchiseStoreInventory(getWmsHeaderMap(), Integer.parseInt(storeId), skuCode);
		} catch (Exception e) {
			_LOGGER.error("Error while fetching StoreInventory", e);
			throw new ValidationException(new ErrorBean(Errors.SERVER_EXCEPTION, "Something went wrong. We are trying to reload your cart. Kindly try again."));
		}
		return storeItem;
	}

	public List<FranchiseStoreInventoryResponse> getStoreSkuInventoryForBulkRequest(Set<String> skuCodes, String storeId) {
		List<FranchiseStoreInventoryResponse> storeItem = null;
		try {
			storeItem = wmsClient.getStoreSkuInventoryForBulkRequest(getWmsHeaderMap(), skuCodes, storeId);
		} catch (FeignException.FeignClientException f) {
			if (f.status() == 404) {
				throw new ValidationException(new ErrorBean("warehouse_not_found", String.format("no warehouse is mapped to StoreId = %s", storeId)));
			} else {
				_LOGGER.error("Error while fetching StoreInventory", f);
				throw new ValidationException(new ErrorBean(Errors.SERVER_EXCEPTION, "Something went wrong while fetch data from warehouse"));
			}
		} catch (Exception e) {
			_LOGGER.error("Error while fetching StoreInventory", e);
			throw new ValidationException(new ErrorBean(Errors.SERVER_EXCEPTION, "Something went wrong while fetch data from warehouse"));
		}
		return storeItem;
	}

	public void saveFranchiseStoreInventory(Integer whId, String skuCode, FranchiseStoreInventoryAddOrDeductRequest request) {
		try {
			wmsClient.addOrDeductFranchiseStoreInventory(getWmsHeaderMap(), whId, skuCode, request);
		} catch (Exception e) {
			_LOGGER.error("Error while Updating FranchiseStoreInventory", e);
			throw new ValidationException(new ErrorBean(Errors.SERVER_EXCEPTION, "Something went wrong. We are trying to reload your cart. Kindly try again."));
		}
	}

	public void saveFranchiseStoreInventoryForBulkRequest(List<FranchiseCartRequest> orderItems) {
		try {
			wmsClient.deductFranchiseStoreInventoryForBulkRequest(getWmsHeaderMap(), orderItems);
		} catch (Exception e) {
			_LOGGER.error("Error while Updating FranchiseStoreInventories", e);
			throw new ValidationException(new ErrorBean(Errors.SERVER_EXCEPTION, "Something went wrong. We are trying to reload your cart. Kindly try again."));
		}
	}

	public WalletBean getStoreWallet(String storeId) {
		try {
			return paymentClient.getStoreWallet(storeId);
		} catch (Exception e) {
			_LOGGER.error("Error while getting wallet detail", e);
			throw new ServerException(new ErrorBean(Errors.SERVER_EXCEPTION, "Something went wrong while getting wallet details."));
		}
	}

	public void addOrDeductFromStoreWallet(String storeId, WalletAddOrDeductBean walletDeductBean, String key) {
		try {
			paymentClient.addOrDeductFromStoreWallet(storeId, walletDeductBean, key);
		} catch (Exception e) {
			_LOGGER.error("Error while deducting money", e);
			throw new ServerException(new ErrorBean(Errors.SERVER_EXCEPTION, "Something went wrong while deducting money."));
		}
	}

	public void uploadFranchiseStoreRequisition(List<FranchiseSRBean> beans) {
		FranchiseSRRequest franchiseSRRequest = new FranchiseSRRequest();
		franchiseSRRequest.setData(beans);
		try {
			wmsClient.updateFranchiseStoreRequisitions(getWmsHeaderMap(), franchiseSRRequest);
		} catch (Exception e) {
			_LOGGER.error("Error while updating store requisition table ", e);
		}
	}

	public UserServiceResponse getUserDetailsFromCustomerId(UUID customerId) {
		String customerIdString = customerId.toString();
		Map<String, Object> headerMap = new HashMap<>();
		try {
			return authConsumerClient.getUserDetailsFromCustomerId(headerMap, customerIdString);
		} catch (Exception e) {
			_LOGGER.error(String.format("Error while getting userDetails for customerId  ", customerIdString), e);
			return null;
		}
	}

	public StoreResponse fetchWmsStoreDetails(String storeId) {
		try {
			return wmsClient.fetchWmsStoreDetails(getWmsHeaderMap(), storeId);
		} catch (Exception e) {
			_LOGGER.error("Error while fetching store details", e);
			throw new ServerException(new ErrorBean(Errors.SERVER_EXCEPTION, "Something went wrong while fetching store details."));
		}
	}

	public void sendPushNotifications(List<PnRequest> request) {
		try {
			notificationClient.sendPushNotifications(request);
		} catch (Exception e) {
			_LOGGER.error("Error while sending pn ", e);
		}
	}

	public List<StoreResponse> fetchStoresDetails(Set<String> storeIds) {
		List<StoreResponse> response = new ArrayList<>();
		Set<Integer> filteredStoreIds = storeIds.stream().filter(e -> e.matches("\\d+")).map(e -> Integer.parseInt(e)).collect(Collectors.toSet());
		if (!filteredStoreIds.isEmpty()) {
			try {
				response = wmsClient.fetchStoresDetails(getWmsHeaderMap(), filteredStoreIds);
			} catch (Exception e) {
				_LOGGER.error("Error while fetching stores details", e);
				throw new ServerException(new ErrorBean(Errors.SERVER_EXCEPTION, "Something went wrong while fetching stores details."));
			}
		}
		return response;
	}

	public List<WalletStatementBean> fetchWalletStatementByTxnDetail(String txnDetail) {
		try {
			return paymentClient.fetchWalletStatementByTxnDetail(txnDetail);
		} catch (Exception e) {
			_LOGGER.error("Error while Fetching wallet statement", e);
			throw new ServerException(new ErrorBean(Errors.SERVER_EXCEPTION, "Something went wrong while fetching wallet statement."));
		}
	}

	public List<WarehouseInventoryResponseBean> fetchWarehouseInventoryDetails(Integer whId, Set<String> skuCodes) {
		try {
			return wmsClient.fetchWarehouseInventoryDetails(getWmsHeaderMap(), whId, skuCodes).getBody();
		} catch (FeignException.FeignClientException f) {
			if (f.status() == 400) {
				throw new ValidationException(new ErrorBean("Inventory not Available.", f.getMessage()));
			} else {
				_LOGGER.error("Error fetching inventory from Warehouse", f);
				throw new ValidationException(new ErrorBean(Errors.SERVER_EXCEPTION, "Something went wrong while fetching inventory from Warehouse."));
			}
		} catch (Exception e) {
			_LOGGER.error("Error fetching inventory from Warehouse", e);
			throw new ServerException(new ErrorBean(Errors.SERVER_EXCEPTION, "Something went wrong while fetching inventory from Warehouse."));
		}
	}

	public void deductWhRejectionInventory(DeductWhRejectionRequest deductRejectionInventoryBean) {
		try {
			wmsClient.deductRejectionInventory(getWmsHeaderMap(), deductRejectionInventoryBean);
		} catch (Exception e) {
			_LOGGER.error("Error updating rejection quantity in Warehouse.", e);
			throw new ServerException(new ErrorBean(Errors.SERVER_EXCEPTION, e.getMessage()));
		}
	}

	public void cancelSR(UUID orderId) {
		try {
			wmsClient.cancelSR(getWmsHeaderMap(), orderId);
		} catch (Exception e) {
			_LOGGER.error(String.format("Error while cancelling the SR orderId : %s ", orderId), e);
			throw new ServerException(new ErrorBean(Errors.SERVER_EXCEPTION, String.format("Error while cancelling the SR orderId : %s ", orderId)));
		}
	}

	public String getUserPhoneFromUserId(UUID userId) {
		UserServiceResponse userServiceResponse = getUserDetailsFromCustomerId(userId);
		if (userServiceResponse == null || StringUtils.isBlank(userServiceResponse.getPhoneNumber()) || userServiceResponse.getPhoneNumber().equals("null")) {
			_LOGGER.error(String.format("Unable to find phoneNumber for customerId - ", userId));
			return null;
		}
		return userServiceResponse.getPhoneNumber();
	}

	@Deprecated
	public ClevertapEventResponse sendClevertapEvent_dep(ClevertapEventRequest clevertapEventRequest) {
		Map<String, Object> headerMap = new HashMap<>();
		headerMap.put("X-CleverTap-Account-Id", cleverTapAccountId);
		headerMap.put("X-CleverTap-Passcode", cleverTapPasscode);
		headerMap.put("Content-Type", "Content-Type:application/json");
		ClevertapEventResponse response = new ClevertapEventResponse();
		try {
			_LOGGER.info("Clevertap Events are paused");
//			response = clevertapClient.sendEvent(headerMap, clevertapEventRequest);
		} catch (Exception e) {
			_LOGGER.error(String.format("Error while sending clevertap request %s", clevertapEventRequest), e);
		}
		return response;
	}

	public void sendClevertapEvent(ClevertapEventRequest clevertapEventRequest) {
		try {
			notificationClient.sendClevertapEvent(clevertapEventRequest);
		} catch (Exception e) {
			_LOGGER.error(String.format("Error while sending clevertap request %s", clevertapEventRequest), e);
		}
	}

	public void sendClevertapProfileUpdate(String userId, Map<String, Object> profileData) {
		try {
			ClevertapEventRequest.ClevertapEventData clevertapEventData = buildClevertapProfileData(userId, profileData);
			ClevertapEventRequest clevertapEventRequest = new ClevertapEventRequest();
			List<ClevertapEventRequest.ClevertapEventData> clevertapOrderEventDataList = new ArrayList<>();
			clevertapOrderEventDataList.add(clevertapEventData);
			clevertapEventRequest.setD(clevertapOrderEventDataList);
			sendClevertapEvent(clevertapEventRequest);
		} catch (Exception e) {
			_LOGGER.info("Something went wrong while sending data to clevertap");
		}
	}

	public ClevertapEventRequest.ClevertapEventData buildClevertapProfileData(String userId, Map<String, Object> profileData) {
		ClevertapEventRequest.ClevertapEventData clevertapProfileUpdateDto = new ClevertapEventRequest.ClevertapEventData();
		clevertapProfileUpdateDto.setIdentity(userId);
		clevertapProfileUpdateDto.setType("profile");
		clevertapProfileUpdateDto.setProfileData(profileData);
		return clevertapProfileUpdateDto;
	}

	public ClevertapEventRequest.ClevertapEventData buildClevertapEventData(String eventName, UUID userId, Map<String, Object> eventData) {
		ClevertapEventRequest.ClevertapEventData clevertapEventData = new ClevertapEventRequest.ClevertapEventData();
		clevertapEventData.setTs(DateUtils.convertDateUtcToIst(new Date()).getTime() / 1000);
		clevertapEventData.setIdentity("+91" + getUserPhoneFromUserId(userId));
		clevertapEventData.setType("event");
		clevertapEventData.setEvtName(eventName);
		clevertapEventData.setEvtData(eventData);
		return clevertapEventData;
	}

	public void sendMultipleClevertapEvents(List<ClevertapEventRequest.ClevertapEventData> clevertapEventDataList) {
		try {
			ClevertapEventRequest clevertapEventRequest = new ClevertapEventRequest();
			clevertapEventRequest.setD(clevertapEventDataList);
			ClevertapEventResponse clevertapEventResponse = sendClevertapEvent_dep(clevertapEventRequest);
			_LOGGER.info(String.format("For request %s Response from Clevertap - %s", clevertapEventRequest, clevertapEventResponse));
		} catch (Exception e) {
			_LOGGER.info("Something went wrong while sending data to clevertap");
		}
	}

	public void sendSingleClevertapEvent(String eventName, UUID userId, Map<String, Object> eventData) {
		try {
			ClevertapEventRequest.ClevertapEventData clevertapEventData = buildClevertapEventData(eventName, userId, eventData);
			ClevertapEventRequest clevertapEventRequest = new ClevertapEventRequest();
			List<ClevertapEventRequest.ClevertapEventData> clevertapOrderEventDataList = new ArrayList<>();
			clevertapOrderEventDataList.add(clevertapEventData);
			clevertapEventRequest.setD(clevertapOrderEventDataList);
			ClevertapEventResponse clevertapEventResponse = sendClevertapEvent_dep(clevertapEventRequest);
			_LOGGER.info(String.format("For request %s Response from Clevertap - %s", clevertapEventRequest, clevertapEventResponse));
		} catch (Exception e) {
			_LOGGER.info("Something went wrong while sending data to clevertap");
		}
	}

	public List<MasterSkuBean> getMasterSkus(Set<String> skuCodes) {
		try {
			return wmsClient.getMasterSkus(getWmsHeaderMap(), skuCodes);
		} catch (Exception e) {
			_LOGGER.error(String.format("Error while fetching masterSkus "), e);
			throw new ServerException(new ErrorBean(Errors.SERVER_EXCEPTION, String.format("Error while fetching masterSkus")));
		}
	}

	private Map<String, Object> getWmsHeaderMap() {
		Map<String, Object> headerMap = new HashMap<>();
		headerMap.put("rz-auth-key", RZ_AUTH_VALUE);
		return headerMap;
	}

	public void sendZipFileLink(String zipLink, UUID userId) {
		NotificationServiceEmailRequest notificationServiceEmailRequest = new NotificationServiceEmailRequest();
		notificationServiceEmailRequest.setUserId(userId);
		notificationServiceEmailRequest.setTemplateName("INVOICE_GENERATED_EMAIL");
		Map<String, String> bb = new HashMap<>();
		bb.put("link", zipLink);
		notificationServiceEmailRequest.setFillers(bb);
		notificationClient.sendEmail(notificationServiceEmailRequest);
	}

	public FranchiseAutoApplyOfferResponse getAutoApplyCouponCode(OfferClientApplyFranchiseOfferRequest req) {
		FranchiseAutoApplyOfferResponse response = null;
		try {
			response = offerClient.getFranchiseAutoApplyCouponCode(req);
		} catch (Exception e) {
			_LOGGER.error("Error while fetching Auto Apply Coupon.", e);
		}
		return response;
	}

	public Set<UUID> fetchPendingTicketOrderIds(List<UUID> cashbackPendingOrderIds) {
		try {
			PendingOrderRefundTicketsRequest pendingRefundTicketsRequest = new PendingOrderRefundTicketsRequest();
			pendingRefundTicketsRequest.setOrderIds(cashbackPendingOrderIds);
			List<UUID> orderIdsList = ticketsClient.getPendingTicketsOrderIds(pendingRefundTicketsRequest).getOrderIds();
			return new HashSet<>(orderIdsList);
		} catch (Exception e) {
			_LOGGER.error("Error while fetching Pending Refund Ticket Order Ids.", e);
			throw new ServerException(new ErrorBean(Errors.SERVER_EXCEPTION, "Something went wrong while fetching Pending Refund Ticket Order Ids."));
		}
	}

	public List<StoreDataResponse> getStoresData(StoreSearchRequest request) {
		List<StoreDataResponse> response = new ArrayList<>();
		try {
			response = storeClient.getStoresData(request);
		} catch (Exception e) {
			_LOGGER.error("Error while fetching stores ", e);
			if (e instanceof ValidationException) {
				throw e;
			} else {
				throw new ServerException(new ErrorBean(Errors.SERVER_EXCEPTION, "Something went wrong while fetching stores"));
			}
		}
		return response;
	}

	public void makeCashCollectionEntry(CashCollectionRequest cashCollectionRequest) {
		try {
			paymentClient.makeCashCollectionEntry(cashCollectionRequest);
		} catch (Exception e) {
			_LOGGER.error("Error while cash collection", e);
			throw new ServerException(new ErrorBean(Errors.SERVER_EXCEPTION, "Something went wrong while cash collection."));
		}
	}

	public AuthServiceStoreDetailsBean getUserMappedStores(String userId) {
		AuthServiceStoreDetailsBean userMappedStores;
		try {
			userMappedStores = authConsumerClient.getFranchiseStoresV2(userId);
		} catch (Exception e) {
			_LOGGER.error("Error while fetching getUserMappedStores", e);
			if (e instanceof ValidationException) {
				throw e;
			} else {
				throw new ValidationException(new ErrorBean("stores_not_found", "Something went wrong. Kindly try again."));
			}
		}
		return userMappedStores;
	}

	public List<FranchiseStoreInventoryResponse> getFranchiseStoreInventoryForAllSkus() {
		List<FranchiseStoreInventoryResponse> storeItems = null;
		try {
			storeItems = wmsClient.getFranchiseStoreInventoryForAllSkus(getWmsHeaderMap());
		} catch (Exception e) {
			_LOGGER.error("Error while fetching Bulk StoreInventory", e);
			throw new ValidationException(new ErrorBean(Errors.SERVER_EXCEPTION, "Something went wrong while fetching Bulk StoreInventory."));
		}
		return storeItems;
	}

	public void sendWhatsappMessages(WhatsappSendMsgRequest request) {
		try {
			SendNotificationsResponse response = notificationClient.sendWhatsappMessages(request);
			_LOGGER.info(response.getMessage());
			if (!response.getSuccess()) {
				_LOGGER.info(String.format("Something went wrong while sending message via whatsapp for request %s", response.getFailedRequests()));
			}
		} catch (Exception e) {
			_LOGGER.info("Something went wrong while sending message via whatsapp.");
		}
	}

	public List<StoreSecPickingViewBean> getStoreSecPickingView() {
		List<StoreSecPickingViewBean> response = null;
		try {
			response = wmsClient.getStoreSecPickingView(getWmsHeaderMap());
		} catch (Exception e) {
			_LOGGER.error("Error while fetching Store Secondary Picking Items", e);
			throw new ValidationException(new ErrorBean(Errors.SERVER_EXCEPTION, "Something went wrong while fetching Store Secondary Picking Items"));
		}
		return response;
	}

	public List<WalletBean> getStoreWalletsInternal(List<String> storeIds) {
		List<WalletBean> response;
		try {
			BulkStoreWalletRequest bulkStoreWalletRequest = new BulkStoreWalletRequest();
			bulkStoreWalletRequest.setStoreIds(storeIds);
			response = paymentClient.getStoreWalletsFromIds(bulkStoreWalletRequest);
		} catch (Exception e) {
			_LOGGER.error("Error while fetching stores ", e);
			if (e instanceof ValidationException) {
				throw e;
			} else {
				throw new ServerException(new ErrorBean(Errors.SERVER_EXCEPTION, "Something went wrong while fetching wallets"));
			}
		}
		return response;
	}

	public SocietyListItemBean getSocietyById(Integer societyId) {
		SocietyListItemBean society = null;
		try {
			society = wmsClient.getSocietyInformationFromId(getWmsHeaderMap(), societyId);
		} catch (Exception e) {
			throw new ValidationException(new ErrorBean(Errors.SERVER_EXCEPTION, "Something went wrong while fetching Society Information."));
		}
		return society;
	}

	public AmStoreDetailsResponse getStoreDetailsFromStoreId(String storeId) {
		try {
			return authConsumerClient.getStoreDetailsFromStoreId(storeId);
		} catch (Exception e) {
			_LOGGER.error(String.format("Error while getting AM Details for storeId : %s ", storeId), e);
			return null;
		}
	}

	public List<ConsumerAddressResponse> getUserAddressesInternal(List<Long> addressIds) {
		List<ConsumerAddressResponse> response;
		try {
			BulkConsumerAddressRequest bulkConsumerAddressRequest = new BulkConsumerAddressRequest();
			bulkConsumerAddressRequest.setAddressIds(addressIds);
			response = authConsumerClient.getUserAddressesInternal(bulkConsumerAddressRequest);
		} catch (Exception e) {
			if (e instanceof ValidationException) {
				throw e;
			} else {
				throw new ServerException(new ErrorBean(Errors.SERVER_EXCEPTION, "Something went wrong while fetching addresses"));
			}
		}
		return response;
	}

	public void sendConsumerOrderToWms(WmsOrderPayload wmsOrderPayload) {
		try {
			wmsClient.pushWmsConsumerOrder(getWmsHeaderMap(), wmsOrderPayload);
		} catch (Exception e) {
			_LOGGER.error("Error while sending order to WMS.", e);
			throw e;
		}
	}

//	public void createFulfilment() {
//		try {
//			wmsClient.createFulfilment(getWmsHeaderMap());
//		} catch (Exception e) {
//			_LOGGER.error("Error while creating fulfilment.", e);
//		}
//	}

	public TicketBean fetchTicketByReferenceId(String id) {
		TicketBean response;
		try {
			response = ticketsClient.fetchTicketByReferenceId(id);
		} catch (Exception e) {
			_LOGGER.error(String.format("Error while fetching ticket data with message :: %s", e.getMessage()));
			response = null;
		}
		return response;
	}

	public void refreshStorePricing() {
		try {
			storeClient.refreshStorePricing();
		} catch (Exception e) {
			_LOGGER.error("Error while refreshing store pricing", e);
		}
	}

	public void updateUserOrderCount(UUID customerId, Long count) {
		try {
			authConsumerClient.updateUserOrderCount(customerId, count);
		} catch (Exception e) {
			_LOGGER.error(String.format("Error while updating the order count for user : %s", customerId), e);
		}
	}

	public List<WalletBean> findAllCustomersWallet(List<UUID> customerIds) {
		try {
			BulkUserWalletRequest request = new BulkUserWalletRequest();
			request.setUserIds(customerIds);
			return paymentClient.getUserWalletsFromIds(request);
		} catch (Exception e) {
			_LOGGER.error("Some error occurred while fetching user wallets", e);
			throw new ServerException(new ErrorBean(Errors.SERVER_EXCEPTION, "Something went wrong while fetching wallets"));
		}
	}

	public void sendRequisitionToWms(List<WmsDsPayload> wmsDsPayloads) {
		try {
			wmsClient.sendOrdersToWms(getWmsHeaderMap(), wmsDsPayloads);
		} catch (Exception e) {
			_LOGGER.error("Error while sending wmsDsPayloads to WMS.", e);
			throw e;
		}
	}

	public List<UserAudienceBean> getUserAudience(UUID customerId) {
		try {
			return authConsumerClient.getUserAudience(customerId);
		} catch (Exception e) {
			_LOGGER.error("Error while fetching UserAudience", e);
			throw new ServerException(new ErrorBean(Errors.SERVER_EXCEPTION, "Something went wrong while fetching UserAudience."));
		}
	}

	public void updatePerPcsWeight(List<SkuPerPcsWeightDto> dtos) {
		try {
			storeInventoryClient.updatePerPcsWeight(dtos);
		} catch (Exception e) {
			_LOGGER.error("Error while updating per pcs weight", e);
			throw new ServerException(new ErrorBean(Errors.SERVER_EXCEPTION, "Something went wrong while updating per pcs weight."));
		}
	}

	public void disableOnboardingOffer(List<String> customerIds) {
		try {
			authConsumerClient.disableOnboardingOffer(customerIds);
		} catch (Exception e) {
			_LOGGER.error("Error while disabling onboarding offer", e);
		}
	}

	public void deactivateFirstOrderFlow(List<String> userIds) {
		try {
			authConsumerClient.deactivateFirstOrderFlow(userIds);
		} catch (Exception e) {
			_LOGGER.error("Error while deactivating first order flow", e);
		}
	}

	public CashCollectionResponse getRequestedCashCollection(UUID userId) {
		try {
			return paymentClient.getCashCollectionByUserId(userId);
		} catch (Exception e) {
			_LOGGER.error("Error while getting requested cash collection", e);
		}
		return null;
	}

	public void sendUserEvent(String eventName, UserEventRequest userEventRequest) {
		try {
			authConsumerClient.sendUserEvent(eventName, userEventRequest);
		} catch (Exception e) {
			_LOGGER.error("Error while sending user event", e);
		}
	}

	public List<UserAddressResponse> getUserAddressByUserId(UUID userId) {
		try {
			return authConsumerClient.getUserAddressById(userId, true);
		} catch (Exception e) {
			_LOGGER.error("Error while fetching User Address", e);
		}
		return null;
	}

	public List<UserServiceResponse> getUserDetailsByIds(List<String> userIds){
		if (CollectionUtils.isEmpty(userIds))
			throw new ValidationException(ErrorBean.withError(Errors.INVALID_REQUEST, "User details not found", ""));
		try {
			UserIdsRequest userIdsRequest = new UserIdsRequest(userIds);
			return authConsumerClient.getUserDetailsByIds(userIdsRequest);
		} catch (Exception e) {
			_LOGGER.error(String.format("Error while getting userDetails for id ", userIds, e));
			return null;
		}
	}

}
