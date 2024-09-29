package net.skycomposer.betting.customer.api;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import net.skycomposer.betting.common.domain.dto.customer.WalletData;
import net.skycomposer.betting.common.domain.dto.customer.WalletResponse;
import net.skycomposer.betting.customer.service.WalletService;

@RestController
@RequiredArgsConstructor
public class CustomerController {

  private final WalletService walletService;

  @GetMapping("/get-wallet/{walletId}")
  public WalletData findWalletById(@PathVariable String walletId) {
    return walletService.findWalletById(walletId);
  }

  @PostMapping("/add-funds/{walletId}/{funds}")
  public WalletResponse addFunds(@PathVariable String walletId, @PathVariable int funds) {
    return walletService.addFunds(walletId, funds);
  }

  @PostMapping("/remove-funds/{walletId}/{funds}")
  public WalletResponse removeWallet(@PathVariable String walletId, @PathVariable int funds) {
    return walletService.removeFunds(walletId, funds);
  }

}
