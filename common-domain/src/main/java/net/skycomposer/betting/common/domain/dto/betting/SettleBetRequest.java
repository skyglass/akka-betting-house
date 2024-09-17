package net.skycomposer.betting.common.domain.dto.betting;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SettleBetRequest {
    private String betId;
    private int result;
}