package net.skycomposer.betting.common.domain.dto.market;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class OddsData {
    private double winHome;
    private double winAway;
    private double tie;
}
