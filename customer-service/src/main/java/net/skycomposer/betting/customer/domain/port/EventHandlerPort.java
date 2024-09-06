package net.skycomposer.betting.customer.domain.port;

import java.util.function.Consumer;
import org.springframework.messaging.Message;

public interface EventHandlerPort {

  Consumer<Message<String>> handleReserveCustomerBalanceRequest();

  Consumer<Message<String>> handleCompensateCustomerBalanceRequest();
}
