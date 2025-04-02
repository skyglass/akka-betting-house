package net.skycomposer.betting.customer.api;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import net.skycomposer.betting.common.domain.dto.customer.WalletData;
import net.skycomposer.betting.common.domain.dto.customer.WalletResponse;
import net.skycomposer.betting.customer.service.WalletService;

import java.util.List;

//remove
@RestController
@RequiredArgsConstructor
public class CustomerController {

  private static final Integer DEFAULT_REGISTERED_CUSTOMER_AMOUNT = 100;

  private final WalletService walletService;

  @GetMapping("/get-wallet/{walletId}")
  public WalletData findWalletById(@PathVariable String walletId) {
    return walletService.findWalletById(walletId);
  }

  @GetMapping("/all")
  public List<WalletData> findAll() {
    return walletService.findAll();
  }

  @PostMapping("/register/{walletId}/{requestId}")
  public WalletResponse addFunds(@PathVariable String walletId, @PathVariable String requestId) {
    return walletService.addFunds(walletId, requestId, DEFAULT_REGISTERED_CUSTOMER_AMOUNT);
  }

  @PostMapping("/add-funds/{walletId}/{requestId}/{funds}")
  public WalletResponse addFunds(@PathVariable String walletId, @PathVariable String requestId, @PathVariable int funds) {
    return walletService.addFunds(walletId, requestId, funds);
  }

  @PostMapping("/remove-funds/{walletId}/{requestId}/{funds}")
  public WalletResponse removeWallet(@PathVariable String walletId, @PathVariable String requestId, @PathVariable int funds) {
    return walletService.removeFunds(walletId, requestId, funds);
  }

}
