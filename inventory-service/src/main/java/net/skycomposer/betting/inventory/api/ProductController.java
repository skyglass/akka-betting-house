package net.skycomposer.betting.inventory.api;

import net.skycomposer.betting.common.domain.dto.inventory.AddStockRequest;
import net.skycomposer.betting.common.domain.dto.inventory.ProductRequest;
import net.skycomposer.betting.common.domain.dto.inventory.Product;
import net.skycomposer.betting.inventory.domain.ProductRetryHelper;
import net.skycomposer.betting.inventory.domain.port.ProductUseCasePort;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequiredArgsConstructor
public class ProductController {

  private final ProductUseCasePort productUseCase;

  private final ProductRetryHelper productRetryHelper;

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public Product create(@RequestBody @Valid ProductRequest productRequest) {
    log.info("Create new product {}", productRequest);
    return productUseCase.create(productRequest);
  }

  @PostMapping("/add-stock")
  public Product addStock(@RequestBody @Valid AddStockRequest addStockRequest) {
    log.info("Add stock with quantity {} to product {}", addStockRequest.quantity(), addStockRequest.productId());
    return productRetryHelper.addStock(addStockRequest);
  }

  @GetMapping("/{productId}")
  public Product findById(@PathVariable UUID productId) {
    return productUseCase.findById(productId);
  }
}
