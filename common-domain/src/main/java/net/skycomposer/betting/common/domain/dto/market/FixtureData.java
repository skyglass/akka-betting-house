package net.skycomposer.betting.common.domain.dto.market;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class FixtureData {
    private String id;
    private String homeTeam;
    private String awayTeam;
}