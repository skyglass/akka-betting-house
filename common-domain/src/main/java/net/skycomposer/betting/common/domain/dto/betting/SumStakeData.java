package net.skycomposer.betting.common.domain.dto.betting;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SumStakeData {
    private double total;
    private int result;
}
