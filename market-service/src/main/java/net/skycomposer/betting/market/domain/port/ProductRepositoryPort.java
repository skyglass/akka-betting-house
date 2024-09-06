package net.skycomposer.betting.market.domain.port;

import net.skycomposer.betting.common.domain.dto.inventory.Product;
import java.util.Optional;
import java.util.UUID;

public interface ProductRepositoryPort {

  Optional<Product> findProductById(UUID productId);

  Product saveProduct(Product product);
}
