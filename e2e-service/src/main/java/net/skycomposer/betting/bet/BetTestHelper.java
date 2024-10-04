package net.skycomposer.betting.bet;

import java.util.concurrent.CompletableFuture;

import lombok.NoArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.skycomposer.betting.common.domain.dto.betting.BetData;
import net.skycomposer.betting.common.domain.dto.betting.BetResponse;
import net.skycomposer.betting.common.domain.dto.betting.SumStakesData;
import net.skycomposer.betting.common.domain.dto.market.MarketData;

@Component
@RequiredArgsConstructor
@Slf4j
public class BetTestHelper {

    private final BetClient betClient;

    @Async
    public CompletableFuture<BetResponse> asyncPlaceBet(String betId, String marketId, String walletId, int stake,
                                                 double odds, MarketData.Result result) {
        return CompletableFuture.completedFuture(createBet(betId, marketId, walletId, stake, odds, result));
    }

    public BetResponse createBet(String betId, String marketId, String walletId, int stake, double odds, MarketData.Result result) {
        BetData betData = BetData.builder()
                .betId(betId)
                .marketId(marketId)
                .walletId(walletId)
                .result(result.getValue())
                .stake(stake)
                .odds(odds)
                .build();
        return betClient.open(betData);
    }

    public BetData getState(String betId) {
        return betClient.getState(betId);
    }

    public SumStakesData getBetsByMarket(String marketId) {
        return betClient.getBetsByMarket(marketId);
    }

}

