package net.skycomposer.betting.common.domain.dto.betting;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.skycomposer.betting.common.domain.dto.market.FixtureData;
import net.skycomposer.betting.common.domain.dto.market.OddsData;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BetData {
    private String betId;
    private String marketId;
    private String marketName;
    private String walletId;
    private double odds;
    private int stake;
    private int result;
    private boolean marketConfirmed;
    private boolean fundsConfirmed;

}
