package net.skycomposer.betting.common.domain.dto.betting;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SumStakesData {
    private List<SumStakeData> sumStakes;
}
