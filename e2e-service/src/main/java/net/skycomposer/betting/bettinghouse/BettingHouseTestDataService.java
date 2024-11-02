package net.skycomposer.betting.bettinghouse;

import lombok.extern.slf4j.Slf4j;
import net.skycomposer.betting.testdata.JdbcTestDataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class BettingHouseTestDataService extends JdbcTestDataService {

    @Autowired
    @Qualifier("bettingHouseJdbcTemplate")
    private JdbcTemplate jdbcTemplate;

    @Override
    protected JdbcTemplate getJdbcTemplate() {
        return jdbcTemplate;
    }

    @Override
    public void resetDatabase() {
        executeString("DELETE FROM event_tag");
        executeString("DELETE FROM event_journal");
        executeString("DELETE FROM snapshot");
        executeString("DELETE FROM akka_projection_management");
        executeString("DELETE FROM akka_projection_offset_store");
        executeString("DELETE FROM bet_wallet_market");
        executeString("DELETE FROM wallet_request");
    }

}
