package net.skycomposer.betting.common.domain.dto.market;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CloseMarketRequest {
    private String marketId;
}
