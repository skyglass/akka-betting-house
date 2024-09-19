package net.skycomposer.betting.customer.api;

import net.skycomposer.betting.common.domain.dto.customer.WalletData;
import net.skycomposer.betting.common.domain.dto.customer.WalletResponse;
import lombok.RequiredArgsConstructor;
import net.skycomposer.betting.customer.http.client.WalletClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class CustomerController {

  private final WalletClient walletClient;

  @GetMapping("/get-wallet/{walletId}")
  public WalletData findWalletById(@PathVariable String walletId) {
    return walletClient.findById(walletId);
  }

  @PostMapping("/add-funds/{walletId}/{funds}")
  public WalletResponse addWallet(@PathVariable String walletId, @PathVariable int funds) {
    return walletClient.add(walletId, funds);
  }

  @PostMapping("/remove-funds/{walletId}/{funds}")
  public WalletResponse removeWallet(@PathVariable String walletId, @PathVariable int funds) {
    return walletClient.remove(walletId, funds);
  }

}
