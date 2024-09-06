package net.skycomposer.betting.betting.domain.port;

import net.skycomposer.betting.common.domain.dto.order.Order;
import java.util.Optional;
import java.util.UUID;

public interface OrderRepositoryPort {

  Optional<Order> findOrderById(UUID orderId);

  void saveOrder(Order order);

  void exportOutBoxEvent(Order order);
}
