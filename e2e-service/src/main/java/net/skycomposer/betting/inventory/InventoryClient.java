package net.skycomposer.betting.inventory;

import jakarta.validation.Valid;
import net.skycomposer.betting.common.domain.dto.inventory.AddStockRequest;
import net.skycomposer.betting.common.domain.dto.inventory.Product;
import net.skycomposer.betting.common.domain.dto.inventory.ProductRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@FeignClient(name = "inventory")
public interface InventoryClient {

    @PostMapping("/")
    Product create(@RequestBody @Valid ProductRequest productRequest);

    @GetMapping("/{productId}")
    Product findById(@PathVariable UUID productId);

    @PostMapping("/add-stock")
    public Product addStock(@RequestBody @Valid AddStockRequest addStockRequest);

}
