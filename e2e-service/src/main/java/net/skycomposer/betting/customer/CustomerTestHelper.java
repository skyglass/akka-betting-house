package net.skycomposer.betting.customer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.skycomposer.betting.common.domain.dto.customer.Customer;
import net.skycomposer.betting.common.domain.dto.customer.CustomerRequest;
import net.skycomposer.betting.common.domain.dto.customer.WalletResponse;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@RequiredArgsConstructor
@Slf4j
public class CustomerTestHelper {

    private final CustomerClient customerClient;

    public WalletResponse createWallet(String walletId, int funds) {
        return customerClient.addWallet(walletId, funds);
    }



}

