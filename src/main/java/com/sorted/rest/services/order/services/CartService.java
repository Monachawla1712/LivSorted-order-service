package com.sorted.rest.services.order.services;

import com.sorted.rest.common.beans.ErrorBean;
import com.sorted.rest.common.dbsupport.crud.BaseCrudRepository;
import com.sorted.rest.common.exceptions.ValidationException;
import com.sorted.rest.common.logging.AppLogger;
import com.sorted.rest.common.logging.LoggingManager;
import com.sorted.rest.common.properties.Errors;
import com.sorted.rest.common.utils.CollectionUtils;
import com.sorted.rest.common.utils.DateUtils;
import com.sorted.rest.common.utils.ParamsUtils;
import com.sorted.rest.common.websupport.base.BaseService;
import com.sorted.rest.services.order.beans.*;
import com.sorted.rest.services.order.beans.EligibleOffersResponse.CashbackOfferSku;
import com.sorted.rest.services.order.beans.StoreInventoryResponse.StoreProductInventory;
import com.sorted.rest.services.order.clients.ClientService;
import com.sorted.rest.services.order.constants.OrderConstants;
import com.sorted.rest.services.order.constants.OrderConstants.DiscountType;
import com.sorted.rest.services.order.constants.OrderConstants.PaymentMethod;
import com.sorted.rest.services.order.constants.OrderConstants.ShippingMethod;
import com.sorted.rest.services.order.entity.*;
import com.sorted.rest.services.order.repository.OrderOfferRepository;
import com.sorted.rest.services.order.utils.ConditionFulfillment;
import com.sorted.rest.services.order.utils.DeliveryDateUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.sorted.rest.services.order.constants.OrderConstants.BACKOFFICE_APP_ID;
import static com.sorted.rest.services.order.constants.OrderConstants.ITEM_OUT_OF_STOCK;

/**
 * Created by mohit on 20.8.22.
 */
@Service
public class CartService implements BaseService<OrderOfferEntity> {

	private final AppLogger _LOGGER = LoggingManager.getLogger(CartService.class);

	@Autowired
	private OrderService orderService;

	@Autowired
	private OrderItemService orderItemService;

	@Autowired
	private PricingService pricingService;

	@Autowired
	private ClientService clientService;

	@Autowired
	private OrderOfferRepository orderOfferRepository;

	@Autowired
	private DisplayOrderIdService displayOrderIdService;

	@Autowired
	private PreBookOrderService preBookOrderService;

	private StoreInventoryAddOrDeductRequest createStoreInventoryAddOrDeductRequest(List<StoreInventoryAddOrDeductRequest.StoreInventoryUpdateData> data) {
		return new StoreInventoryAddOrDeductRequest(data);
	}

	private StoreInventoryUpdateResponse addOrDeductInventory(String storeId, String skuCode, Double diff) {
		List<StoreInventoryAddOrDeductRequest.StoreInventoryUpdateData> storeInventoryUpdateDataList = new ArrayList<>();
		StoreInventoryAddOrDeductRequest.StoreInventoryUpdateData storeInventoryUpdateData = new StoreInventoryAddOrDeductRequest.StoreInventoryUpdateData(diff,
				skuCode);
		storeInventoryUpdateDataList.add(storeInventoryUpdateData);
		StoreInventoryAddOrDeductRequest storeInvRequest = createStoreInventoryAddOrDeductRequest(storeInventoryUpdateDataList);
		return clientService.addOrDeductStoreInventory(storeId, storeInvRequest);
	}

	private void removeItemFromCart(OrderEntity cart, OrderItemEntity cartItem) {
		cartItem.setFinalQuantity(0d);
		cartItem.getMetadata().setPieces(null);
		if (cartItem.getMetadata().getGrades() != null) {
			updateGradeQty(cartItem, null);
		}
		cartItem.setOrderedQty(0d);
		cartItem.setActive(0);
		orderItemService.save(cartItem);
		cart.getOrderItems().remove(cartItem);
		if (cart.getOrderItems().size() == 0) {
			cart.setActive(0);
		}
		pricingService.setAmountAndTaxesInOrderAndItems(cart, CoinsParamsObject.newInstance());
	}

	private void removeItemsFromCart(OrderEntity cart, List<OrderItemEntity> cartItems) {
		for (OrderItemEntity cartItem : cartItems) {
			cartItem.setFinalQuantity(0d);
			cartItem.getMetadata().setPieces(null);
			if (cartItem.getMetadata().getGrades() != null) {
				updateGradeQty(cartItem, null);
			}
			cartItem.setOrderedQty(0d);
			cartItem.setActive(0);
			orderItemService.save(cartItem);
			cart.getOrderItems().remove(cartItem);
		}
		if (cart.getOrderItems().size() == 0) {
			cart.setActive(0);
		}
		pricingService.setAmountAndTaxesInOrderAndItems(cart, CoinsParamsObject.newInstance());
	}

	@Transactional(propagation = Propagation.REQUIRED)
	public void changeOrderState(OrderEntity order, ShippingMethod shippingMethod, PaymentMethod paymentMethod, OrderConstants.OrderStatus orderStatus,
			Double amountReceived) {
		order.setShippingMethod(shippingMethod);
		order.setPaymentMethod(paymentMethod);
		order.setStatus(orderStatus);
		order.setSubmittedAt(new Date());
		if (amountReceived != null) {
			order.setAmountReceived(amountReceived);
		}
		orderService.save(order);
	}

	public void doStoreChangeAction(OrderEntity cart, List<StoreProductInventory> storeItems) {
		Map<String, StoreProductInventory> storeItemMap = storeItems.stream().collect(Collectors.toMap(i -> i.getInventorySkuCode(), i -> i));
		boolean isCartEmpty = true;
		List<OrderItemEntity> removeItems = new ArrayList<>();
		for (OrderItemEntity i : cart.getOrderItems()) {
			if (storeItemMap.containsKey(i.getSkuCode())) {
				StoreProductInventory storeItem = storeItemMap.get(i.getSkuCode());
				if (Double.compare(storeItem.getInventoryQuantity(), 0.0) <= 0) {
					removeItem(i, removeItems);
					_LOGGER.info(String.format("doStoreChangeAction :: storeItem: %s removed from cart", i.getSkuCode()));
					ErrorBean error = new ErrorBean("ITEMS_OOS", "Few Items in your cart have been updated. Kindly Recheck.");
					cart.setError(error);
				} else if (Double.compare(storeItem.getInventoryQuantity(), i.getFinalQuantity()) < 0) {
					_LOGGER.info(String.format("doStoreChangeAction :: storeItem: %s quantity reduced", i.getSkuCode()));
					i.setFinalQuantity(storeItem.getInventoryQuantity());
					if (i.getMetadata().getPieces() != null) {
						Integer totalPieces = calculatePieces(i.getFinalQuantity(), i.getMetadata().getPerPiecesWeight());
						i.getMetadata().setPieces(totalPieces);
					}
					if (i.getMetadata().getGrades() != null) {
						updateGradeQty(i, null);
					}
					i.setError(new ErrorBean("INSUFFICIENT_QUANTITY",
							String.format("%s's quantity has been reduced due to less availability in the market.", i.getProductName())));
					ErrorBean error = new ErrorBean("ITEMS_OOS", "Few Items in your cart have been updated. Kindly Recheck.");
					cart.setError(error);
					isCartEmpty = false;
				} else {
					isCartEmpty = false;
				}
			} else {
				removeItem(i, removeItems);
				_LOGGER.info(String.format("doStoreChangeAction :: storeItem: %s removed from cart", i.getSkuCode()));
				ErrorBean error = new ErrorBean("ITEMS_OOS", "Few Items in your cart have been updated. Kindly Recheck.");
				cart.setError(error);
			}
		}
		cart.getOrderItems().removeAll(removeItems);
		if (isCartEmpty) {
			_LOGGER.info("doStoreChangeAction :: all items removed from cart");
			cart.setActive(0);
		}
	}

	private void removeItem(OrderItemEntity i, List<OrderItemEntity> removeItems) {
		i.setActive(0);
		i.setFinalQuantity(0d);
		i.getMetadata().setPieces(null);
		if (i.getMetadata().getGrades() != null) {
			updateGradeQty(i, null);
		}
		removeItems.add(i);
	}

	public Boolean validateAndDoChangeAction(OrderEntity cart, List<StoreProductInventory> storeItems, List<StoreProductInventory> oldStoreItems) {
		Boolean isChanged = false;
		if (cart.getOrderItems() != null) {
			Map<String, StoreProductInventory> storeItemMap = storeItems.stream().collect(Collectors.toMap(i -> i.getInventorySkuCode(), i -> i));
			Map<String, StoreProductInventory> oldStoreItemMap = new HashMap<>();
			if (oldStoreItems != null) {
				oldStoreItemMap = oldStoreItems.stream().collect(Collectors.toMap(i -> i.getInventorySkuCode(), i -> i));
			}
			List<OrderItemEntity> removeItems = new ArrayList<>();
			for (OrderItemEntity i : cart.getOrderItems()) {
				StoreProductInventory oldStoreItem = oldStoreItemMap.get(i.getSkuCode());
				Double oldCartQty = i.getFinalQuantity();
				if (oldStoreItem != null) {
					oldCartQty = 0d;
					OrderEntity oldCart = new OrderEntity();
					oldCart.setStoreId(oldStoreItem.getInventoryStoreId());
					addOrDeductInventory(oldCart.getStoreId(), i.getSkuCode(), i.getFinalQuantity());
					oldStoreItem.setInventoryQuantity(oldStoreItem.getInventoryQuantity() + i.getFinalQuantity());
				}
				isChanged = validateStockCartItem(i, storeItemMap, removeItems, oldCartQty, null) || isChanged;
				if (!isChanged) {
					StoreProductInventory storeItem = storeItemMap.get(i.getSkuCode());
					addOrDeductInventory(cart.getStoreId(), storeItem.getInventorySkuCode(), -1 * i.getFinalQuantity());
					storeItem.setInventoryQuantity(storeItem.getInventoryQuantity() + -1 * i.getFinalQuantity());
				}
			}
			cart.getOrderItems().removeAll(removeItems);
			cart.setItemCount(cart.getOrderItems() != null ? cart.getOrderItems().size() : 0);
			if (cart.getItemCount() < 1) {
				_LOGGER.info("doChangeAction :: all items removed from cart");
				cart.setActive(0);
			}
		}
		return isChanged;
	}

