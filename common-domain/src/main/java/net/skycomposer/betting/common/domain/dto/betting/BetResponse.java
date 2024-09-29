package net.skycomposer.betting.common.domain.dto.betting;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BetResponse {

    private String betId;
    private String message;
}

