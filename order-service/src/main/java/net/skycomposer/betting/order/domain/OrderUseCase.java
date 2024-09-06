package net.skycomposer.betting.order.domain;

import com.fasterxml.jackson.databind.ObjectMapper;
import net.skycomposer.betting.common.domain.dto.order.OrderRequest;
import net.skycomposer.betting.common.domain.dto.order.Order;
import net.skycomposer.betting.common.domain.dto.order.OrderStatus;
import net.skycomposer.betting.order.domain.port.OrderRepositoryPort;
import net.skycomposer.betting.order.domain.port.OrderUseCasePort;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class OrderUseCase implements OrderUseCasePort {

  private final ObjectMapper mapper;

  private final OrderRepositoryPort orderRepository;

  @Override
  public UUID placeOrder(OrderRequest orderRequest) {
    var order = mapper.convertValue(orderRequest, Order.class);
    order.setCreatedAt(Timestamp.from(Instant.now()));
    order.setStatus(OrderStatus.PENDING);
    order.setId(UUID.randomUUID());
    orderRepository.saveOrder(order);
    orderRepository.exportOutBoxEvent(order);
    return order.getId();
  }

  @Override
  public void updateOrderStatus(UUID orderId, boolean success) {
    var order = orderRepository.findOrderById(orderId);
    if (order.isPresent()) {
      if (success) {
        order.get().setStatus(OrderStatus.COMPLETED);
      } else {
        order.get().setStatus(OrderStatus.CANCELED);
      }
      orderRepository.saveOrder(order.get());
    }
  }

  @Override
  public Order getOrder(UUID orderId) {
    return orderRepository.findOrderById(orderId).orElseThrow(
            () -> new RuntimeException("Order with id %s not found".formatted(orderId)));
  }
}