	private boolean validateStockCartItem(OrderItemEntity cartItem, Map<String, StoreProductInventory> storeItemMap, List<OrderItemEntity> removeItems,
			Double oldCartQty, List<CartRequest.OrderItemGradeBean> oldGrades) {
		Boolean isChanged = false;
		if (storeItemMap.containsKey(cartItem.getSkuCode())) {
			StoreProductInventory storeItem = storeItemMap.get(cartItem.getSkuCode());
			if (Double.compare(storeItem.getInventoryQuantity(), 0.0) <= 0) {
				removeItem(cartItem, removeItems);
				_LOGGER.info(String.format("validateCartItem :: storeItem: %s removed from cart", cartItem.getSkuCode()));
				String outOfStockMessage = ParamsUtils.getParam("SKU_OUT_OF_STOCK_MSG", "Oops! We are out!! %s removed from cart");
				ErrorBean error = new ErrorBean("ITEMS_OOS", String.format(outOfStockMessage, cartItem.getProductName()));
				cartItem.setFinalQuantity(0d);
				cartItem.setError(error);
				isChanged = true;
			} else if (Double.compare(storeItem.getInventoryQuantity(), cartItem.getFinalQuantity()) < 0) {
				_LOGGER.info(String.format("validateCartItem :: storeItem: %s quantity reduced", cartItem.getSkuCode()));
				cartItem.setFinalQuantity(storeItem.getInventoryQuantity());
				if (cartItem.getMetadata().getPieces() != null) {
					Integer totalPieces = calculatePieces(cartItem.getFinalQuantity(), cartItem.getMetadata().getPerPiecesWeight());
					cartItem.getMetadata().setPieces(totalPieces);
				}
				if (cartItem.getMetadata().getGrades() != null) {
					updateGradeQty(cartItem, oldGrades);
				}
				Double diff = oldCartQty - storeItem.getInventoryQuantity();
				addOrDeductInventory(cartItem.getOrder().getStoreId(), storeItem.getInventorySkuCode(), diff);
				storeItem.setInventoryQuantity(0d);
				cartItem.setError(new ErrorBean("INSUFFICIENT_QUANTITY",
						String.format("%s's quantity has been reduced due to less availability in the market.", cartItem.getProductName())));
				isChanged = true;
			}
		} else {
			removeItem(cartItem, removeItems);
			_LOGGER.info(String.format("validateCartItem :: storeItem: %s removed from cart", cartItem.getSkuCode()));
			ErrorBean error = new ErrorBean("ITEMS_OOS", "Few Items in your cart have been updated. Kindly Recheck.");
			cartItem.setFinalQuantity(0d);
			cartItem.setError(error);
			isChanged = true;
		}
		return isChanged;
	}

	private Integer calculatePieces(Double finalQuantity, Double perPieceWeight) {
		return (int) (Math.round(finalQuantity / perPieceWeight));
	}

	private void updateGradeQty(OrderItemEntity cartItem, List<CartRequest.OrderItemGradeBean> oldGrades) {
		CartRequest.OrderItemGradeBean resetGrade;
		Double totalExistingGradesQuantity = 0d;
		if (oldGrades != null) {
			resetGrade = cartItem.getMetadata().getGrades().stream().filter(item -> !oldGrades.contains(item)).findFirst().orElse(null);
			totalExistingGradesQuantity = cartItem.getMetadata().getGrades().stream().filter(oldGrades::contains)
					.mapToDouble(CartRequest.OrderItemGradeBean::getQuantity).sum();
		} else {
			resetGrade = cartItem.getMetadata().getGrades().stream().findFirst().orElse(null);
		}
		if (resetGrade != null) {
			Double gradeQty = cartItem.getFinalQuantity() - totalExistingGradesQuantity;
			resetGrade.setQuantity(gradeQty);
			if (resetGrade.getPieces() != null) {
				Integer totalPieces = calculatePieces(gradeQty, cartItem.getMetadata().getPerPiecesWeight());
				resetGrade.setPieces(totalPieces != 0 ? totalPieces : null);
			}
		}
	}

	@Transactional(propagation = Propagation.REQUIRED)
	public void addItemToCartV2(OrderEntity cart, StoreProductInventory storeItem, Double quantity, Integer pieceQty, String productName, Double discountAmount,
			String appId, CartResponse response, List<CartRequest.OrderItemGradeBean> grades, String notes, Boolean isRepeatItem, Boolean isOzoneWashedItem,
			Boolean skipWalletCheck) {
		OrderItemEntity cartItem = null;
		if (storeItem.getInventoryQuantity() < 0) {
			storeItem.setInventoryQuantity(0.0d);
		}
		if (!Objects.equals(storeItem.getProductUnitOfMeasurement(), "KILOGRAM")) {
			storeItem.setInventoryQuantity(Math.floor(storeItem.getInventoryQuantity()));
		}
		_LOGGER.info(String.format("CartService:addItemToCartV2 :: storeItem Sku: %s", storeItem.getInventorySkuCode()));
		if (CollectionUtils.isNotEmpty(cart.getOrderItems())) {
			Optional<OrderItemEntity> opCartItem = cart.getOrderItems().stream().filter(i -> i.getSkuCode().equals(storeItem.getInventorySkuCode()))
					.findFirst();
			if (opCartItem.isPresent()) {
				cartItem = opCartItem.get();
				_LOGGER.info(String.format("CartService:addItemToCartV2 :: cartItem found: %s", cartItem));
			}
		}
		BigDecimal itemSalePrice = OrderItemEntity.calcSalePriceBracket(storeItem.getInventorySalePrice(), storeItem.getInventoryPriceBrackets(),
				pieceQty != null ? pieceQty.doubleValue() : quantity, cartItem);
		if (isRepeatItem != null && isRepeatItem) {
			skipWalletCheck = true;
		}
		if (!skipWalletCheck) {
			validateOrderCountAndWallet(cart, cartItem, response, itemSalePrice, quantity, cart.getMetadata().getAppVersion());
		}
		if (response.getError() != null) {
			return;
		}
		if (cartItem == null && Double.compare(quantity, 0.0d) == 0) {
			_LOGGER.info("CartService:addItemToCartV2 :: cartItem null and quantity zero");
			return;
		}
		Map<String, StoreProductInventory> storeItemMap = new HashMap<>();
		storeItemMap.put(storeItem.getProductSkuCode(), storeItem);
		double ozoneWashingCharges = 0d;
		if (isOzoneWashedItem != null && isOzoneWashedItem) {
			ozoneWashingCharges = storeItem.getOzoneWashingCharges() != null
					? storeItem.getOzoneWashingCharges()
					: Double.parseDouble(ParamsUtils.getParam("OZONE_WASHING_CHARGES", "0"));
		}
		if (cartItem == null && Double.compare(quantity, 0.0d) > 0) {
			_LOGGER.info("CartService:addItemToCartV2 :: create new item in cart");
			cartItem = OrderItemEntity.newCartItem(cart, storeItem, quantity, isRepeatItem);
			if (discountAmount != null && !cartItem.getDiscountAmount().equals(discountAmount)) {
				cartItem.setDiscountAmount(discountAmount);
			}
			if (pieceQty != null) {
				cartItem.getMetadata().setProductName(productName);
				cartItem.getMetadata().setPieces(pieceQty);
				cartItem.getMetadata().setSuffix(storeItem.getProductPerPcsSuffix());
			}
			if (grades != null) {
				cartItem.getMetadata().setGrades(grades);
			}
//			if (!isPosRequest(appId) && validateCartItem(cartItem, storeItemMap, new ArrayList<>()) && cartItem.getError() != null) {
//				response.error("requested_qty_not_available",
//						String.format("Requested Quantity not available. Only %s %s available.", cartItem.getFinalQuantity(),
//								storeItem.getProductUnitOfMeasurement()));
//			}
			boolean isInvalidStockInCart = validateStockCartItem(cartItem, storeItemMap, new ArrayList<>(), 0d, null);
			if (!isPosRequest(appId) && isInvalidStockInCart && cartItem.getError() != null) {
				response.error(cartItem.getError().getCode(), cartItem.getError().getMessage());
			} else if (!isInvalidStockInCart && cartItem.getError() == null) {
				addOrDeductInventory(cart.getStoreId(), storeItem.getInventorySkuCode(), -1 * quantity);
			}
			if (cartItem.getFinalQuantity().equals(0d) && (cart.getOrderItems() == null || cart.getOrderItems().size() == 0)) {
				cart.setActive(0);
				return;
			}
			if (!cartItem.getFinalQuantity().equals(0d)) {
				cart.addOrderItem(cartItem);
			}
			cartItem.setSalePrice(OrderItemEntity.calcSalePriceBracket(storeItem.getInventorySalePrice(), storeItem.getInventoryPriceBrackets(),
					pieceQty != null ? pieceQty.doubleValue() : quantity, cartItem));
			cartItem.getMetadata().setNotes(notes);
			pricingService.setAmountAndTaxesInOrderAndItem(cart, cartItem, null);
		} else if (!cartItem.getFinalQuantity().equals(quantity) && Double.compare(quantity, 0.0d) > 0) {
			_LOGGER.info("CartService:addItemToCartV2 :: updating item quantity");
			if (discountAmount != null && !cartItem.getDiscountAmount().equals(discountAmount)) {
				cartItem.setDiscountAmount(discountAmount);
			}
			List<CartRequest.OrderItemGradeBean> oldGrades = new ArrayList<>();
			if (grades != null) {
				oldGrades = cartItem.getMetadata().getGrades();
				cartItem.getMetadata().setGrades(grades);
			}
			Double oldCartQty = cartItem.getFinalQuantity();
			storeItem.setInventoryQuantity(storeItem.getInventoryQuantity() + cartItem.getFinalQuantity());
			cartItem.setFinalQuantity(quantity);
			cartItem.setOrderedQty(quantity);
			if (cartItem.getMetadata() == null) {
				cartItem.setMetadata(new OrderItemMetadata());
			}
			if (pieceQty != null) {
				cartItem.getMetadata().setProductName(productName);
				cartItem.getMetadata().setPieces(pieceQty);
				cartItem.getMetadata().setSuffix(storeItem.getProductPerPcsSuffix());
			}
			boolean isInvalidStockInCart = validateStockCartItem(cartItem, storeItemMap, new ArrayList<>(), oldCartQty,
					!Objects.isNull(oldGrades) ? oldGrades : null);
			if (isInvalidStockInCart && cartItem.getError() != null) {
				response.error(cartItem.getError().getCode(), cartItem.getError().getMessage());
			} else if (!isInvalidStockInCart && cartItem.getError() == null) {
				Double diff = oldCartQty - quantity;
				addOrDeductInventory(cart.getStoreId(), storeItem.getInventorySkuCode(), diff);
			}
			if (cart.getOrderItems().size() == 1 && cartItem.getFinalQuantity().equals(0d)) {
				cart.setActive(0);
				return;
			}
			cartItem.setSalePrice(OrderItemEntity.calcSalePriceBracket(storeItem.getInventorySalePrice(), storeItem.getInventoryPriceBrackets(),
					pieceQty != null ? pieceQty.doubleValue() : quantity, cartItem));
			cartItem.getMetadata().setNotes(notes);
			pricingService.setAmountAndTaxesInOrderAndItem(cart, cartItem, null);
		} else if (Double.compare(quantity, 0.0d) > 0 && discountAmount != null && !cartItem.getDiscountAmount().equals(discountAmount)) {
			if (grades != null) {
				cartItem.getMetadata().setGrades(grades);
			}
			cartItem.setDiscountAmount(discountAmount);
			cartItem.setSalePrice(OrderItemEntity.calcSalePriceBracket(storeItem.getInventorySalePrice(), storeItem.getInventoryPriceBrackets(),
					pieceQty != null ? pieceQty.doubleValue() : quantity, cartItem));
			cartItem.getMetadata().setNotes(notes);
			pricingService.setAmountAndTaxesInOrderAndItem(cart, cartItem, null);
		} else if (Double.compare(quantity, 0.0d) == 0) {
			_LOGGER.info("CartService:addItemToCartV2 :: removing item from cart");
			cartItem.getMetadata().setNotes(notes);
			addOrDeductInventory(cart.getStoreId(), storeItem.getInventorySkuCode(), cartItem.getFinalQuantity());
			removeItemFromCart(cart, cartItem);
		}
		if (cartItem.getMetadata() != null && !Objects.equals(cartItem.getMetadata().getNotes(), notes)) {
			cartItem.getMetadata().setNotes(notes); // case when only notes is updated, for ex: when qty is not changed
			orderItemService.save(cartItem);
		}
		if (storeItem.getProductPacketDescription() != null && cartItem.getMetadata() != null) {
			cartItem.getMetadata().setPacketDescription(storeItem.getProductPacketDescription());
		}
		updateOzoneWashedItem(cartItem, storeItem, ozoneWashingCharges, isOzoneWashedItem);
		pricingService.setAmountAndTaxesInOrderAndItem(cart, cartItem, null);
	}

