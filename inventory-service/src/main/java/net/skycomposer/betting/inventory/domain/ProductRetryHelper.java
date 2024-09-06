package net.skycomposer.betting.inventory.domain;

import lombok.RequiredArgsConstructor;
import net.skycomposer.betting.common.domain.dto.inventory.AddStockRequest;
import net.skycomposer.betting.common.domain.dto.inventory.Product;
import net.skycomposer.betting.inventory.domain.port.ProductUseCasePort;
import org.springframework.dao.DataAccessException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ProductRetryHelper {

    private final ProductUseCasePort productUseCase;

    @Retryable(retryFor = {DataAccessException.class},
            maxAttempts = 5, backoff = @Backoff(delay = 1000))
    public Product addStock(AddStockRequest addStockRequest) {
        return productUseCase.addStock(addStockRequest);
    }

}
