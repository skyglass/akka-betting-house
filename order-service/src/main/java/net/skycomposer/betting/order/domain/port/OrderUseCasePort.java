package net.skycomposer.betting.order.domain.port;

import net.skycomposer.betting.common.domain.dto.order.OrderRequest;
import net.skycomposer.betting.common.domain.dto.order.Order;

import java.util.UUID;

public interface OrderUseCasePort {

  UUID placeOrder(OrderRequest orderRequest);

  void updateOrderStatus(UUID orderId, boolean success);

  Order getOrder(UUID orderId);

}
