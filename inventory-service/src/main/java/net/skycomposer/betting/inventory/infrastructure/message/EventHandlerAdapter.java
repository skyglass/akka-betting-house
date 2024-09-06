package net.skycomposer.betting.inventory.infrastructure.message;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.skycomposer.betting.inventory.domain.port.EventHandlerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.function.Consumer;

@Slf4j
@Component
@RequiredArgsConstructor
public class EventHandlerAdapter implements EventHandlerPort {

  private final EventHandlerDelegate eventHandlerDelegate;

  @Bean
  @Override
  @Transactional
  public Consumer<Message<String>> handleReserveProductStockRequest() {
    return event -> eventHandlerDelegate.handleReserveProductStockRequest(event);
  }

  @Bean
  @Override
  @Transactional
  public Consumer<Message<String>> handleDlq() {
    return event -> eventHandlerDelegate.handleDlq(event);
  }
}
