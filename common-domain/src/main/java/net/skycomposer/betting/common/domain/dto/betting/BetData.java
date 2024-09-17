package net.skycomposer.betting.common.domain.dto.betting;

import lombok.Builder;
import lombok.Data;
import net.skycomposer.betting.common.domain.dto.market.FixtureData;
import net.skycomposer.betting.common.domain.dto.market.OddsData;

@Data
@Builder
public class BetData {
    private String betId;
    private String marketId;
    private String walletId;
    private double odds;
    private int stake;
    private int result;

}
