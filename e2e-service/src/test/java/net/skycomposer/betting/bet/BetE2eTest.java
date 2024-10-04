package net.skycomposer.betting.bet;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

import java.time.Duration;
import java.util.UUID;

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
        String walletId = UUID.randomUUID().toString();
        int walletBalance = 100;
        String marketId = UUID.randomUUID().toString();
        int betStake = 50;
        double betOdds = 2.8;
        MarketData.Result betResult = MarketData.Result.TIE;

        WalletResponse walletResponse = customerTestHelper.createWallet(walletId, walletBalance);
        assertThat(walletResponse.getMessage(), equalTo("The request has been accepted for processing, but the processing has not been completed."));
        MarketResponse marketResponse = marketTestHelper.createMarket(marketId);
        assertThat(marketResponse.getMarketId(), equalTo(marketId));
        assertThat(marketResponse.getMessage(), equalTo("initialized"));

        BetResponse betResponse = betTestHelper.createBet(betId, marketId, walletId, betStake, betOdds, betResult);
        assertThat(betResponse.getBetId(), equalTo(betId));
        assertThat(betResponse.getMessage(), equalTo("initialized"));

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
                    while (walletData.getAmount() != 50) {
                        walletData = customerTestHelper.findWalletById(walletId);
                    }
                    assertThat(walletData.getAmount(), equalTo(50));
                }, () -> "Wallet amount is not reduced to 50; current amount = " + customerTestHelper.findWalletById(walletId).getAmount()
        );

        customerTestHelper.removeFunds(walletId, 50);
        customerTestHelper.addFunds(walletId, 30);

        assertTimeoutPreemptively(
                Duration.ofSeconds(10)
                , () -> {
                    WalletData walletData = customerTestHelper.findWalletById(walletId);
                    while (walletData.getAmount() != 30) {
                        walletData = customerTestHelper.findWalletById(walletId);
                    }
                    assertThat(walletData.getAmount(), equalTo(30));
                }, () -> "Wallet amount is not reduced to thirty; current amount = " + customerTestHelper.findWalletById(walletId).getAmount()
        );
    }


}
