package net.skycomposer.betting.customer.service;

import net.skycomposer.betting.common.domain.dto.customer.WalletList;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import net.skycomposer.betting.common.domain.dto.customer.WalletData;
import net.skycomposer.betting.common.domain.dto.customer.WalletResponse;
import net.skycomposer.betting.customer.http.client.WalletClient;

import java.util.List;

@Service
@RequiredArgsConstructor
public class WalletService {

    private final WalletClient walletClient;

    public List<WalletData> findAll() {
        return walletClient.findAll().getWallets();
    }

    public WalletData findWalletById(String walletId) {
        return walletClient.findById(walletId);
    }

    public WalletResponse addFunds(String walletId, String requestId, Integer funds) {
        WalletData walletData = findWalletById(walletId);
        return new WalletResponse(walletClient.add(walletId, requestId, funds), walletId, walletData.getBalance() + funds);
    }

    public WalletResponse removeFunds(String walletId, String requestId, Integer funds) {
        WalletData walletData = findWalletById(walletId);
        return new WalletResponse(walletClient.remove(walletId, requestId, funds), walletId, walletData.getBalance() - funds);
    }

}
