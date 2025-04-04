package net.skycomposer.betting.bet.api;

import net.skycomposer.betting.bet.grpc.client.BettingGrpcClient;
import net.skycomposer.betting.common.domain.dto.betting.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequiredArgsConstructor
public class BetController {

  private final BettingGrpcClient bettingGrpcClient;

  @GetMapping("/get-state/{betId}")
  public BetData getState(@PathVariable String betId) {
    return bettingGrpcClient.getState(betId);
  }

  @GetMapping("/get-bets-by-market/{marketId}")
  public SumStakesData getBetsByMarket(@PathVariable String marketId) {
    return bettingGrpcClient.getBetByMarket(marketId);
  }

  @GetMapping("/get-bets-for-market/{marketId}")
  public BetDataList getBetsForMarket(@PathVariable String marketId) {
    return bettingGrpcClient.getBetsForMarket(marketId);
  }

  @GetMapping("/get-bets-for-player/{walletId}")
  public BetDataList getBetsForPlayer(@PathVariable String walletId) {
    return bettingGrpcClient.getBetsForPlayer(walletId);
  }

  @PostMapping("/open")
  @ResponseStatus(HttpStatus.CREATED)
  public BetResponse open(@RequestBody @Valid BetData betData) {
    log.info("Open new bet {}", betData);
    return bettingGrpcClient.open(betData);
  }

  @PostMapping("/cancel")
  public BetResponse close(@RequestBody @Valid CancelBetRequest request) {
    log.info("Cancel bet {}", request);
    return bettingGrpcClient.cancel(request);
  }
}
