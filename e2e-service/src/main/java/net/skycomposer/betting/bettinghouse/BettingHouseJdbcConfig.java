package net.skycomposer.betting.bettinghouse;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

@Configuration
public class BettingHouseJdbcConfig {

    @Bean
    @ConfigurationProperties("spring.datasource.betting-house")
    public DataSourceProperties bettingHouseDataSourceProperties() {
        return new DataSourceProperties();
    }

    @Bean
    @Primary
    @ConfigurationProperties("spring.datasource.betting-house.hikari")
    public DataSource bettingHouseDataSource() {
        return bettingHouseDataSourceProperties()
                .initializeDataSourceBuilder()
                .build();
    }

    @Bean
    public JdbcTemplate bettingHouseJdbcTemplate(@Qualifier("bettingHouseDataSource") DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }

}
