package net.skycomposer.betting.bet;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

import java.time.Duration;

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
        String betId = "betId";
        String walletId = "walletId1";
        int walletBalance = 100;
        String marketId = "marketId1";
        int betStake = 50;
        double betOdds = 2.8;
        MarketData.Result betResult = MarketData.Result.TIE;

        WalletResponse walletResponse = customerTestHelper.createWallet(walletId, walletBalance);
        assertThat(walletResponse.getMessage(), equalTo("test"));
        MarketResponse marketResponse = marketTestHelper.createMarket(marketId);
        assertThat(marketResponse.getMessage(), equalTo("test"));

        BetResponse betResponse = betTestHelper.createBet(betId, marketId, walletId, betStake, betOdds, betResult);
        assertThat(betResponse.getMessage(), equalTo("test"));

        BetData betData =  RetryHelper.retry(() ->  betTestHelper.getState(betId));

        assertThat(betData.getWalletId(), equalTo(walletId));
        assertThat(betData.getMarketId(), equalTo(marketId));
        assertThat(betData.getBetId(), equalTo(marketId));
        assertThat(betData.getResult(), equalTo(betResult.getValue()));
        assertThat(betData.getStake(), equalTo(betStake));
        assertThat(betData.getOdds(), equalTo(betOdds));

        assertTimeoutPreemptively(
                Duration.ofSeconds(10)
                , () -> {
                    customerTestHelper.removeFunds(walletId, 50);
                    WalletData walletData = customerTestHelper.findWalletById(walletId);
                    while (walletData.getAmount() != 0) {
                        walletData = customerTestHelper.findWalletById(walletId);
                    }
                    assertThat(walletData.getAmount(), equalTo(0));
                }, () -> "Wallet amount is not reduced to zero; current amount = " + customerTestHelper.findWalletById(walletId).getAmount()
        );

        customerTestHelper.addFunds(walletId, 30);

        assertTimeoutPreemptively(
                Duration.ofSeconds(10)
                , () -> {
                    customerTestHelper.removeFunds(walletId, 50);
                    WalletData walletData = customerTestHelper.findWalletById(walletId);
                    while (walletData.getAmount() != 0) {
                        walletData = customerTestHelper.findWalletById(walletId);
                    }
                    assertThat(walletData.getAmount(), equalTo(0));
                }, () -> "Wallet amount is not reduced to zero; current amount = " + customerTestHelper.findWalletById(walletId).getAmount()
        );
    }


}
