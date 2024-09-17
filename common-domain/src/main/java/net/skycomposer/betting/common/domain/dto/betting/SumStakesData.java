package net.skycomposer.betting.common.domain.dto.betting;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class SumStakesData {
    private List<SumStakeData> sumStakes;
}
