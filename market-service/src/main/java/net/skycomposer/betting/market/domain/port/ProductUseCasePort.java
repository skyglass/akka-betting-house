package net.skycomposer.betting.market.domain.port;

import net.skycomposer.betting.common.domain.dto.inventory.AddStockRequest;
import net.skycomposer.betting.market.domain.PlacedOrderEvent;
import net.skycomposer.betting.common.domain.dto.inventory.ProductRequest;
import net.skycomposer.betting.common.domain.dto.inventory.Product;

import java.util.UUID;

public interface ProductUseCasePort {

  Product findById(UUID productId);

  Product create(ProductRequest productRequest);

  Product addStock(AddStockRequest addStockRequest);

  boolean reserveProduct(PlacedOrderEvent orderEvent);
}
