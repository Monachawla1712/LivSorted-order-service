package com.sorted.rest.services.order.beans;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Data
public class UserServiceResponse {

	@JsonProperty("created_at")
	private Date createdAt;

	@JsonProperty("updated_at")
	private Date updatedAt;

	@JsonProperty("updated_by")
	private String updatedBy;

	@JsonProperty("id")
	private String id;

	@JsonProperty("greeting")
	private String greeting;

	@JsonProperty("name")
	private String name;

	@JsonProperty("country_code")
	private String countryCode;

	@JsonProperty("phone_number")
	private String phoneNumber;

	@JsonProperty("email")
	private String email;

	@JsonProperty("avatar_url")
	private String avatar_url;

	@JsonProperty("phone_confirmed_at")
	private Date phoneNumberConfirmedAt;

	@JsonProperty("email_confirmed_at")
	private Date emailNumberConfirmedAt;

	@JsonProperty("last_sign_in_at")
	private Date lastSignInAt;

	@JsonProperty("is_active")
	private Boolean isActive;

	@JsonProperty("is_verified")
	private Boolean isVerified;

	@JsonProperty("is_deleted")
	private Boolean isDeleted;

	@JsonProperty("banned_until")
	private Date bannedUntil;

	@JsonProperty("userPreferences")
	private UserPreferences userPreferences;

	@JsonProperty("eligible_offers")
	private EligibleOffersResponse eligibleOffers;

	@JsonProperty("easebuzzQrCode")
	private String easebuzzQrCode;

	@JsonProperty("meta_data")
	private UserMetadata userMetadata;

	@Data
	public static class UserPreferences {

		private String orderCount;

		private Integer slot;

		private Integer vipOrderNum;

		private Boolean isPrepaidUser;

		private String paymentPreference;

		private String paymentMethod;
	}

	@Data
	public static class UserMetadata implements Serializable {

		private static final long serialVersionUID = 961728346845264469L;

		private Boolean isFirstOrderFlow;
	}
}
