package net.skycomposer.betting.customer;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import feign.FeignException;
import lombok.SneakyThrows;
import net.skycomposer.betting.common.domain.dto.betting.BetResponse;
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
    public CompletableFuture<WalletResponse> asyncAddFunds(String walletId, String requestId, int funds) {
        return CompletableFuture.completedFuture(addFunds(walletId, requestId, funds));
    }

    public WalletResponse createWallet(String walletId, String requestId, int funds) {
        return addFunds(walletId, requestId, funds);
    }

    @SneakyThrows
    public WalletResponse addFunds(String walletId, String requestId, int funds) {
        WalletResponse response = null;
        while (response == null) {
            try {
                response = customerClient.addFunds(walletId, requestId, funds);
            } catch (FeignException.TooManyRequests e) {
                TimeUnit.MILLISECONDS.sleep(Duration.ofSeconds(1).toMillis());
            }
        }
        return response;
    }

    public WalletResponse removeFunds(String walletId, String requestId, int funds) {
        return customerClient.removeFunds(walletId, requestId, funds);
    }

    public WalletData findWalletById(String walletId) {
        return customerClient.findWalletById(walletId);
    }



}

