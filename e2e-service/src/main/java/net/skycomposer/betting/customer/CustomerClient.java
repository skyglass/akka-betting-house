package net.skycomposer.betting.customer;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

import net.skycomposer.betting.common.domain.dto.customer.WalletData;
import net.skycomposer.betting.common.domain.dto.customer.WalletResponse;

@FeignClient(name = "customer")
public interface CustomerClient {

    @GetMapping("/get-wallet/{walletId}")
    WalletData findWalletById(@PathVariable("walletId") String walletId);

    @PostMapping("/add-funds/{walletId}/{funds}")
    WalletResponse addFunds(@PathVariable("walletId") String walletId, @PathVariable("funds") int funds);

    @PostMapping("/remove-funds/{walletId}/{funds}")
    WalletResponse removeFunds(@PathVariable("walletId") String walletId, @PathVariable("funds") int funds);
}
