package net.skycomposer.betting.market;

import java.time.Instant;

import net.skycomposer.betting.common.domain.dto.market.*;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class MarketTestHelper {

    private final MarketClient marketClient;

    public MarketResponse createMarket(String marketId) {
        FixtureData fixtureData = FixtureData.builder()
                .id("id1")
                .homeTeam("RM")
                .awayTeam("MU")
                .build();
        OddsData oddsData = OddsData.builder()
                .winHome(1.5)
                .winAway(3.5)
                .tie(2.8)
                .build();
        MarketData marketData = MarketData.builder()
                .marketId(marketId)
                .fixture(fixtureData)
                .odds(oddsData)
                .opensAt(Instant.now().toEpochMilli())
                .build();
        return marketClient.open(marketData);
    }

    public MarketResponse updateMarket(String marketId, double marketTieOdds) {
        OddsData oddsData = OddsData.builder()
                .winHome(1.5)
                .winAway(3.5)
                .tie(marketTieOdds)
                .build();
        MarketData marketData = MarketData.builder()
                .marketId(marketId)
                .odds(oddsData)
                .build();
        return marketClient.update(marketData);
    }

    public MarketResponse closeMarket(String marketId, MarketData.Result result) {
        CloseMarketRequest closeMarketRequest = CloseMarketRequest
                .builder()
                .marketId(marketId)
                .result(result.getValue())
                .build();
        return marketClient.close(closeMarketRequest);
    }

    public MarketData getMarketData(String marketId) {
        return marketClient.getState(marketId);
    }
}

