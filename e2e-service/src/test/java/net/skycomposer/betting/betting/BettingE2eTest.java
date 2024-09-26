package net.skycomposer.betting.betting;

import lombok.SneakyThrows;
import net.skycomposer.betting.customer.CustomerTestHelper;
import net.skycomposer.betting.market.MarketTestHelper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import net.skycomposer.betting.common.E2eTest;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
public class BettingE2eTest extends E2eTest {

    @Autowired
    private CustomerTestHelper customerTestHelper;

    @Autowired
    private MarketTestHelper marketTestHelper;

    @Autowired
    private BettingTestHelper bettingTestHelper;


    @Test
    @SneakyThrows
    void test() {
        Integer stockQuantity = 20;
        String customerName = "testCustomer";
        double customerBalance = 100.0;
        String productName = "testProduct";
        Integer orderQuantity = 2;
        double productPrice = 2.0;
        AtomicInteger counter = new AtomicInteger(0);

        Customer customer = customerTestHelper.createCustomer(customerName, customerBalance);
        Product product = inventoryTestHelper.createProduct(productName, stockQuantity);

        UUID orderId = orderTestHelper.placeOrder(product.getId(), customer.getId(),
                orderQuantity, productPrice, counter);

        Order orderCreated =  RetryHelper.retry(() ->
            orderTestHelper.getOrder(orderId, counter)
        );

        assertNotNull(orderCreated);

        assertEquals(orderId, orderCreated.getId());
        assertEquals(product.getId(), orderCreated.getProductId());
        assertEquals(orderQuantity, orderCreated.getQuantity());

        Boolean stockReduced =  RetryHelper.retry(() -> {
            Product p = inventoryTestHelper.getProduct(product.getId(), counter);
            return p.getStocks() == stockQuantity - orderQuantity;
        });

        assertTrue(stockReduced);

        inventoryTestHelper.addStock(product.getId(), 3, counter);

        Boolean stockIncreased =  RetryHelper.retry(() -> {
            Product p = inventoryTestHelper.getProduct(product.getId(), counter);
            return p.getStocks() == stockQuantity - orderQuantity + 3;
        });

        assertTrue(stockIncreased);
    }


}
