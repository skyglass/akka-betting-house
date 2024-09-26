package net.skycomposer.betting.market;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.skycomposer.betting.common.domain.dto.customer.WalletResponse;
import net.skycomposer.betting.common.domain.dto.market.MarketData;
import net.skycomposer.betting.common.domain.dto.market.MarketResponse;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class MarketTestHelper {

    private final MarketClient marketClient;

    public MarketResponse createMarket(MarketData marketData) {
        return marketClient.open(marketData);
    }

}

