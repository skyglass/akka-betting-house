package net.skycomposer.betting.customer;

import java.util.concurrent.CompletableFuture;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.skycomposer.betting.common.domain.dto.customer.WalletData;
import net.skycomposer.betting.common.domain.dto.customer.WalletResponse;

@Component
@RequiredArgsConstructor
@Slf4j
public class CustomerTestHelper {

    private final CustomerClient customerClient;

    @Async
    public CompletableFuture<WalletResponse> asyncAddFunds(String walletId, int funds) {
        return CompletableFuture.completedFuture(addFunds(walletId, funds));
    }

    public WalletResponse createWallet(String walletId, int funds) {
        return addFunds(walletId, funds);
    }

    public WalletResponse addFunds(String walletId, int funds) {
        return customerClient.addFunds(walletId, funds);
    }

    public WalletResponse removeFunds(String walletId, int funds) {
        return customerClient.removeFunds(walletId, funds);
    }

    public WalletData findWalletById(String walletId) {
        return customerClient.findWalletById(walletId);
    }



}

