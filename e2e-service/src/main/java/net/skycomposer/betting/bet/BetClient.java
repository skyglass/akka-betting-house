package net.skycomposer.betting.bet;

import jakarta.validation.Valid;
import net.skycomposer.betting.common.domain.dto.betting.*;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@FeignClient(name = "betting")
public interface BetClient {

    @GetMapping("/get-state/{betId}")
    BetData getState(@PathVariable String betId);

    @GetMapping("/get-bets-by-market/{marketId}")
    SumStakesData getBetsByMarket(@PathVariable String marketId);

    @PostMapping("/open")
    @ResponseStatus(HttpStatus.CREATED)
    BetResponse open(@RequestBody @Valid BetData betData);

    @PostMapping("/settle")
    BetResponse settle(@RequestBody @Valid SettleBetRequest request);

    @PostMapping("/cancel")
    BetResponse close(@RequestBody @Valid CancelBetRequest request);
}