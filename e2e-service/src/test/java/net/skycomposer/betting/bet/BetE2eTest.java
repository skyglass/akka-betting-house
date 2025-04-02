package net.skycomposer.betting.bet;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

import java.time.Duration;
import java.util.UUID;

import feign.FeignException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import lombok.SneakyThrows;
import net.skycomposer.betting.common.E2eTest;
import net.skycomposer.betting.common.domain.dto.betting.BetData;
import net.skycomposer.betting.common.domain.dto.betting.BetResponse;
import net.skycomposer.betting.common.domain.dto.customer.WalletData;
import net.skycomposer.betting.common.domain.dto.customer.WalletResponse;
import net.skycomposer.betting.common.domain.dto.market.MarketData;
import net.skycomposer.betting.common.domain.dto.market.MarketResponse;
import net.skycomposer.betting.customer.CustomerTestHelper;
import net.skycomposer.betting.helper.RetryHelper;
import net.skycomposer.betting.market.MarketTestHelper;

@SpringBootTest
public class BetE2eTest extends E2eTest {

    @Autowired
    private CustomerTestHelper customerTestHelper;

    @Autowired
    private MarketTestHelper marketTestHelper;

    @Autowired
    private BetTestHelper betTestHelper;

    @Test
    @SneakyThrows
    void test() {
        String betId = UUID.randomUUID().toString();
        String betId2 = UUID.randomUUID().toString();
        String betId3 = UUID.randomUUID().toString();
        String walletId = UUID.randomUUID().toString();
        String walletRequestId = UUID.randomUUID().toString();
        int walletBalance = 100;
        String marketId = UUID.randomUUID().toString();
        int betStake = 100;
        int betStake2 = 101;
        int betStake3 = 101;
        double betOdds = 2.8;
        double betOdds2 = 2.9;
        double betOdds3 = 2.8;
        MarketData.Result betResult = MarketData.Result.TIE;

        WalletResponse walletResponse = customerTestHelper.createWallet(walletId, walletRequestId, walletBalance);
        //Duplicate request with the same request id to make sure that duplicates are handled correctly
        try {
            walletResponse = customerTestHelper.createWallet(walletId, walletRequestId, walletBalance);
        } catch (FeignException.InternalServerError e) {
            //expected
        }
        assertThat(walletResponse.getMessage(), equalTo("The request has been accepted for processing, but the processing has not been completed."));
        MarketResponse marketResponse = marketTestHelper.createMarket(marketId);
        assertThat(marketResponse.getMarketId(), equalTo(marketId));
        assertThat(marketResponse.getMessage(), equalTo("initialized"));

        BetResponse betResponse = betTestHelper.createBet(betId, marketId, walletId, betStake, betOdds, betResult);
        assertThat(betResponse.getBetId(), equalTo(betId));
        assertThat(betResponse.getMessage(), equalTo("initialized"));

        //Duplicate requests to make sure that opening the bet with the same id should be handled idempotently (only one bet open event should be handled, other duplicate events should be ignored)
        betResponse = betTestHelper.createBet(betId, marketId, walletId, betStake, betOdds, betResult);
        betResponse = betTestHelper.createBet(betId, marketId, walletId, betStake, betOdds, betResult);
        betResponse = betTestHelper.createBet(betId, marketId, walletId, betStake, betOdds, betResult);

        BetData betData =  RetryHelper.retry(() ->  betTestHelper.getState(betId));

        assertThat(betData.getWalletId(), equalTo(walletId));
        assertThat(betData.getMarketId(), equalTo(marketId));
        assertThat(betData.getBetId(), equalTo(betId));
        assertThat(betData.getResult(), equalTo(betResult.getValue()));
        assertThat(betData.getStake(), equalTo(betStake));
        assertThat(betData.getOdds(), equalTo(betOdds));

        assertTimeoutPreemptively(
                Duration.ofSeconds(10)
                , () -> {
                    WalletData walletData = customerTestHelper.findWalletById(walletId);
                    while (walletData.getBalance() != 0) {
                        Thread.sleep(100);
                        walletData = customerTestHelper.findWalletById(walletId);
                    }
                    assertThat(walletData.getBalance(), equalTo(0));
                }, () -> "Wallet amount is not equal to 0; current amount = " + customerTestHelper.findWalletById(walletId).getBalance()
        );

        BetResponse betResponse2 = betTestHelper.createBet(betId2, marketId, walletId, betStake2, betOdds2, betResult);
        assertThat(betResponse2.getBetId(), equalTo(betId2));
        assertThat(betResponse2.getMessage(), equalTo("initialized"));

        //Duplicate requests to make sure that opening the bet with the same id should be handled idempotently (only one bet open event should be handled, other duplicate events should be ignored)
        betResponse2 = betTestHelper.createBet(betId2, marketId, walletId, betStake2, betOdds2, betResult);
        //betResponse2 = betTestHelper.createBet(betId2, marketId, walletId, betStake2, betOdds2, betResult);
        //betResponse2 = betTestHelper.createBet(betId2, marketId, walletId, betStake2, betOdds2, betResult);

        BetData betData2 =  RetryHelper.retry(() ->  betTestHelper.getState(betId2));

        assertThat(betData2.getWalletId(), equalTo(walletId));
        assertThat(betData2.getMarketId(), equalTo(marketId));
        assertThat(betData2.getBetId(), equalTo(betId2));
        assertThat(betData2.getResult(), equalTo(betResult.getValue()));
        assertThat(betData2.getStake(), equalTo(betStake2));
        assertThat(betData2.getOdds(), equalTo(betOdds2));


        BetResponse betResponse3 = betTestHelper.createBet(betId3, marketId, walletId, betStake3, betOdds3, betResult);
        assertThat(betResponse3.getBetId(), equalTo(betId3));
        assertThat(betResponse3.getMessage(), equalTo("initialized"));

        //Duplicate requests to make sure that opening the bet with the same id should be handled idempotently (only one bet open event should be handled, other duplicate events should be ignored)
        betResponse3 = betTestHelper.createBet(betId3, marketId, walletId, betStake3, betOdds3, betResult);
        //betResponse3 = betTestHelper.createBet(betId3, marketId, walletId, betStake3, betOdds3, betResult);
        //betResponse3 = betTestHelper.createBet(betId3, marketId, walletId, betStake3, betOdds3, betResult);

        BetData betData3 =  RetryHelper.retry(() ->  betTestHelper.getState(betId3));

        assertThat(betData3.getWalletId(), equalTo(walletId));
        assertThat(betData3.getMarketId(), equalTo(marketId));
        assertThat(betData3.getBetId(), equalTo(betId3));
        assertThat(betData3.getResult(), equalTo(betResult.getValue()));
        assertThat(betData3.getStake(), equalTo(betStake3));
        assertThat(betData3.getOdds(), equalTo(betOdds3));

        marketResponse = marketTestHelper.closeMarket(marketId, betResult);
        assertThat(marketResponse.getMarketId(), equalTo(marketId));
        assertThat(marketResponse.getMessage(), equalTo("initialized"));

        assertTimeoutPreemptively(
                Duration.ofSeconds(10)
                , () -> {
                    WalletData walletData = customerTestHelper.findWalletById(walletId);
                    while (walletData.getBalance() != 100) {
                        Thread.sleep(100);
                        walletData = customerTestHelper.findWalletById(walletId);
                    }
                    assertThat(walletData.getBalance(), equalTo(100));
                }, () -> "Wallet amount is not equal to 100; current amount = " + customerTestHelper.findWalletById(walletId).getBalance()
        );

    }


}
