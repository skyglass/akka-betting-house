package net.skycomposer.betting.customer.domain.port;

import net.skycomposer.betting.common.domain.dto.customer.Customer;
import java.util.Optional;
import java.util.UUID;

public interface CustomerRepositoryPort {

  Optional<Customer> findCustomerById(UUID customerId);

  Customer saveCustomer(Customer customer);
}
