package net.skycomposer.betting.market;

import java.time.Instant;

import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.skycomposer.betting.common.domain.dto.market.FixtureData;
import net.skycomposer.betting.common.domain.dto.market.MarketData;
import net.skycomposer.betting.common.domain.dto.market.MarketResponse;
import net.skycomposer.betting.common.domain.dto.market.OddsData;

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

}

