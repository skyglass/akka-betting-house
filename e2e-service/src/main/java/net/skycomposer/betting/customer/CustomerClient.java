package net.skycomposer.betting.customer;

import jakarta.validation.Valid;
import net.skycomposer.betting.common.domain.dto.customer.Customer;
import net.skycomposer.betting.common.domain.dto.customer.CustomerRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@FeignClient(name = "customer")
public interface CustomerClient {

    @PostMapping("/")
    Customer create(@RequestBody @Valid CustomerRequest customerRequest);

    @GetMapping("/{customerId}")
    Customer findById(@PathVariable UUID customerId);
}