	private void updateOzoneWashedItem(OrderItemEntity cartItem, StoreProductInventory storeItem, double ozoneWashingCharges, Boolean isOzoneWashedItem) {
		if (cartItem.getFinalQuantity() > 0d) {
			boolean isOzoneWashed = Boolean.TRUE.equals(isOzoneWashedItem) && Boolean.TRUE.equals(storeItem.getIsOzoneWashedItem());
			cartItem.getMetadata().setIsOzoneWashedItem(isOzoneWashed);
			cartItem.getMetadata().setOzoneWashingCharge(isOzoneWashed ? ozoneWashingCharges : 0d);
		}
	}

	public void computeCashback(OrderEntity cart, EligibleOffersResponse eligibleOffers, boolean isOfferItemUpdated) {
		if (!isOfferItemUpdated) {
			removeCashbackItem(cart);
		}
		if (eligibleOffers != null && eligibleOffers.getFreeTomatoes() != null && eligibleOffers.getFreeTomatoes()
				&& eligibleOffers.getExpiry() != null && eligibleOffers.getExpiry().after(new Date())) {
			String eligibleOfferMsg = String.format("Don’t forget to order your %s", eligibleOffers.getOfferName().toLowerCase());
			Double maxCb = 0d;
			OrderItemEntity cbItem = null;
			CashbackOfferSku cbOfferSku = null;
			for (OrderItemEntity item : cart.getOrderItems()) {
				for (CashbackOfferSku offerSku : eligibleOffers.getSkus()) {
					if (offerSku.getSkuCode().equals(item.getSkuCode())) {
						Double cashbackQty = Math.min(offerSku.getQuantity(), item.getFinalQuantity());
						Double cashbackAmount = getCashbackAmount(item, cashbackQty);
						if (maxCb.compareTo(cashbackAmount) < 0
								&& (eligibleOffers.getMinCartValue() == null || cart.getFinalBillAmount() >= eligibleOffers.getMinCartValue())) {
							maxCb = cashbackAmount;
							cbItem = item;
							cbOfferSku = offerSku;
						}
					}
				}
			}
			if (cbItem != null) {
				eligibleOfferMsg = String.format("Congrats! You got %s!", eligibleOffers.getOfferName().toLowerCase());
				setCashbackDetails(cbItem, cbOfferSku.getQuantity());
			}
			cart.getMetadata().setEligibleOfferMsg(eligibleOfferMsg);
		}
	}

	public Double getCashbackAmount(OrderItemEntity item, Double cashbackQty) {
		return BigDecimal.valueOf(item.getProrataAmount() / item.getFinalQuantity()).multiply(BigDecimal.valueOf(cashbackQty)).setScale(0, RoundingMode.HALF_UP)
				.doubleValue();
	}

	private void setCashbackDetails(OrderItemEntity item, Double offerSkuQty) {
		Double cashbackQty = Math.min(offerSkuQty, item.getFinalQuantity());
		Double cashbackAmount = getCashbackAmount(item, cashbackQty);
		item.getMetadata().setItemCashbackQty(cashbackQty);
		item.getMetadata().setItemCashbackAmount(cashbackAmount);
		item.getMetadata().setIsCashbackItem(true);
		item.getMetadata().setItemCashbackMaxQty(offerSkuQty);
		item.getMetadata().setCashbackType(OrderConstants.CashbackType.FREE_ITEM);
	}

	public void removeCashbackItem(OrderEntity cart) {
		_LOGGER.info(String.format("removeCashbackItem request for order : %s", cart.getDisplayOrderId()));
		if (CollectionUtils.isNotEmpty(cart.getOrderItems())) {
			cart.getOrderItems().stream().forEach(i -> {
				i.getMetadata().setIsCashbackItem(false);
				i.getMetadata().setItemCashbackMaxQty(null);
				i.getMetadata().setItemCashbackQty(null);
				i.getMetadata().setItemCashbackAmount(null);
			});
		}
	}

	private void validateOrderCountAndWallet(OrderEntity cart, OrderItemEntity cartItem, CartResponse response, BigDecimal itemSalePrice, Double quantity,
			String appVersion) {
		Integer codCountLimit = ParamsUtils.getIntegerParam("COD_ORDER_COUNT_LIMIT", 7);
		if ((cart.getMetadata().getOrderCount() != null && cart.getMetadata().getOrderCount() > codCountLimit
				&& !hasSufficientPrepaidWalletMoney(cart, cartItem, itemSalePrice, quantity))) {
			addedWalletErrorResponse(response, appVersion, cart.getCustomerId());
		}
	}

	public void hasSufficientCodWalletAmount(CartResponse response, UUID customerId, String appVersion) {
		// Double thresholdAmount =
		// Double.valueOf(ParamsUtils.getParam("B2C_COD_WALLET_THRESHOLD", "-100"));
		UserServiceResponse userDetails = clientService.getUserDetailsFromCustomerId(customerId);
		if (isFirstOrder(userDetails)) {
			return;
		}
		WalletBean wallet = clientService.getUserWallet(customerId.toString());
		BigDecimal walletAmount = BigDecimal.valueOf(wallet.getAmount());
		BigDecimal creditLimit = BigDecimal.valueOf(wallet.getCreditLimit());
		if (walletAmount.add(creditLimit).compareTo(BigDecimal.ZERO) < 0) {
			showLowWalletBalanceError(response, appVersion, customerId);
		}
	}

	public boolean isFirstOrderFlowVersion(String appVersion) {
		String newOrderFlowVersion = ParamsUtils.getParam("FIRST_ORDER_FLOW_VERSION");
		return this.isVersionGreaterOrEqual(appVersion, newOrderFlowVersion);
	}

	void showLowWalletBalanceError(CartResponse response, String appVersion, UUID customerId) {
		boolean isFirstOrderFlowVersion = isFirstOrderFlowVersion(appVersion);
		if (isFirstOrderFlowVersion) {
			CashCollectionResponse cashCollectionResponse = clientService.getRequestedCashCollection(customerId);
			if (cashCollectionResponse != null) {
				response.setError(new ErrorBean("CC_SCHEDULED", "CashCollection request is raised"));
			} else {
				response.setError(getWalletError(true));
			}
		} else {
			throw new ValidationException(getWalletError(false));
		}
	}

	public void setOrderSlotDetails(OrderEntity cart, OrderSlotEntity orderSlot) {
		if (orderSlot != null) {
			cart.setSlotId(orderSlot.getId());
			cart.getMetadata().setOrderSlot(orderSlot.getSlot());
			cart.getMetadata().getEta().setEta(orderSlot.getEta());
			cart.getExtraFeeDetails().setSlotCharges(orderSlot.getFees());
		}
	}

	private boolean hasSufficientPrepaidWalletMoney(OrderEntity cart, OrderItemEntity cartItem, BigDecimal salePrice, Double requestedQuantity) {
		WalletBean walletBean = clientService.getUserWallet(cart.getCustomerId().toString());
		BigDecimal walletAmount = BigDecimal.valueOf(0);
		if (walletBean == null) {
			return false;
		} else {
			walletAmount = BigDecimal.valueOf(walletBean.getAmount());
		}
		cart.setWalletAmount(walletBean.getAmount());
		BigDecimal additionalAmount;
		if (cartItem == null) {
			additionalAmount = BigDecimal.valueOf(requestedQuantity).multiply(salePrice);
		} else if (cartItem.getFinalQuantity() < requestedQuantity) {
			additionalAmount = BigDecimal.valueOf(requestedQuantity).multiply(salePrice).subtract(BigDecimal.valueOf(cartItem.getSpGrossAmount()));
		} else {
			return true;
		}
		BigDecimal expectedAmount = BigDecimal.valueOf(cart.getFinalBillAmount()).add(additionalAmount);
		if (expectedAmount.subtract(walletAmount).subtract(BigDecimal.valueOf(walletBean.getCreditLimit())).compareTo(BigDecimal.ZERO) > 0) {
			return false;
		}
		return true;
	}

	private void addedWalletErrorResponse(CartResponse response, String appVersion, UUID customerId) {
		_LOGGER.debug("ADD money to your wallet");
		if (response.getError() == null) {
			showLowWalletBalanceError(response, appVersion, customerId);
		}
	}

	private AlertErrorBean getWalletError(boolean isNewVersion) {
		String topUpWalletError = ParamsUtils.getParam("TOP_UP_WALLET_ERROR_MESSAGE");
		if (isNewVersion) {
			return new AlertErrorBean(Errors.INVALID_REQUEST, "Please top-up your wallet to\ncontinue shopping", "wallet", "Please fill me up!",
					"https://d69ugcdrlg41w.cloudfront.net/public/a928fab0-7b01-48ad-ab06-46b8306472ce.png", "Continue", "https://livesorted.com?target=pay_now",
					"<b>Low wallet balance</b>");
		} else {
			return new AlertErrorBean(Errors.INVALID_REQUEST, topUpWalletError, "wallet", "Please top up your wallet to continue shopping",
					"https://d69ugcdrlg41w.cloudfront.net/public/da108958-5409-4e1a-81f9-ac3bc5320c07.png", "Continue",
					"https://livesorted.com?target=payment_screen", "");
		}
	}

	private boolean isPosRequest(String appId) {
		return Objects.equals(appId, "com.example.pos_flutter_app");
	}

	public List<OrderOfferEntity> getAllOrderOffers() {
		Date currentDate = DateUtils.convertDateUtcToIst(new Date());
		return orderOfferRepository.getValidOrderOffers(new java.sql.Date(currentDate.getTime()));
	}

