package net.skycomposer.betting.common.domain.dto.market;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CancelMarketRequest {
    private String marketId;
    private String reason;
}
