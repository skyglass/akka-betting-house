package net.skycomposer.betting.common.domain.dto.market;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CloseMarketRequest {
    private String marketId;
    private int result;
}

