package net.skycomposer.betting.common.domain.dto.market;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MarketData {
    private String marketId;
    private FixtureData fixture;
    private OddsData odds;
    private Result result;
    private long opensAt;

    public enum Result {
        HOME_WINS,
        HOME_LOSES,
        TIE
    }
}