	public void buildOrderOfferResponse(String storeId, OrderEntity cart, CartResponse response) {
		List<OrderOfferEntity> orderOffers = getAllOrderOffers();
		if (CollectionUtils.isEmpty(orderOffers)) {
			return;
		}
		List<OrderOfferEntity> validOrderOffers = getValidOrderOffers(orderOffers, cart == null ? response.getData().getCustomerId() : cart.getCustomerId());
		if (CollectionUtils.isEmpty(validOrderOffers)) {
			return;
		}
		sortOrderOfferByPriority(validOrderOffers);
		validOrderOffers = List.of(validOrderOffers.get(0));
		Set<String> skuCodes = validOrderOffers.stream().map(OrderOfferEntity::getSkuCode).collect(Collectors.toSet());
		StoreInventoryResponse storeInventory = clientService.getStoreInventory(storeId, String.join(",", skuCodes), null);
		if (storeInventory == null || CollectionUtils.isEmpty(storeInventory.getInventory())) {
			return;
		}
		List<StoreProductInventory> complimentaryInventory = storeInventory.getInventory().stream()
				.filter(storeProductInventory -> storeProductInventory.getIsComplimentary() != null && storeProductInventory.getIsComplimentary())
				.collect(Collectors.toList());
		if (CollectionUtils.isEmpty(complimentaryInventory)) {
			return;
		}
		Map<String, StoreProductInventory> inventoryMap = complimentaryInventory.stream()
				.collect(Collectors.toMap(StoreProductInventory::getInventorySkuCode, Function.identity()));
		Double offerItemTotalsSpAmount = 0d;
		Map<String, OrderItemEntity> orderOfferItemsInCart = new HashMap<>();
		if (cart != null && CollectionUtils.isNotEmpty(cart.getOrderItems())) {
			orderOfferItemsInCart = cart.getOrderItems().stream().filter(orderItem -> inventoryMap.containsKey(orderItem.getSkuCode()))
					.collect(Collectors.toMap(OrderItemEntity::getSkuCode, Function.identity()));
			offerItemTotalsSpAmount = getOrderOfferItemsTotalSpAmount(orderOfferItemsInCart);
			cart.setTotalSpGrossAmount(cart.getTotalSpGrossAmount() - offerItemTotalsSpAmount - cart.getTotalAdditionalDiscount().getOfferDiscount());
		}
		List<OrderOfferResponseBean> orderOfferResponseBeans = validOrderOffers.stream().filter(orderOffer -> inventoryMap.containsKey(orderOffer.getSkuCode()))
				.map(orderOfferEntity -> buildOrderOfferResponseBean(orderOfferEntity, inventoryMap.get(orderOfferEntity.getSkuCode()), cart, response))
				.collect(Collectors.toList());
		if (cart != null && CollectionUtils.isNotEmpty(cart.getOrderItems())) {
			cart.setTotalSpGrossAmount(cart.getTotalSpGrossAmount() + offerItemTotalsSpAmount + cart.getTotalAdditionalDiscount().getOfferDiscount());
		}
		for (OrderOfferResponseBean offerBean : orderOfferResponseBeans) {
			if (orderOfferItemsInCart.containsKey(offerBean.getSkuCode())) {
				offerBean.setIsItemInCart(Boolean.TRUE);
			}
		}
		response.setOrderOffers(orderOfferResponseBeans);
		if (cart != null) {
			generateBottomSheetResponse(cart, response);
		}
	}

	private List<OrderOfferEntity> getValidOrderOffers(List<OrderOfferEntity> orderOffers, UUID customerId) {
		List<OrderOfferEntity> validOrderOffers = new ArrayList<>();
		List<UserAudienceBean> userAudienceDtos = clientService.getUserAudience(customerId);
		Set<Integer> userAudienceIdsSet = userAudienceDtos.stream().map(UserAudienceBean::getAudienceId).collect(Collectors.toSet());
		if (CollectionUtils.isNotEmpty(orderOffers)) {
			orderOffers.forEach(orderOffer -> {
				OrderOfferMetadata orderOfferMetadata = orderOffer.getMetadata();
				boolean shouldAddSku = false;
				if (CollectionUtils.isNotEmpty(orderOfferMetadata.getAudienceIds())) {
					for (Integer id : orderOfferMetadata.getAudienceIds()) {
						if (userAudienceIdsSet.contains(id)) {
							shouldAddSku = true;
							break;
						}
					}
				} else {
					shouldAddSku = true;
				}
				if (CollectionUtils.isNotEmpty(orderOfferMetadata.getHideForAudienceIds())) {
					for (Integer id : orderOfferMetadata.getHideForAudienceIds()) {
						if (userAudienceIdsSet.contains(id)) {
							shouldAddSku = false;
							break;
						}
					}
				}
				if (shouldAddSku) {
					validOrderOffers.add(orderOffer);
				}
			});
		}
		return validOrderOffers;
	}

	public void sortOrderOfferByPriority(List<OrderOfferEntity> orderOffers) {
		orderOffers.sort(Comparator.comparingInt(
				orderOffer -> orderOffer.getMetadata().getPriority() != null ? orderOffer.getMetadata().getPriority() : Integer.MAX_VALUE));
	}

	private OrderOfferResponseBean buildOrderOfferResponseBean(OrderOfferEntity orderOfferEntity, StoreProductInventory storeProductInventory, OrderEntity cart,
			CartResponse response) {
		OrderOfferResponseBean orderOffer = new OrderOfferResponseBean();
		BeanUtils.copyProperties(orderOfferEntity, orderOffer);
		Double salePriceAfterDiscount = applyDiscountAndGetPrice(orderOfferEntity, BigDecimal.valueOf(storeProductInventory.getInventorySalePrice()));
		if (cart != null && conditionsMet(orderOfferEntity, cart)
				|| orderOfferEntity.getOfferApplicationRules().getConditions().getAll().get(0).getValue().compareTo(0d) == 0d) {
			orderOffer.setActualProductSalePrice(salePriceAfterDiscount);
			orderOffer.setActiveAmount(roundToInteger(salePriceAfterDiscount * orderOffer.getQuantity()));
		} else {
			orderOffer.setActualProductSalePrice(storeProductInventory.getInventorySalePrice());
			orderOffer.setActiveAmount(roundToInteger(storeProductInventory.getInventorySalePrice() * orderOffer.getQuantity()));
		}
		orderOffer.setAmount(roundToInteger(salePriceAfterDiscount * orderOffer.getQuantity()));
		orderOffer.setActualAmount(roundToInteger(storeProductInventory.getInventorySalePrice() * orderOffer.getQuantity()));
		orderOffer.setProductName(storeProductInventory.getProductName());
		orderOffer.setImage(storeProductInventory.getProductImageUrl());
		orderOffer.setUom(storeProductInventory.getProductUnitOfMeasurement());
		String newOrderFlowVersion = ParamsUtils.getParam("FIRST_ORDER_FLOW_VERSION");
		boolean isVersionGreater = this.isVersionGreaterOrEqual(
				cart == null ? response.getData().getMetadata().getAppVersion() : cart.getMetadata().getAppVersion(), newOrderFlowVersion);
		if (!isVersionGreater) {
			List<String> terms = orderOffer.getMetadata().getTerms();
			if (CollectionUtils.isNotEmpty(terms)) {
				orderOffer.getMetadata().setTerms(terms.stream().map(this::removeTagFromString).collect(Collectors.toList()));
			}
		}
		return orderOffer;
	}

	private  double roundToInteger(double value) {
		BigDecimal bd = BigDecimal.valueOf(value);
		bd = bd.setScale(0, RoundingMode.HALF_UP);
		return bd.doubleValue();
	}

	private boolean conditionsMet(OrderOfferEntity orderOfferEntity, OrderEntity cart) {
		if (orderOfferEntity.getOfferApplicationRules() != null) {
			ConditionFulfillment<OrderEntity> conditionFulfillment = new ConditionFulfillment<>();
			return conditionFulfillment.fulfillConditions(cart, orderOfferEntity.getOfferApplicationRules().getConditions());
		}
		return false;
	}

