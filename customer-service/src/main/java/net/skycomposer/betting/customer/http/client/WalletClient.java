package net.skycomposer.betting.customer.http.client;

import jakarta.validation.Valid;
import net.skycomposer.betting.common.domain.dto.customer.Customer;
import net.skycomposer.betting.common.domain.dto.customer.CustomerRequest;
import net.skycomposer.betting.common.domain.dto.customer.WalletData;
import net.skycomposer.betting.common.domain.dto.customer.WalletResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.UUID;

@FeignClient(name = "wallet")
public interface WalletClient {

    @GetMapping("/{walletId}")
    WalletData findById(@PathVariable String walletId);

    @PostMapping("/add/{walletId}/{funds}")
    WalletResponse add(@PathVariable String walletId, @PathVariable int funds);

    @PostMapping("/remove/{walletId}/{funds}")
    WalletResponse remove(@PathVariable String walletId, @PathVariable int funds);
}
