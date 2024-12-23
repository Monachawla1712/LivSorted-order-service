package com.sorted.rest.services.order.beans;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
public class StoreInventoryResponse {

	@JsonProperty("inventory")
	private List<StoreProductInventory> inventory;

	@Data
	@NoArgsConstructor
	public static class StoreProductInventory {

		@JsonProperty("inventory_id")
		private String inventoryId;

		@JsonProperty("inventory_quantity")
		private Double inventoryQuantity;

		@JsonProperty("inventory_market_price")
		private Double inventoryMarketPrice;

		@JsonProperty("inventory_sale_price")
		private Double inventorySalePrice;

		@JsonProperty("inventory_max_price")
		private Double inventoryMaxPrice;

		@JsonProperty("inventory_sku_code")
		private String inventorySkuCode;

		@JsonProperty("inventory_price_brackets")
		private List<InventoryPriceBracket> inventoryPriceBrackets = null;

		@JsonProperty("inventory_store_id")
		private String inventoryStoreId;

		@JsonProperty("inventory_created_at")
		private String inventoryCreatedAt;

		@JsonProperty("inventory_updated_at")
		private String inventoryUpdatedAt;

		@JsonProperty("inventory_updated_by")
		private String inventoryUpdatedBy;

		@JsonProperty("inventory_grades")
		private List<Grade> inventoryGrades;

		@JsonProperty("inventory_cutoff_time")
		private String cutoffTime;

		@JsonProperty("inventory_is_ozone_washed_item")
		private Boolean isOzoneWashedItem;

		@JsonProperty("inventory_ozone_washing_charges")
		private Double ozoneWashingCharges;

		@JsonProperty("product_id")
		private Integer productId;

		@JsonProperty("product_sku_code")
		private String productSkuCode;

		@JsonProperty("product_category_id")
		private Integer productCategoryId;

		@JsonProperty("product_name")
		private String productName;

		@JsonProperty("product_image_url")
		private String productImageUrl;

		@JsonProperty("product_tags")
		private List<ProductTags> productTags;

		@JsonProperty("product_serves1")
		private Double productServes1;

		@JsonProperty("product_is_active")
		private Boolean productIsActive;

		@JsonProperty("product_unit_of_measurement")
		private String productUnitOfMeasurement;

		@JsonProperty("product_market_price")
		private Double productMarketPrice;

		@JsonProperty("product_sale_price")
		private Double productSalePrice;

		@JsonProperty("product_display_name")
		private String productDisplayName;

		@JsonProperty("product_min_quantity")
		private Integer productMinQuantity;

		@JsonProperty("product_max_quantity")
		private Integer productMaxQuantity;

		@JsonProperty("product_buffer_quantity")
		private Integer productBufferQuantity;

		@JsonProperty("product_is_coins_redeemable")
		private Integer productIsCoinsRedeemable;

		@JsonProperty("product_per_pcs_suffix")
		private String productPerPcsSuffix;

		@JsonProperty("product_per_pcs_weight")
		private Double perPiecesWeight;

		@JsonProperty("product_created_at")
		private String productCreatedAt;

		@JsonProperty("product_updated_at")
		private String productUpdatedAt;

		@JsonProperty("product_updated_by")
		private String productUpdatedBy;

		@JsonProperty("product_gst")
		private GstInfo productGst;

		@JsonProperty("product_hsn")
		private String productHsn;

		@JsonProperty("product_packet_description")
		private String productPacketDescription;

		@JsonProperty("category_id")
		private Integer categoryId;

		@JsonProperty("category_name")
		private String categoryName;

		@JsonProperty("category_image_url")
		private String categoryImageUrl;

		@JsonProperty("category_created_at")
		private String categoryCreatedAt;

		@JsonProperty("category_is_active")
		private Boolean categoryIsActive;

		@JsonProperty("category_updated_at")
		private String categoryUpdatedAt;

		@JsonProperty("category_updated_by")
		private String categoryUpdatedBy;

		@JsonProperty("product_consumer_contents")
		private ConsumerContents productConsumerContents;

		@JsonProperty("is_complimentary")
		private Boolean isComplimentary;

		@JsonProperty("isPreBook")
		private Boolean isPreBook = false;

		@JsonProperty("preBookDate")
		private String preBookDate;
	}

	@Data
	@NoArgsConstructor
	public static class Grade {
		@JsonProperty("name")
		private String name;
	}

	@Data
	@NoArgsConstructor
	public static class ConsumerContents {
		@JsonProperty("classes")
		private List<String> classes;
	}

	@Data
	@NoArgsConstructor
	public static class ProductTags {
		@JsonProperty("color")
		private String color;

		@JsonProperty("value")
		private Double value;

		@JsonProperty("image_url")
		private String imageUrl;

		@JsonProperty("display_name")
		private String displayName;
	}

	@Data
	@NoArgsConstructor
	public static class InventoryPriceBracket {

		@JsonProperty("max")
		private Double max;

		@JsonProperty("min")
		private Double min;

		@JsonProperty("sale_price")
		private Integer salePrice;

		@JsonProperty("discount_percentage")
		private Integer discountPercentage;
	}

	@Data
	@NoArgsConstructor
	public static class GstInfo {

		@JsonProperty("cgst")
		private Double cgst;

		@JsonProperty("sgst")
		private Double sgst;

		@JsonProperty("igst")
		private Double igst;

		@JsonProperty("cess")
		private Double cess;
	}
}
