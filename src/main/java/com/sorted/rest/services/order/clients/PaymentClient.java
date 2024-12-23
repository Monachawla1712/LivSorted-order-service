package com.sorted.rest.services.order.clients;

import com.sorted.rest.common.openfeign.FeignCustomConfiguration;
import com.sorted.rest.services.order.beans.*;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@FeignClient(value = "payment", url = "${client.payment.url}", configuration = { FeignCustomConfiguration.class })
public interface PaymentClient {

	@PostMapping(value = "/payments/confirm/lithos")
	PaymentNotifyBean sendLithosPaymentUpdate(@RequestBody PaymentNotifyBean payment);

	@PostMapping(value = "/payments/internal/confirm")
	PaymentNotifyBean sendPaymentUpdate(@RequestBody PaymentNotifyBean payment);

	@GetMapping("/payments/wallet/USER/{customerId}")
	WalletBean getUserWallet(@PathVariable String customerId);

	@PostMapping("/payments/wallet/addOrDeduct/USER/{customerId}")
	WalletAddOrDeductBean addOrDeductFromWallet(@PathVariable String customerId, @RequestBody WalletAddOrDeductBean request, @RequestParam String key);

	@GetMapping("/payments/wallet/STORE/{storeId}")
	WalletBean getStoreWallet(@PathVariable String storeId);

	@PostMapping("/payments/wallet/addOrDeduct/STORE/{storeId}")
	WalletAddOrDeductBean addOrDeductFromStoreWallet(@PathVariable String storeId, @RequestBody WalletAddOrDeductBean request, @RequestParam String key);

	@PostMapping("/payments/cash-collections/internal")
	Long makeCashCollectionEntry(@RequestBody CashCollectionRequest request);

	@GetMapping("/payments/walletStatement/{txnDetail}")
	List<WalletStatementBean> fetchWalletStatementByTxnDetail(@PathVariable String txnDetail);

	@PostMapping("/payments/wallet/internal")
	List<WalletBean> getStoreWalletsFromIds(@RequestBody BulkStoreWalletRequest bulkStoreWalletRequest);

	@GetMapping("/payments/wallet/consumer/internal")
	List<WalletBean> getUserWalletsFromIds(@RequestBody BulkUserWalletRequest bulkUserWalletRequest);

	@GetMapping("/payments/internal/cash-collections/{userId}")
	CashCollectionResponse getCashCollectionByUserId(@PathVariable UUID userId);
}