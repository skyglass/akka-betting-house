package net.skycomposer.betting.bet;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.awaitility.Awaitility.await;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import net.skycomposer.betting.common.E2eTest;
import net.skycomposer.betting.common.domain.dto.betting.BetResponse;
import net.skycomposer.betting.common.domain.dto.customer.WalletResponse;
import net.skycomposer.betting.common.domain.dto.market.MarketData;
import net.skycomposer.betting.common.domain.dto.market.MarketResponse;
import net.skycomposer.betting.customer.CustomerTestHelper;
import net.skycomposer.betting.market.MarketTestHelper;

@SpringBootTest
@Slf4j
public class BetConcurrencyE2eTest extends E2eTest {

    @Autowired
    private CustomerTestHelper customerTestHelper;

    @Autowired
    private MarketTestHelper marketTestHelper;

    @Autowired
    private BetTestHelper betTestHelper;


    @Test
    @SneakyThrows
    void createParallelBetsThenFundsAreZeroTest() {
        String walletId = UUID.randomUUID().toString();
        int walletBalance = 100;
        int walletBeforeMarketUpdateBalance = 0;
        int walletBeforeMarketCloseBalance = 0;
        int walletAfterMarketCloseBalance = 200;
        String marketId = UUID.randomUUID().toString();
        int betStake = 10;
        double betOdds = 2.8;
        double betOdds2 = 2.9;
        MarketData.Result betResult = MarketData.Result.TIE;
        AtomicInteger counter = new  AtomicInteger(0);

        WalletResponse walletResponse = customerTestHelper.createWallet(walletId, walletBalance);
        assertThat(walletResponse.getMessage(), equalTo("The request has been accepted for processing, but the processing has not been completed."));
        MarketResponse marketResponse = marketTestHelper.createMarket(marketId);
        assertThat(marketResponse.getMessage(), equalTo("initialized"));

        // Start the clock
        long start = Instant.now().toEpochMilli();

        int numberOfBets = 20;
        List<CompletableFuture<BetResponse>> createdBets = new ArrayList<>();
        for (int i = 0; i < numberOfBets; i++) {
            CompletableFuture<BetResponse> betResponse = betTestHelper.asyncPlaceBet(
                    UUID.randomUUID().toString(),
                    marketId, walletId, betStake,
                    counter.getAndIncrement() % 2 == 0 ? betOdds : betOdds2,
                    betResult);
            createdBets.add(betResponse);
        }

        int numberOfWalletUpdates = 10;
        List<CompletableFuture<WalletResponse>> addedFunds = new ArrayList<>();
        for (int i = 0; i < numberOfWalletUpdates; i++) {
            CompletableFuture<WalletResponse> addFundsResult = customerTestHelper
                    .asyncAddFunds(walletId, 10);
            addedFunds.add(addFundsResult);
        }

        // Wait until they are all done
        CompletableFuture.allOf(createdBets.toArray(new CompletableFuture[0])).join();
        CompletableFuture.allOf(addedFunds.toArray(new CompletableFuture[0])).join();

        for (CompletableFuture<BetResponse> betFuture: createdBets) {
            BetResponse betResponse = betFuture.get();
            assertNotNull(betResponse);
            log.info("--> " + betResponse.getBetId());
            assertTimeoutPreemptively(
                    Duration.ofSeconds(5)
                    , () -> {
                        var result = betTestHelper.getState(betResponse.getBetId());
                        while (result == null) {
                            Thread.sleep(200);
                            result = betTestHelper.getState(betResponse.getBetId());
                        }
                        assertThat(result.getStake(), equalTo(betStake));
                        Thread.sleep(200);
                    }, () -> "Can't find the bet with betId = " + betResponse.getBetId()
            );
        }
        for (CompletableFuture<WalletResponse> addFundsResultFuture: addedFunds) {
            WalletResponse addFundsResult = addFundsResultFuture.get();
            assertNotNull(addFundsResult);
            log.info("Available Funds for walletId = {} --> {}" + addFundsResult.getWalletId(), addFundsResult.getCurrentAmount());
        }

        log.info("Elapsed time: " + (Instant.now().toEpochMilli() - start));

        assertTimeoutPreemptively(
                Duration.ofSeconds(5)
                , () -> {
                    var result = customerTestHelper.findWalletById(walletId);
                    while (result.getAmount() != walletBeforeMarketUpdateBalance) {
                        Thread.sleep(1000);
                        result = customerTestHelper.findWalletById(walletId);
                    }
                    assertThat(result.getAmount(), equalTo(walletBeforeMarketUpdateBalance));
                }, () -> String.format("Available wallet funds before market update are incorrect for walletId = %s: amount = %d", walletId, customerTestHelper.findWalletById(walletId).getAmount())
        );

        marketTestHelper.updateMarket(marketId, 2.9);

        assertTimeoutPreemptively(
                Duration.ofSeconds(5)
                , () -> {
                    var result = marketTestHelper.getMarketData(marketId);
                    while (result.getOdds().getTie() != 2.9) {
                        Thread.sleep(1000);
                        result =  marketTestHelper.getMarketData(marketId);
                    }
                    assertThat(result.getOdds().getTie(), equalTo(2.9));
                }, () -> String.format("Market update odds are incorrect for marketId = %s: tieOdds = %.2f", marketId, marketTestHelper.getMarketData(marketId).getOdds().getTie())
        );

        //Before market is closed, make sure that all bets are confirmed by market and wallet
        for (CompletableFuture<BetResponse> betFuture: createdBets) {
            BetResponse betResponse = betFuture.get();
            log.info("--> " + betResponse.getBetId());
            assertTimeoutPreemptively(
                    Duration.ofSeconds(60)
                    , () -> {
                        var result = betTestHelper.getState(betResponse.getBetId());
                        while (!result.isMarketConfirmed() || !result.isFundsConfirmed()) {
                            Thread.sleep(1000);
                            result = betTestHelper.getState(betResponse.getBetId());
                        }
                        assertThat(result.isMarketConfirmed(), equalTo(true));
                        assertThat(result.isFundsConfirmed(), equalTo(true));
                    }, () -> String.format("Bet with betId = %s is not market or funds confirmed", betResponse.getBetId())
            );
        }

        assertTimeoutPreemptively(
                Duration.ofSeconds(5)
                , () -> {
                    var result = customerTestHelper.findWalletById(walletId);
                    while (result.getAmount() != walletBeforeMarketCloseBalance) {
                        Thread.sleep(1000);
                        result = customerTestHelper.findWalletById(walletId);
                    }
                    assertThat(result.getAmount(), equalTo(walletBeforeMarketCloseBalance));
                }, () -> String.format("Available wallet funds after market update are incorrect for walletId = %s: amount = %d", walletId, customerTestHelper.findWalletById(walletId).getAmount())
        );


        marketTestHelper.closeMarket(marketId, betResult);


        assertTimeoutPreemptively(
                Duration.ofSeconds(180)
                , () -> {
                    var result = customerTestHelper.findWalletById(walletId);
                    while (result.getAmount() != walletAfterMarketCloseBalance) {
                        log.info("--> " + result.getAmount());
                        Thread.sleep(2000);
                        result = customerTestHelper.findWalletById(walletId);
                    }
                    assertThat(result.getAmount(), equalTo(walletAfterMarketCloseBalance));
                }, () -> String.format("Available wallet funds after market close are incorrect for walletId = %s: amount = %d", walletId, customerTestHelper.findWalletById(walletId).getAmount())
        );
    }


}

