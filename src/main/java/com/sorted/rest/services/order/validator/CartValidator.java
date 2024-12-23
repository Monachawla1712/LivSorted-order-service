package com.sorted.rest.services.order.validator;

import java.math.BigDecimal;

import org.springframework.stereotype.Component;

import com.sorted.rest.services.order.beans.CartResponse;
import com.sorted.rest.services.order.beans.FranchiseCartResponse;
import com.sorted.rest.services.order.beans.FranchiseStoreInventoryResponse;
import com.sorted.rest.services.order.beans.StoreInventoryResponse.StoreProductInventory;

/**
 * <p>
 * CustomerCartValidator class.
 * </p>
 *
 * @author mohit
 * @version $Id: $Id
 */
@Component
public class CartValidator {

	public boolean isValidStoreInventory(StoreProductInventory storeProductInventory, Double cartQuantity, CartResponse response) {
		if (storeProductInventory == null) {
			response.error("invalid_store_product", "Product is not valid. ");
			return false;
		} else if (storeProductInventory.getInventoryQuantity() != null) {
			BigDecimal storeQty = BigDecimal.valueOf(storeProductInventory.getInventoryQuantity());
			BigDecimal cartQty = BigDecimal.valueOf(cartQuantity);
			BigDecimal zero = new BigDecimal(0.0);
			if (storeQty.compareTo(zero) <= 0) {
				response.error("product_not_available", "Product is not available on the store.");
				return false;
			} else if (storeQty.compareTo(cartQty) < 0) {
				response.error("requested_qty_not_available", String.format("Requested Quantity not available. Only %s %s available.", storeQty,
						storeProductInventory.getProductUnitOfMeasurement()));
			}
		}
		return true;
	}
}
