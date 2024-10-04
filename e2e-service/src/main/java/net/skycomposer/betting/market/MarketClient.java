package net.skycomposer.betting.market;

import jakarta.validation.Valid;
import net.skycomposer.betting.common.domain.dto.customer.WalletData;
import net.skycomposer.betting.common.domain.dto.customer.WalletResponse;
import net.skycomposer.betting.common.domain.dto.market.CancelMarketRequest;
import net.skycomposer.betting.common.domain.dto.market.CloseMarketRequest;
import net.skycomposer.betting.common.domain.dto.market.MarketData;
import net.skycomposer.betting.common.domain.dto.market.MarketResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@FeignClient(name = "market")
public interface MarketClient {

    @GetMapping("/get-state/{marketId}")
    MarketData getState(@PathVariable("marketId") String marketId);

    @PostMapping("/open")
    @ResponseStatus(HttpStatus.CREATED)
    MarketResponse open(@RequestBody @Valid MarketData marketData);

    @PostMapping("/update")
    MarketResponse update(@RequestBody @Valid MarketData marketData);

    @PostMapping("/close")
    MarketResponse close(@RequestBody @Valid CloseMarketRequest request);

    @PostMapping("/cancel")
    MarketResponse cancel(@RequestBody @Valid CancelMarketRequest request);
}
