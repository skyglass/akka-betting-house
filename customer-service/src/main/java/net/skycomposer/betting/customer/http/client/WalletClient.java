package net.skycomposer.betting.customer.http.client;

import net.skycomposer.betting.common.domain.dto.customer.WalletData;
import net.skycomposer.betting.common.domain.dto.customer.WalletList;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@FeignClient(name = "wallet")
public interface WalletClient {

    @GetMapping
    WalletData findById(@RequestParam String walletId);

    @GetMapping("/all")
    WalletList findAll();

    @PostMapping("/add")
    String add(@RequestParam String walletId, @RequestParam String requestId, @RequestParam Integer funds);

    @PostMapping("/remove")
    String remove(@RequestParam String walletId, @RequestParam String requestId, @RequestParam Integer funds);
}
