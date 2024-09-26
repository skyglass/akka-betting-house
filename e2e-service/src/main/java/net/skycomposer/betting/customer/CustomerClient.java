package net.skycomposer.betting.customer;

import jakarta.validation.Valid;
import net.skycomposer.betting.common.domain.dto.customer.Customer;
import net.skycomposer.betting.common.domain.dto.customer.CustomerRequest;
import net.skycomposer.betting.common.domain.dto.customer.WalletData;
import net.skycomposer.betting.common.domain.dto.customer.WalletResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@FeignClient(name = "customer")
public interface CustomerClient {

    @GetMapping("/get-wallet/{walletId}")
    WalletData findWalletById(@PathVariable String walletId);

    @PostMapping("/add-funds/{walletId}/{funds}")
    WalletResponse addWallet(@PathVariable String walletId, @PathVariable int funds);

    @PostMapping("/remove-funds/{walletId}/{funds}")
    WalletResponse removeWallet(@PathVariable String walletId, @PathVariable int funds);
}
