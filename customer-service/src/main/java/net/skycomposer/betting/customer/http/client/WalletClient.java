package net.skycomposer.betting.customer.http.client;

import net.skycomposer.betting.common.domain.dto.customer.WalletData;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

@FeignClient(name = "wallet")
public interface WalletClient {

    @GetMapping
    WalletData findById(@RequestParam String walletId);

    @PostMapping("/add")
    String add(@RequestParam String walletId, @RequestParam int funds);

    @PostMapping("/remove")
    String remove(@RequestParam String walletId, @RequestParam int funds);
}