	private Double applyDiscountAndGetPrice(OrderOfferEntity orderOfferEntity, BigDecimal salePrice) {
		if (orderOfferEntity.getOfferApplicationRules() != null) {
			DiscountType type = orderOfferEntity.getOfferApplicationRules().getEvent().getParams().getDiscountType();
			BigDecimal discount = BigDecimal.valueOf(orderOfferEntity.getOfferApplicationRules().getEvent().getParams().getDiscountValue());
			if (type == DiscountType.FLAT) {
				return salePrice.subtract(discount).max(BigDecimal.ZERO).doubleValue();
			} else {
				return salePrice.subtract(salePrice.multiply(discount).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP)).max(BigDecimal.ZERO)
						.doubleValue();
			}
		}
		return salePrice.doubleValue();
	}

	public void reComputeOrderOffers(OrderEntity cart, CartResponse cartResponse) {
		List<OrderOfferResponseBean> orderOfferResponseBeans = cartResponse.getOrderOffers();
		if (CollectionUtils.isEmpty(cart.getOrderItems()) || CollectionUtils.isEmpty(orderOfferResponseBeans)) {
			return;
		}
		Map<String, OrderOfferResponseBean> orderOfferResponseBeanMap = orderOfferResponseBeans.stream()
				.collect(Collectors.toMap(OrderOfferResponseBean::getSkuCode, offer -> offer));
		List<OrderItemEntity> offerItemsInCart = cart.getOrderItems().stream()
				.filter(orderItemEntity -> orderOfferResponseBeanMap.containsKey(orderItemEntity.getSkuCode())).collect(Collectors.toList());
		if (CollectionUtils.isNotEmpty(offerItemsInCart)) {
//			if (offerItemsInCart.size() == cart.getOrderItems().size()) {
//				removeItemsFromCart(cart, offerItemsInCart);
//			} else {
			for (final OrderItemEntity orderItem : offerItemsInCart) {
				OrderOfferResponseBean orderOfferBean = orderOfferResponseBeanMap.get(orderItem.getSkuCode());
				orderItem.setSalePrice(BigDecimal.valueOf(orderOfferBean.getActualProductSalePrice()));
				orderItem.getMetadata().setIsOrderOfferItem(true);
			}
			pricingService.setAmountAndTaxesInOrderAndItems(cart, null);
//			}
		}
	}

	private void generateBottomSheetResponse(OrderEntity cart, CartResponse cartResponse) {
		if (CollectionUtils.isEmpty(cartResponse.getOrderOffers()))
			return;
		List<OrderOfferResponseBean> discountedOrderOfferItems = new ArrayList<>();
		for (OrderOfferResponseBean offerResponseBean : cartResponse.getOrderOffers()) {
			if (offerResponseBean.getAmount().compareTo(offerResponseBean.getActiveAmount()) == 0) {
				discountedOrderOfferItems.add(offerResponseBean);
			}
		}
		DiscountedOrderOfferResponse discountedOrderOfferResponse = new DiscountedOrderOfferResponse();
		if (!CollectionUtils.isEmpty(discountedOrderOfferItems)) {
			if (cart.getMetadata().getIsBottomSheetShown().equals(Boolean.FALSE) && discountedOrderOfferItems.get(0).getIsItemInCart().equals(Boolean.FALSE)) {
				discountedOrderOfferResponse.setFreeComboOffers(discountedOrderOfferItems);
				discountedOrderOfferResponse.setShowBottomSheet(Boolean.TRUE);
				if (discountedOrderOfferItems.size() == 1) {
					discountedOrderOfferResponse.setOfferName(discountedOrderOfferItems.get(0).getMetadata().getDisplayName());
				} else {
					discountedOrderOfferResponse.setOfferName("");
				}
				discountedOrderOfferResponse.setOfferTitle("<b>Hurray! Offer unlocked!</b> \uD83C\uDF89");
				discountedOrderOfferResponse.setDescription("Don't forget to add it\nin your selection\n on cart page.");
				discountedOrderOfferResponse.setCtaText("Done!");
				cart.getMetadata().setIsBottomSheetShown(Boolean.TRUE);
			} else {
				discountedOrderOfferResponse.setShowBottomSheet(Boolean.FALSE);
			}
		} else {
			cart.getMetadata().setIsBottomSheetShown(Boolean.FALSE);
		}
		cartResponse.setDiscountedOrderOffer(discountedOrderOfferResponse);
		orderService.save(cart);
	}

	private Double getOrderOfferItemsTotalSpAmount(Map<String, OrderItemEntity> orderOfferItems) {
		BigDecimal orderOfferSpSum = BigDecimal.ZERO;
		for (final OrderItemEntity orderItem : orderOfferItems.values()) {
			orderOfferSpSum = orderOfferSpSum.add(BigDecimal.valueOf(orderItem.getSpGrossAmount()));
		}
		return orderOfferSpSum.doubleValue();
	}

	@Transactional(propagation = Propagation.REQUIRED)
	public OrderEntity addToCartV2(OrderEntity cart, CartRequest request, Date deliveryDate, UUID customerId, String storeId, String appId,
			SocietyListItemBean society, String appVersion, Map<String, String> additionalParams, Boolean isInternalRequest, CartResponse response) {
		String skuCode = request.getSkuCode();
		StoreDataResponse storeDataResponse = clientService.getStoreDataFromId(storeId);
		Location location = new Location();
		if (isPosRequest(appId)) {
			if (storeDataResponse.getLocation() != null) {
				location.setLatitude(String.valueOf(storeDataResponse.getLocation().getCoordinates().get(1)));
				location.setLongitude(String.valueOf(storeDataResponse.getLocation().getCoordinates().get(0)));
			}
		} else {
			location.setLatitude(String.valueOf(society.getLatitude()));
			location.setLongitude(String.valueOf(society.getLongitude()));
		}
		UserServiceResponse userDetails = clientService.getUserDetailsFromCustomerId(customerId);
		if (userDetails != null && userDetails.getUserPreferences() != null && userDetails.getUserPreferences().getSlot() != null) {
			request.setSlotId(userDetails.getUserPreferences().getSlot());
		}
		if (cart == null) {
			_LOGGER.info("addToCartV2 : cart is null");
			String displayOrderId = displayOrderIdService.getNewDisplayOrderId();
			OrderContactDetail contactDetail = getContactDetailsFromCustomerId(userDetails);
			cart = OrderEntity.createNewCart(customerId, request.getAddressId(), storeId, request.getChannel(), contactDetail, location, request.getTime(),
					displayOrderId, storeDataResponse.getDistance(), storeDataResponse.getLocation().getCoordinates(), storeDataResponse.getZoneId(),
					isPosRequest(appId), deliveryDate, request.getSlotId());
			addOrderCountToCart(cart);
			if (userDetails.getUserPreferences() != null) {
				checkIfVipOrder(userDetails.getUserPreferences().getOrderCount(), userDetails.getUserPreferences().getVipOrderNum(), cart);
			}
			orderService.save(cart);
			orderService.reserveOrderSlot(request.getSlotId());
		} else if (cart.getSlotId() != request.getSlotId()) {
			orderService.reserveOrderSlot(request.getSlotId());
			orderService.releaseOrderSlot(cart.getSlotId());
			cart.setSlotId(request.getSlotId());
		}
		cart.getMetadata().setSocietyId(society.getId());
		if (userDetails.getUserPreferences() != null && userDetails.getUserPreferences().getIsPrepaidUser() != null
				&& userDetails.getUserPreferences().getIsPrepaidUser()) {
			cart.getMetadata().setIsPrepaid(true);
		} else {
			cart.getMetadata().setIsPrepaid(society.getMetadata() != null ? society.getMetadata().getIsPrepaid() : Boolean.FALSE);
		}
		List<String> skuCodes = cart.getOrderItems() != null && cart.getOrderItems().size() > 0
				? cart.getOrderItems().stream().map(OrderItemEntity::getSkuCode).collect(Collectors.toList())
				: new ArrayList<>();
		if (skuCodes.isEmpty() || !skuCodes.contains(skuCode)) {
			skuCodes.add(skuCode);
		}
		StoreInventoryResponse storeItemResponse = clientService.getStoreInventory(storeId, String.join(",", skuCodes), customerId,
				cart.getMetadata().getAppVersion());
		if(storeItemResponse == null || CollectionUtils.isEmpty(storeItemResponse.getInventory())) {
			_LOGGER.info("addToCartV2 :: SKU_NOT_FOUND");
			throw new ValidationException(new ErrorBean("SKU_NOT_FOUND", "Item not found in the Store."));
		}
		assignMaxPriceToSalePrice(storeItemResponse.getInventory());
		if (isCartRefreshedV2(cart, location, response, storeDataResponse, storeItemResponse.getInventory(), skuCodes)) {
			_LOGGER.info("addToCartV2 :: cart refreshed");
			orderService.updateOrder(cart);
		}
		if (!isPosRequest(appId)) {
			if (cart.getDeliveryAddress() == null || (request.getAddressId() != null && !cart.getDeliveryAddress().equals(request.getAddressId()))) {
				_LOGGER.info(String.format("addToCartV2 :: cart DeliveryAddress: %s", cart.getDeliveryAddress()));
				if (request.getAddressId() != null) {
					cart.setDeliveryAddress(request.getAddressId());
				}
			}
		}
		if (storeItemResponse.getInventory().stream().filter(i -> i.getProductSkuCode().equals(skuCode)).findFirst().isEmpty()) {
			response.error("SKU_NOT_FOUND", "Item not found in the Store.");
			_LOGGER.info("addToCartV2 :: isValidCartRequest failed");
		}
		StoreProductInventory storeItem = storeItemResponse.getInventory().stream().filter(i -> i.getProductSkuCode().equals(skuCode)).findFirst().get();
		validateStoreItem(storeItem, request);
		if (Objects.isNull(isInternalRequest) || !isInternalRequest) {
			validatePreBookItem(storeItem, customerId.toString());
		}
		cart.getMetadata().setAppVersion(appVersion);
		Integer offerItemCountBefore = getOfferItemCount(cart.getOrderItems());
		_LOGGER.info("addToCartV2 :: proceeding with add to cart");
		setIsCodFlag(cart, userDetails);
		setIsFirstOrderFlagInCart(cart, userDetails);
		if (!Objects.isNull(isInternalRequest) && isInternalRequest) {
			storeItem.setInventorySalePrice(0d);
		}
		Boolean skipWalletCheck = isFirstOrder(userDetails);
		addItemToCartV2(cart, storeItem, request.getQuantity(), request.getPieceQty(), storeItem.getProductName(), request.getDiscountAmount(), appId, response,
				request.getGrades(), request.getNotes(), request.getIsRepeatItem(), request.getIsOzoneWashedItem(), skipWalletCheck);
		if (response.getError() != null && Objects.equals(response.getError().getCode(), ITEM_OUT_OF_STOCK)) {
			_LOGGER.info("addToCartV2 :: OOS item tapped");
			UserEventRequest userEventRequest = new UserEventRequest();
			userEventRequest.setSkuCode(request.getSkuCode());
			userEventRequest.setEventTime(DateUtils.convertDateUtcToIst(new Date()));
			userEventRequest.setUserId(cart.getCustomerId());
			clientService.sendUserEvent(OrderConstants.UserEventType.OOS_ITEM_TAPPED.getValue(), userEventRequest);
		}
		updateOzoneWashedItemCount(cart);
		applyOnboardingOffer(cart, userDetails, response);
		buildOrderOfferResponse(storeId, cart, response);
		reComputeOrderOffers(cart, response);
		orderService.updateOrderProductMetadata(cart);
//		if (CollectionUtils.isEmpty(cart.getOrderItems())) {
//			response.setOrderOffers(null);
//		}
		setValidSlotInOrder(cart, request.getSlotId(), userDetails, request.getSocietyId());
		checkIfAutoCheckout(userDetails, response);
		Integer offerItemCountAfter = getOfferItemCount(cart.getOrderItems());
		boolean isOfferItemUpdated = !offerItemCountBefore.equals(offerItemCountAfter);
		computeCashback(cart, userDetails.getEligibleOffers(), isOfferItemUpdated);
		return cart;
	}

	public void setValidSlotInOrder(OrderEntity cart, Integer slotId, UserServiceResponse userDetails, Integer societyId) {
		if (slotId != null) {
			OrderSlotEntity orderSlot = orderService.getOrderSlotById(slotId);
			if (orderSlot == null) {
				Integer defaultOrUserSlot = getUserSlotOrDefault(userDetails, String.valueOf(societyId));
				orderSlot = orderService.getOrderSlotById(defaultOrUserSlot);
			}
			setOrderSlotDetails(cart, orderSlot);
		}
	}

	private void updateOzoneWashedItemCount(OrderEntity cart) {
		if (cart != null && CollectionUtils.isNotEmpty(cart.getOrderItems())) {
			long ozoneWashedItemCount = cart.getOrderItems().stream().filter(i -> Boolean.TRUE.equals(i.getMetadata().getIsOzoneWashedItem())).count();
			cart.getMetadata().setOzoneWashedItemCount((int) ozoneWashedItemCount);
		} else if(cart != null) {
			cart.getMetadata().setOzoneWashedItemCount(0);
		}
	}

	public void sortOrderItems(OrderEntity cart) {
		if (CollectionUtils.isNotEmpty(cart.getOrderItems())) {
			cart.getOrderItems().forEach(item -> {
				if (item.getFinalQuantity() == 0d) {
					item.getMetadata().setIsShowOnOrderSummary(Boolean.FALSE);
				} else {
					item.getMetadata().setIsShowOnOrderSummary(Boolean.TRUE);
				}
			});
			cart.getOrderItems().sort(
					Comparator.comparing((OrderItemEntity item) -> item.getMetadata().getIsOrderOfferItem()).thenComparing(OrderItemEntity::getCreatedAt));
		}
	}

	public void setIsFirstOrderFlagInCart(OrderEntity cart, UserServiceResponse userDetails) {
		if (isFirstOrder(userDetails)) {
			cart.getMetadata().setIsFirstOrder(Boolean.TRUE);
		}
	}

	public void checkIfAutoCheckout(UserServiceResponse userDetails, CartResponse response) {
		if (!isFirstOrder(userDetails)) {
			return;
		}
		UserOrderSettingEntity userOrderSetting = getOrCreateAutoCheckoutSetting(UUID.fromString(userDetails.getId()));
		Boolean isAutoCheckoutEnabled = Boolean.valueOf(userOrderSetting.getValue());
		response.setIsAutoCheckoutEnabled(isAutoCheckoutEnabled);
	}

	public boolean isFirstOrder(UserServiceResponse userDetails) {
		return userDetails.getUserMetadata() != null
				&& (userDetails.getUserMetadata().getIsFirstOrderFlow() != null && Boolean.TRUE.equals(userDetails.getUserMetadata().getIsFirstOrderFlow()));
	}

	public void setAutoCheckoutFlag(UserServiceResponse userDetails, CartResponse cartResponse) {
		if (!isFirstOrder(userDetails)) {
			return;
		}
		UserOrderSettingEntity userOrderSetting = getOrCreateAutoCheckoutSetting(UUID.fromString(userDetails.getId()));
		userOrderSetting.setValue("true");
		orderService.saveUserOrderSetting(userOrderSetting);
		cartResponse.setIsAutoCheckoutEnabled(Boolean.TRUE);
	}

	private UserOrderSettingEntity getOrCreateAutoCheckoutSetting(UUID customerId) {
		UserOrderSettingEntity userOrderSetting = orderService.getUserOrderSetting(customerId, UserOrderSettingEntity.Keys.isAutoCheckoutEnabled);
		if (userOrderSetting == null) {
			userOrderSetting = UserOrderSettingEntity.createNewEntity(customerId, UserOrderSettingEntity.Keys.isAutoCheckoutEnabled, "false", customerId,
					customerId);
			orderService.saveUserOrderSetting(userOrderSetting);
		}
		return userOrderSetting;
	}

	private void assignMaxPriceToSalePrice(List<StoreProductInventory> inventory) {
		if (CollectionUtils.isNotEmpty(inventory)) {
			List<OrderOfferEntity> orderOffers = getAllOrderOffers();
			Set<String> orderOfferSkuCodes = new HashSet<>();
			if (CollectionUtils.isNotEmpty(orderOffers)) {
				orderOfferSkuCodes = orderOffers.stream().map(OrderOfferEntity::getSkuCode).collect(Collectors.toSet());
			}
			for (StoreProductInventory i : inventory) {
				if (i.getInventoryMaxPrice() != null && !orderOfferSkuCodes.contains(i.getProductSkuCode())) {
					i.setInventorySalePrice(i.getInventoryMaxPrice());
					i.setInventoryMarketPrice(i.getInventoryMaxPrice());
				}
			}
		}
	}

	private Integer getOfferItemCount(List<OrderItemEntity> orderItems) {
		if (CollectionUtils.isNotEmpty(orderItems)) {
			return (int) orderItems.stream().filter(i -> i.getMetadata().getIsOrderOfferItem()).count();
		} else {
			return 0;
		}
	}

	private void validatePreBookItem(StoreProductInventory storeItem, String customerId) {
		if (storeItem.getIsPreBook()) {
			List<PreBookOrderEntity> prebookOrders = preBookOrderService.findPrebookedOrdersBySku(customerId, storeItem.getInventorySkuCode(),
					DateUtils.getDate(DateUtils.DATE_MM_FMT, storeItem.getPreBookDate()));
			if (CollectionUtils.isNotEmpty(prebookOrders)) {
				throw new ValidationException(ErrorBean.withError("ALREADY_BOOKED_ITEM", "You have already pre-ordered this item ", "alreadyBooked"));
			}
		}
	}

	private void validateStoreItem(StoreProductInventory storeItem, CartRequest request) {
		if (storeItem.getCutoffTime() != null && !storeItem.getCutoffTime().isEmpty()) {
			if (isPastCutoffTime(storeItem.getCutoffTime())) {
				_LOGGER.info("addToCartV2 :: isValidCartRequest failed");
				throw new ValidationException(ErrorBean.withError("PAST_CUTOFF_TIME", "Order cannot be placed after cutoff time.", "cutoffTime"));
			}
		}
		if (storeItem.getProductConsumerContents() != null) {
			if (storeItem.getInventoryGrades() == null && request.getGrades() != null) {
				throw new ValidationException(ErrorBean.withError("grades_mismatch", "Provided Grades do not exist.", "grades"));
			}
			if (storeItem.getInventoryGrades() != null && request.getGrades() == null) {
				throw new ValidationException(ErrorBean.withError("grades_mismatch", "Product should have some selection of grades.", "grades"));
			}
		}
	}

	private boolean isPastCutoffTime(String cutoffTime) {
		return isCurrentTimePastCutoff(cutoffTime);
	}

	public static boolean isCurrentTimePastCutoff(String cutoffTime) {
		LocalTime cutoffLocalTime = LocalTime.parse(cutoffTime);
		ZonedDateTime nowIST = ZonedDateTime.now(ZoneId.of("Asia/Kolkata"));
		LocalTime nowLocalTime = nowIST.toLocalTime();
		return nowLocalTime.isAfter(cutoffLocalTime);
	}

	public void customerIdCheck(UUID customerId) {
		if (customerId == null) {
			throw new ValidationException(ErrorBean.withError("customer_id_missing", "Customer Id not found.", "customerId"));
		}
	}

	public Date getDeliveryDate(Date deliveryDate) {
		return getDeliveryDate(deliveryDate, true);
	}

	public Date getDeliveryDate(Date deliveryDate, boolean checkForHoliday) {
		if (deliveryDate == null) {
			deliveryDate = DeliveryDateUtils.getConsumerDeliveryDate(checkForHoliday);
		}
		if (!checkForHoliday && DeliveryDateUtils.isHoliday(deliveryDate, null)) {
			String holidayMsg = ParamsUtils.getParam("HOLIDAY_MESSAGE");
			String holidayMsgImage = ParamsUtils.getParam("HOLIDAY_MESSAGE_IMAGE");
			String holidayMsgTitle = ParamsUtils.getParam("HOLIDAY_MESSAGE_TITLE");
			throw new ValidationException(
					new AlertErrorBean(Errors.INVALID_REQUEST, holidayMsg, "deliveryDate", holidayMsgTitle, holidayMsgImage, "Got it!", null, null));
		}
		return deliveryDate;
	}

	public GameBean prepareGameDetails(UUID customerId, CartResponse response) {
		UserServiceResponse customerDetails = clientService.getUserDetailsFromCustomerId(customerId);
		Integer orderCount;
		if (customerDetails != null && customerDetails.getUserPreferences() != null) {
			orderCount = customerDetails.getUserPreferences().getOrderCount() != null ? Integer.parseInt(customerDetails.getUserPreferences().getOrderCount())
					: 0;
		} else {
			orderCount = 0;
		}
		Boolean offerEligible = gameOfferEligible(orderCount, customerDetails.getEligibleOffers());
		GameBean gameBean = GameBean.newInstance();
		gameBean.setOfferEligible(offerEligible);
		addDisallowedSkus(gameBean);
		if (customerDetails.getEligibleOffers() != null && customerDetails.getEligibleOffers().getOfferName() != null) {
			String eligibleOfferMsg = String.format("Don’t forget to order your %s", customerDetails.getEligibleOffers().getOfferName().toLowerCase());
			response.getData().setMetadata(new OrderMetadata());
			response.getData().getMetadata().setProductMetadata(new ArrayList<OrderProductMetadata>());
			Map<String, Long> categoryCount = new HashMap<>();
			categoryCount.put("otp", 0L);
			response.getData().getMetadata().setCategoryCount(categoryCount);
			response.getData().getMetadata().setEligibleOfferMsg(eligibleOfferMsg);
		}
		return gameBean;
	}

	private void addDisallowedSkus(GameBean gameBean) {
		String disallowedSkus = ParamsUtils.getParam("TREE_GAME_DISALLOWED_SKUS");
		List<String> disallowedSkusList = Arrays.stream(disallowedSkus.split(",")).map(String::trim).collect(Collectors.toList());
		gameBean.setDisallowedSkus(disallowedSkusList);
	}

	public Boolean gameOfferEligible(Integer orderCount, EligibleOffersResponse eligibleOffers) {
		Integer gamePlayOrderCount = ParamsUtils.getIntegerParam("B2C_GAME_ELIGIBLE_ORDER_COUNT", 15);
		if ((eligibleOffers == null || eligibleOffers.getTreeGame()) && orderCount >= 0 && orderCount < gamePlayOrderCount) {
			return true;
		}
		return false;
	}

	public void addGameDetailsToResponse(OrderEntity cart, OrderResponseBean response) {
		GameBean gameBean = GameBean.newInstance();
		Integer orderCount = cart.getMetadata().getOrderCount() != null ? cart.getMetadata().getOrderCount() : 0;
		addDisallowedSkus(gameBean);
		Boolean offerEligible = gameOfferEligible(orderCount, null);
		gameBean.setOfferEligible(offerEligible);
		if (offerEligible) {
			if (!hasAlreadyPlayedGame(cart)) {
				Integer orderOfferItemCount =
						cart.getOrderItems() != null ? (int) cart.getOrderItems().stream().filter(i -> i.getMetadata().getIsOrderOfferItem() || i.getFinalAmount() == 0).count() : 0;
				String condition = ParamsUtils.getParam("B2C_GAME_CONDITION", "AND");
				Integer gameAov = ParamsUtils.getIntegerParam("B2C_GAME_AOV", 500);
				Integer cartItemCount = ParamsUtils.getIntegerParam("B2C_GAME_CART_ITEM_COUNT", 10);
				Boolean gamePlayEligible = gamePlayEligible(cart, condition, gameAov, cartItemCount, orderOfferItemCount);
				gameBean.setGamePlayEligible(gamePlayEligible);
				if (!gamePlayEligible) {
					Integer itemCount = cart.getItemCount() != null ? (cart.getItemCount() - orderOfferItemCount) : 0;
					// Double amountDiff = Double.valueOf(gameAov) - cart.getFinalBillAmount() > 0 ?
					// Double.valueOf(gameAov) - cart.getFinalBillAmount() : 0d;
					Integer orderCountDiff = cartItemCount - itemCount > 0 ? cartItemCount - itemCount : 0;
					String productStr = "product";
					if (orderCountDiff > 1) {
						productStr = "products";
					}
					String message = String.format("Your Gifting Tree is %s %s away!", orderCountDiff, productStr);
					gameBean.setMessage(message);
					Integer gameStage = calculateGameStage(cart.getFinalBillAmount(), itemCount, gameAov, cartItemCount, condition);
					gameBean.setGameStage(gameStage);
				} else {
					gameBean.setGameStage(5);
				}
			} else {
				gameBean.setGamePlayEligible(Boolean.FALSE);
				String message = "You have already played the game.";
				gameBean.setMessage(message);
			}
		}
		_LOGGER.info(String.format("game details: %s", gameBean));
		response.setGameDetails(gameBean);
	}

	private Boolean gamePlayEligible(OrderEntity cart, String condition, Integer gameAov, Integer cartItemCount, Integer orderOfferItemCount) {
		Boolean result;
		switch (condition) {
		case "AND":
			result = validateAndCaseEligibility(cart, gameAov, cartItemCount, orderOfferItemCount);
			break;
		case "OR":
			result = validateOrCaseEligibility(cart, gameAov, cartItemCount, orderOfferItemCount);
			break;
		case "ORDER_COUNT":
			result = validateOrderCountEligibility(cart, cartItemCount, orderOfferItemCount);
			break;
		default:
			result = false;
			break;
		}
		return result;
	}

	private Boolean validateOrderCountEligibility(OrderEntity cart, Integer cartItemCount, Integer orderOfferItemCount) {
		if (cart.getItemCount() != null && (cart.getItemCount() - orderOfferItemCount) >= cartItemCount) {
			return true;
		}
		return false;
	}

	private Boolean validateOrCaseEligibility(OrderEntity cart, Integer gameAov, Integer cartItemCount, Integer orderOfferItemCount) {
		if ((cart.getFinalBillAmount() != null && cart.getFinalBillAmount().compareTo(Double.valueOf(gameAov)) >= 0)
				|| (cart.getItemCount() != null && (cart.getItemCount() - orderOfferItemCount) >= cartItemCount)) {
			return true;
		}
		return false;
	}

	private Boolean validateAndCaseEligibility(OrderEntity cart, Integer gameAov, Integer cartItemCount, Integer orderOfferItemCount) {
		if (cart.getFinalBillAmount() != null && cart.getItemCount() != null && cart.getFinalBillAmount().compareTo(Double.valueOf(gameAov)) >= 0
				&& (cart.getItemCount() - orderOfferItemCount) >= cartItemCount) {
			return true;
		}
		return false;
	}

	private boolean hasAlreadyPlayedGame(OrderEntity cart) {
		if (CollectionUtils.isEmpty(cart.getOrderItems())) {
			return false;
		}
		return cart.getOrderItems().stream().anyMatch(i -> i.getMetadata().getIsCashbackItem());
	}

	private Integer calculateGameStage(Double billAmount, Integer itemCount, Integer gameAov, Integer gameItemCount, String condition) {
		BigDecimal aovFactor = BigDecimal.ZERO;
		if (!condition.equals("ORDER_COUNT")) {
			aovFactor = calculateAovFactor(billAmount, gameAov);
		}
		BigDecimal itemCountFactor = calculateItemCountFactor(itemCount, gameItemCount);
		BigDecimal averageFactor = itemCountFactor;
		if (!condition.equals("ORDER_COUNT")) {
			averageFactor = aovFactor.add(itemCountFactor).divide(BigDecimal.valueOf(2));
		}
		return averageFactor.divide(BigDecimal.valueOf(20)).intValue();
	}

	private BigDecimal calculateItemCountFactor(Integer itemCount, Integer gameItemCount) {
		if (itemCount < gameItemCount) {
			return BigDecimal.valueOf(itemCount).divide(BigDecimal.valueOf(gameItemCount)).multiply(BigDecimal.valueOf(100));
		} else {
			return BigDecimal.valueOf(100);
		}
	}

	private BigDecimal calculateAovFactor(Double billAmount, Integer gameAov) {
		if (billAmount.compareTo(Double.valueOf(gameAov)) < 0) {
			return BigDecimal.valueOf(billAmount).divide(BigDecimal.valueOf(gameAov)).multiply(BigDecimal.valueOf(100));
		} else {
			return BigDecimal.valueOf(100);
		}
	}

	public OrderContactDetail getContactDetailsFromCustomerId(UserServiceResponse userDetails) {
		OrderContactDetail contactDetail = new OrderContactDetail();
		contactDetail.setName(userDetails.getName());
		contactDetail.setPhone(userDetails.getPhoneNumber());
		return contactDetail;
	}

	public void addOrderCountToCart(OrderEntity cart) {
		Long orderCount = orderService.getDeliveredOrderCount(cart.getCustomerId());
		if (orderCount != null) {
			cart.getMetadata().setOrderCount(orderCount.intValue());
		} else {
			cart.getMetadata().setOrderCount(0);
		}
	}

	public void checkIfVipOrder(String orderCount, Integer vipOrderNum, OrderEntity cart) {
		if (orderCount != null && vipOrderNum != null) {
			cart.getMetadata().setIsVip(Integer.compare(vipOrderNum, Integer.parseInt(orderCount)) > 0);
		}
	}

	public boolean isCartRefreshedV2(OrderEntity cart, Location location, CartResponse response, StoreDataResponse storeDataResponse,
			List<StoreProductInventory> storeItems, List<String> skuCodes) {
		cart.getMetadata().setLocation(location);
		if (storeDataResponse != null && cart.getStoreId().equals(storeDataResponse.getStoreId())) {
			_LOGGER.info("isCartRefreshedV2 :: cart StoreId matching storeDataResponse");
			OrderEntity.setStoreLocation(cart, storeDataResponse.getLocation().getCoordinates());
			return false;
		} else if (storeDataResponse != null) {
			_LOGGER.info("isCartRefreshedV2 :: cart StoreId NOT matching storeDataResponse");
			final String storeId = storeDataResponse.getStoreId();
			StoreInventoryResponse oldStoreItemResponse = clientService.getStoreInventory(cart.getStoreId(), String.join(",", skuCodes), cart.getCustomerId());
			cart.setStoreId(storeId);
			OrderEntity.setStoreLocation(cart, storeDataResponse.getLocation().getCoordinates());
			response.error("store_changed", "Your store has been changed. We have reloaded your cart.");
			validateAndDoChangeAction(cart, storeItems, oldStoreItemResponse != null ? oldStoreItemResponse.getInventory() : null);
			return true;
		}
		response.error("store_changed", "Something went wrong. We are trying to reload your cart. Kindly try again.");
		return false;
	}

	public String setScratchCardItem(OrderEntity cart) {
		int minItemCountToExclude = ParamsUtils.getIntegerParam("MIN_ITEM_COUNT_TO_EXCLUDE", 2);
		List<OrderItemEntity> sortedItems = cart.getOrderItems().stream()
				.sorted(Comparator.comparing(OrderItemEntity::getFinalAmount))
				.filter(i -> !i.getMetadata().getIsOrderOfferItem() && i.getFinalAmount() != 0)
				.collect(Collectors.toList());
		int randomIndex = (int) (Math.random() * Math.min((sortedItems.size() - minItemCountToExclude), 5));
		return sortedItems.get(randomIndex).getSkuCode();
	}

	public void applyOnboardingOffer(OrderEntity cart, UserServiceResponse userDetails, CartResponse cartResponse) {
		if (!isValidOnboardingOffer(cart, userDetails)) {
			return;
		}

		OnboardingOfferResponse onboardingOffer = userDetails.getEligibleOffers().getOnboardingOffer();
		if (onboardingOffer.getOfferType().equals(OrderConstants.OfferType.ORDER_DISCOUNT_OFFER)) {
			applyOrderDiscountOffer(cart, onboardingOffer, cartResponse);
		} else if (onboardingOffer.getOfferType().equals(OrderConstants.OfferType.CASHBACK_OFFER)) {
			applyCashbackOffer(cart, onboardingOffer, cartResponse);
		}
	}

	public void removeOrderLevelOffer(OrderEntity cart) {
		if (Boolean.TRUE.equals(cart.getOfferData().getIsOfferApplied())) {
			cart.setOfferData(new OfferData());
			pricingService.setAmountAndTaxesInOrder(cart, null, null);
			cart.getMetadata().setCashbackAmount(null);
			cart.getMetadata().setIsCashbackApplicable(Boolean.FALSE);
		}
	}

	private void applyOrderDiscountOffer(OrderEntity cart, OnboardingOfferResponse onboardingOffer, CartResponse cartResponse) {
		if (!isCartEligibleForOffer(cart, onboardingOffer, cartResponse)) {
			return;
		}
		setOfferDataInOrder(cart, onboardingOffer);
		pricingService.setAmountAndTaxesInOrder(cart, null, null);
	}

	private void applyCashbackOffer(OrderEntity cart, OnboardingOfferResponse onboardingOffer, CartResponse cartResponse) {
		if (!isCartEligibleForOffer(cart, onboardingOffer, cartResponse)) {
			return;
		}
		setOfferDataInOrder(cart, onboardingOffer);
		pricingService.setOrderLevelCashback(cart, BigDecimal.valueOf(cart.getTotalSpGrossAmount()));
		orderService.save(cart);
	}

	private boolean isCartEligibleForOffer(OrderEntity cart, OnboardingOfferResponse onboardingOffer, CartResponse cartResponse) {
		if (cart.getTotalSpGrossAmount().compareTo(onboardingOffer.getConstraint().getMinCartValue()) < 0) {
			int remAmountToApplyOffer = (int) (onboardingOffer.getConstraint().getMinCartValue() - cart.getTotalSpGrossAmount());
			String discount = onboardingOffer.getDiscountType().equals(DiscountType.FLAT)
					? "₹" + (int) onboardingOffer.getAmount()
					: (int) onboardingOffer.getAmount() + "%";
			setAmountRemMsg(cartResponse, remAmountToApplyOffer, discount, onboardingOffer);
			removeOrderLevelOffer(cart);
			return false;
		}
		return true;
	}

	private void setAmountRemMsg(CartResponse cartResponse, int remAmountToApplyOffer, String discount, OnboardingOfferResponse onboardingOffer) {
		String message = "";
		if (onboardingOffer.getOfferType().equals(OrderConstants.OfferType.ORDER_DISCOUNT_OFFER)) {
			message = String.format("Add products worth ₹%s more to avail %s off", remAmountToApplyOffer, discount);
		} else if (onboardingOffer.getOfferType().equals(OrderConstants.OfferType.CASHBACK_OFFER)) {
			message = String.format("Add products worth ₹%s more to avail %s cashback", remAmountToApplyOffer, discount);
		}
		cartResponse.setAmountRemainingMsg(message);
	}

	private void setOfferDataInOrder(OrderEntity cart, OnboardingOfferResponse onboardingOffer) {
		OfferData offerData = new OfferData();
		offerData.setTerms(onboardingOffer.getTerms());
		offerData.setConstraint(onboardingOffer.getConstraint());
		offerData.setOfferType(onboardingOffer.getOfferType().getValue());
		offerData.setDiscountType(onboardingOffer.getDiscountType());
		offerData.setOrderDiscount(onboardingOffer.getAmount());
		offerData.setVoucherCode(onboardingOffer.getVoucherCode());
		offerData.setIsOfferApplied(Boolean.TRUE);
		setCouponLabel(offerData, OrderConstants.CouponType.SIGNUP_CODE);
		cart.setOfferData(offerData);
	}

	public void setCouponLabel(OfferData offerData, OrderConstants.CouponType couponType) {
		if (couponType != null) {
			if (couponType.equals(OrderConstants.CouponType.COUPON_CODE)) {
				offerData.setCouponLabel("Coupon code");
			} else if (couponType.equals(OrderConstants.CouponType.SIGNUP_CODE)) {
				offerData.setCouponLabel("Signup code");
			}
		}
	}

	private boolean isValidOnboardingOffer(OrderEntity cart, UserServiceResponse userDetails) {
		if (cart == null || userDetails.getEligibleOffers() == null || userDetails.getEligibleOffers().getOnboardingOffer() == null) {
			return false;
		}
		OnboardingOfferResponse onboardingOffer = userDetails.getEligibleOffers().getOnboardingOffer();
		return onboardingOffer.getIsOnboardingOfferValid().equals(Boolean.TRUE) && !onboardingOffer.getOfferExpiry().before(new Date());
	}

	public void failInactiveAutoCheckoutOrders(Date deliveryDate) {
		List<OrderEntity> carts = orderService.findInactiveAutoCheckoutOrders(new java.sql.Date(deliveryDate.getTime()), new Date());
		if (CollectionUtils.isEmpty(carts)) {
			return;
		}
		for (OrderEntity cart : carts) {
			setAutoCheckoutFailureReason(cart);
		}
		orderService.saveAllOrders(carts);
	}

	private void setAutoCheckoutFailureReason(OrderEntity cart) {
		cart.setStatus(OrderConstants.OrderStatus.ORDER_FAILED);
		if (cart.getMetadata().getFailureReason() == null) {
			cart.getMetadata().setFailureReason(new ArrayList<>());
		}
		cart.getMetadata().getFailureReason().add("Auto Checkout not enabled");
	}

	public void failOrdersAndUpdateInventory(List<OrderEntity> orders) {
		if (CollectionUtils.isEmpty(orders)) {
			return;
		}
		Map<String, Double> inventoryChanges = new HashMap<>();
		for (OrderEntity order : orders) {
			for (OrderItemEntity orderItem : order.getOrderItems()) {
				String key = order.getStoreId() + "-" + orderItem.getSkuCode();
				inventoryChanges.put(key, inventoryChanges.getOrDefault(key, 0d) + orderItem.getOrderedQty());
			}
			setAutoCheckoutFailureReason(order);
		}

		// Process inventory changes in one go
		for (Map.Entry<String, Double> entry : inventoryChanges.entrySet()) {
			String[] parts = entry.getKey().split("-");
			String storeId = parts[0];
			String skuCode = parts[1];
			Double totalQty = entry.getValue();
			addOrDeductInventory(storeId, skuCode, totalQty);
		}

		orderService.saveAllOrders(orders);
	}

	public void setIsCodFlag(OrderEntity cart, UserServiceResponse userDetails) {
		if (userDetails.getUserPreferences() != null) {
			if (isFirstOrder(userDetails)) {
				String paymentPreference = userDetails.getUserPreferences().getPaymentPreference();
				Boolean isCod = paymentPreference != null && paymentPreference.equals("At Delivery");
				cart.getMetadata().setIsCod(isCod);
			}
		}
	}

	public void sendCodNotificationToCustomer(UUID customerId) {
		Map<String, String> fillers = new HashMap<>();
		PnRequest pnRequest = PnRequest.builder().userId(customerId.toString()).templateName(OrderConstants.COD_ORDER_TEMPLATE_NAME).fillers(fillers).build();
		clientService.sendPushNotifications(Collections.singletonList(pnRequest));
	}

	public boolean isVersionGreaterOrEqual(String version1, String version2) {
		if(StringUtils.isEmpty(version1) || StringUtils.isEmpty(version2)){
			return false;
		}
		String[] levels1 = version1.split("\\.");
		String[] levels2 = version2.split("\\.");

		int length = Math.max(levels1.length, levels2.length);
		for (int i = 0; i < length; i++) {
			int v1 = i < levels1.length ? Integer.parseInt(levels1[i]) : 0;
			int v2 = i < levels2.length ? Integer.parseInt(levels2[i]) : 0;
			if (v1 < v2) {
				return false;
			} else if (v1 > v2) {
				return true;
			}
		}
		return true;
	}

	public boolean checkIfReducingQuantity(OrderEntity cart,CartRequest request){
		if(cart == null || CollectionUtils.isEmpty(cart.getOrderItems())){
			return false;
		}
		for(OrderItemEntity orderItem : cart.getOrderItems()){
			if(orderItem.getSkuCode().equals(request.getSkuCode()) && orderItem.getFinalQuantity() > request.getQuantity()){
				return true;
			}
		}
		return false;
	}

	@Transactional(propagation = Propagation.REQUIRED)
	public void addRepeatOrderToCart(RepeatOrderEntity repeatOrder) {
		_LOGGER.info(String.format("Adding repeat order to cart: %s", repeatOrder));
		try {
			OrderEntity cart = orderService.findCustomerCurrentCart(repeatOrder.getCustomerId(), repeatOrder.getNextDeliveryDate());
			List<UserAddressResponse> addresses = clientService.getUserAddressByUserId(repeatOrder.getCustomerId());
			if (CollectionUtils.isEmpty(addresses)) {
				_LOGGER.info(String.format("Skipping repeat order for user %s. Empty Address", repeatOrder.getCustomerId()));
				return;
			}
			UserAddressResponse userAddress = addresses.get(0);
			UserServiceResponse userDetails = clientService.getUserDetailsFromCustomerId(repeatOrder.getCustomerId());
			Integer slotId = getUserSlotOrDefault(userDetails, String.valueOf(userAddress.getSocietyId()));
			SocietyListItemBean society = clientService.getSocietyById(userAddress.getSocietyId());
			CartRequest request = CartRequest.builder().channel(OrderConstants.OrderChannel.CONSUMER_APP.getValue()).customerId(repeatOrder.getCustomerId()).deliveryDate(repeatOrder.getNextDeliveryDate())
					.skuCode(repeatOrder.getSkuCode()).quantity(repeatOrder.getPreferences().getQuantity()).slotId(slotId).societyId(userAddress.getSocietyId())
					.addressId(userAddress.getId()).storeId(society.getStoreId()).time(Instant.now().toEpochMilli()).isRepeatItem(Boolean.TRUE).build();
			String defaultAppVersion = ParamsUtils.getParam("DEFAULT_APP_VERSION");
			addToCartV2(cart, request, repeatOrder.getNextDeliveryDate(), repeatOrder.getCustomerId(), society.getStoreId(), OrderConstants.CONSUMER_APP_ID, society, defaultAppVersion,
					null, false, new CartResponse());
			_LOGGER.info(String.format("Added repeat order to cart: %s", repeatOrder));
		} catch (Exception e) {
			_LOGGER.error("Error while adding repeat order to cart", e);
		}
	}

	public Integer getUserSlotOrDefault(UserServiceResponse userDetails, String societyId) {
		if (userDetails != null && userDetails.getUserPreferences() != null && userDetails.getUserPreferences().getSlot() != null) {
			return userDetails.getUserPreferences().getSlot();
		}
		return getDefaultSlotBySocietyId(societyId);
	}

	public Integer getDefaultSlotBySocietyId(String societyId) {
		List<OrderSlotResponseBean> orderSlotResponse = orderService.buildOrderSlotResponse(societyId);
		OrderSlotResponseBean defaultSlot = orderSlotResponse.stream().filter(OrderSlotResponseBean::getIsDefault).findFirst().orElse(null);
		if (defaultSlot == null) {
			return orderSlotResponse.get(0).getId();
		}
		return defaultSlot.getId();
	}

	public List<BulkA2cSheetBean> preProcessBulkA2cUpload(List<BulkA2cSheetBean> rawBeans) {
		return sanitizeBulkA2cUpload(rawBeans);
	}

	private List<BulkA2cSheetBean> sanitizeBulkA2cUpload(List<BulkA2cSheetBean> rawBeans) {
		List<String> customerIds = rawBeans.stream().map(BulkA2cSheetBean::getUserId).filter(Objects::nonNull).collect(Collectors.toList());
		if (CollectionUtils.isEmpty(customerIds)) {
			return rawBeans;
		}
		List<UserServiceResponse> userDetails = clientService.getUserDetailsByIds(customerIds);
		Map<String, UserServiceResponse> userDetailsMap = userDetails.stream()
				.collect(Collectors.toMap(UserServiceResponse::getId, Function.identity(), (first, second) -> first));
		for (BulkA2cSheetBean bean : rawBeans) {
			if (bean.getUserId() == null || bean.getUserId().isEmpty()) {
				bean.getErrors().add(ErrorBean.withError("CUSTOMER_ID_MISSING", "Customer Id is missing", "customerId"));
			} else {
				UserServiceResponse user = userDetailsMap.get(bean.getUserId());
				if (user == null) {
					bean.getErrors().add(ErrorBean.withError("CUSTOMER_NOT_FOUND", "Customer not found", "customerId"));
				}
			}
			if (bean.getItemNotes() != null && !bean.getItemNotes().isEmpty()) {
				String notes = bean.getItemNotes();
				if (!notes.matches("^[A-Z0-9]+:[^:]+(?:|[A-Z0-9]+:[^:]+)*$")) {
					bean.getErrors()
							.add(ErrorBean.withError("INVALID_NOTE_FORMAT", "Note format should be skuCode:Note or skuCode:Note|skuCode:Note", "noteFormat"));
				}
			}
		}
		return rawBeans;
	}

	public void validateBulkA2cDataOnUpload(BulkA2cUploadBean uploadBean, org.springframework.validation.Errors errors) {
		if(!errors.hasErrors()) {
			if(Objects.isNull(uploadBean.getUserId())) {
				uploadBean.getErrors().add(ErrorBean.withError(Errors.MANDATORY, "Customer Id not found", "customerId"));
			}
			if(Objects.isNull(uploadBean.getItems())) {
				uploadBean.getErrors().add(ErrorBean.withError(Errors.MANDATORY, "Items not found", "items"));
			}
			if (CollectionUtils.isNotEmpty(uploadBean.getErrors())) {
				errors.reject("_ERRORS", "Uploaded Data Error(s)");
			}
		}
	}

	@Transactional(propagation = Propagation.REQUIRED)
	public List<BulkA2cResponse> saveBulkA2cData(List<BulkA2cUploadBean> beans) {
		String defaultAppVersion = ParamsUtils.getParam("DEFAULT_APP_VERSION");
		Date deliveryDate = DeliveryDateUtils.getConsumerDeliveryDate(false);
		boolean isHoliday = DeliveryDateUtils.isHoliday(deliveryDate, null);
		List<BulkA2cResponse> response = new ArrayList<>();
		if (isHoliday) {
		response.add(new BulkA2cResponse(null, "We are not delivering orders on this delivery date"));
			return response;
		}
		for (BulkA2cUploadBean bean : beans) {
			Map<String, String> itemNotes = parseItemNotes(bean.getItemNotes());
			processBulkA2cBean(bean, deliveryDate, defaultAppVersion, response, itemNotes);
		}
		return response;
	}

	private Map<String, String> parseItemNotes(String notes) {
		Map<String, String> itemNotes = new HashMap<>();
		if (notes != null && !notes.isEmpty()) {
			String[] noteArray = notes.split("\\|");
			for (String note : noteArray) {
				String[] noteParts = note.split(":");
				if (noteParts.length == 2) {
					itemNotes.put(noteParts[0], noteParts[1]);
				}
			}
		}
		return itemNotes;
	}

	@Transactional(propagation = Propagation.REQUIRED)
	public void processBulkA2cBean(BulkA2cUploadBean bean, Date deliveryDate, String defaultAppVersion, List<BulkA2cResponse> response,
			Map<String, String> itemNotes) {
		try {
			List<UserAddressResponse> addresses = clientService.getUserAddressByUserId(UUID.fromString(bean.getUserId()));
			if (CollectionUtils.isEmpty(addresses)) {
				_LOGGER.info(String.format("Skipping order for user %s. Empty Address", bean.getUserId()));
				response.add(new BulkA2cResponse(bean.getUserId(), "Address not found"));
				return;
			}
			UserAddressResponse userAddress = addresses.get(0);
			UserServiceResponse userDetails = clientService.getUserDetailsFromCustomerId(UUID.fromString(bean.getUserId()));
			Integer slotId = null;
			if (userDetails.getUserPreferences() != null && userDetails.getUserPreferences().getSlot() != null) {
				slotId = userDetails.getUserPreferences().getSlot();
			}
			SocietyListItemBean society = clientService.getSocietyById(userAddress.getSocietyId());
			for (BulkA2cItemRequest item : bean.getItems()) {
				OrderEntity cart = orderService.findCustomerCurrentCart(UUID.fromString(bean.getUserId()), deliveryDate);
				processBulkA2cItem(bean, item, deliveryDate, defaultAppVersion, cart, userAddress, slotId, society, response, itemNotes);
			}
		} catch (Exception e) {
			response.add(new BulkA2cResponse(bean.getUserId(), e.getMessage()));
			_LOGGER.error("Error while adding order to cart", e);
		}
	}

	@Transactional(propagation = Propagation.REQUIRED)
	public void processBulkA2cItem(BulkA2cUploadBean bean, BulkA2cItemRequest item, Date deliveryDate, String defaultAppVersion, OrderEntity cart,
			UserAddressResponse userAddress, Integer slotId, SocietyListItemBean society, List<BulkA2cResponse> response, Map<String, String> itemNotes) {
		CartRequest request = CartRequest.builder().channel(OrderConstants.OrderChannel.BACKOFFICE.getValue()).customerId(UUID.fromString(bean.getUserId())).deliveryDate(deliveryDate)
				.skuCode(item.getSkuCode()).quantity(item.getQuantity()).slotId(slotId).societyId(userAddress.getSocietyId()).addressId(userAddress.getId())
				.grades(item.getGrades() != null ? item.getGrades() : null).storeId(society.getStoreId()).time(Instant.now().toEpochMilli()).isRepeatItem(Boolean.FALSE)
				.notes(itemNotes.getOrDefault(item.getSkuCode(), null)).build();
		CartResponse cartResponse = new CartResponse();
		addToCartV2(cart, request, deliveryDate, UUID.fromString(bean.getUserId()), society.getStoreId(), BACKOFFICE_APP_ID, society, defaultAppVersion, null,
				false, cartResponse);
		if (cartResponse.getError() != null) {
			response.add(new BulkA2cResponse(bean.getUserId(), cartResponse.getError().getMessage()));
		}
	}

	public String removeTagFromString(String str) {
		return str.replaceAll("<[^>]*>", "");
	}

	@Override
	public Class<OrderOfferEntity> getEntity() {
		return OrderOfferEntity.class;
	}

	@Override
	public BaseCrudRepository getRepository() {
		return orderOfferRepository;
	}

}