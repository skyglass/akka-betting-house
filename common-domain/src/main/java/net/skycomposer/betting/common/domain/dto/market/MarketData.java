package net.skycomposer.betting.common.domain.dto.market;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
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