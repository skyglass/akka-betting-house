package net.skycomposer.betting.customer.domain.port;

import net.skycomposer.betting.common.domain.dto.customer.CustomerRequest;
import net.skycomposer.betting.customer.domain.PlacedOrderEvent;
import net.skycomposer.betting.common.domain.dto.customer.Customer;
import java.util.UUID;

public interface CustomerUseCasePort {

  Customer findById(UUID customerId);

  Customer create(CustomerRequest customerRequest);

  boolean reserveBalance(PlacedOrderEvent orderEvent);

  void compensateBalance(PlacedOrderEvent orderEvent);
}
