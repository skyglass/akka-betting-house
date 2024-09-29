package net.skycomposer.betting.customer.service;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import net.skycomposer.betting.common.domain.dto.customer.WalletData;
import net.skycomposer.betting.common.domain.dto.customer.WalletResponse;
import net.skycomposer.betting.customer.http.client.WalletClient;

@Service
@RequiredArgsConstructor
public class WalletService {

    private final WalletClient walletClient;

    public WalletData findWalletById(String walletId) {
        return walletClient.findById(walletId);
    }

    public WalletResponse addFunds(String walletId, int funds) {
        WalletData walletData = findWalletById(walletId);
        return new WalletResponse(walletClient.add(walletId, funds), walletId, walletData.getAmount() + funds);
    }

    public WalletResponse removeFunds(String walletId, int funds) {
        WalletData walletData = findWalletById(walletId);
        return new WalletResponse(walletClient.remove(walletId, funds), walletId, walletData.getAmount() - funds);
    }

}
