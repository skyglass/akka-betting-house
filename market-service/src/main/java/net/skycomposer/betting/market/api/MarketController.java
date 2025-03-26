package net.skycomposer.betting.market.api;

import net.skycomposer.betting.common.domain.dto.market.CancelMarketRequest;
import net.skycomposer.betting.common.domain.dto.market.CloseMarketRequest;
import net.skycomposer.betting.common.domain.dto.market.MarketData;
import net.skycomposer.betting.common.domain.dto.market.MarketResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.skycomposer.betting.market.grpc.client.MarketGrpcClient;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
public class MarketController {

  private final MarketGrpcClient marketGrpcClient;

  @GetMapping("/get-state/{marketId}")
  public MarketData getState(@PathVariable String marketId) {
    return marketGrpcClient.getState(marketId);
  }

  @GetMapping("/all")
  public List<MarketData> getAllMarkets() {
    log.info("Fetching all markets");
    return marketGrpcClient.getAllMarkets();
  }

  @PostMapping("/open")
  @ResponseStatus(HttpStatus.CREATED)
  public MarketResponse open(@RequestBody @Valid MarketData marketData) {
    log.info("Open new market {}", marketData);
    return marketGrpcClient.open(marketData);
  }

  @PostMapping("/update")
  public MarketResponse update(@RequestBody @Valid MarketData marketData) {
    log.info("Update market {}", marketData);
    return marketGrpcClient.update(marketData);
  }

  @PostMapping("/close")
  public MarketResponse close(@RequestBody @Valid CloseMarketRequest request) {
    log.info("Close market {}", request);
    return marketGrpcClient.close(request);
  }

  @PostMapping("/cancel")
  public MarketResponse cancel(@RequestBody @Valid CancelMarketRequest request) {
    log.info("Cancel market {}", request);
    return marketGrpcClient.cancel(request);
  }
}
