package net.skycomposer.betting.common.domain.dto.market;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MarketData {
    private String marketId;
    private FixtureData fixture;
    private OddsData odds;
    private Result result;
    private long opensAt;
    private boolean open;

    public enum Result {
        HOME_WINS(0),
        HOME_LOSES(1),
        TIE(2);

        private int value;

        private Result(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }

        public static Result fromValue(int value) {
            for (Result result: values()) {
                if (result.getValue() == value) {
                    return result;
                }
            }
            throw new IllegalArgumentException(String.format("Unknown value for MarketData.Result enum: %d", value));
        }
    }
}