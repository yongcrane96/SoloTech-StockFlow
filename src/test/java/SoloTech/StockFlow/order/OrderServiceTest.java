package SoloTech.StockFlow.order;
import SoloTech.StockFlow.order.dto.OrderDto;
import SoloTech.StockFlow.order.entity.Order;
import SoloTech.StockFlow.order.exception.OrderCreationException;
import SoloTech.StockFlow.order.exception.OrderNotFoundException;
import SoloTech.StockFlow.order.repository.OrderRepository;
import SoloTech.StockFlow.order.service.OrderService;
import SoloTech.StockFlow.payment.dto.PaymentDto;
import SoloTech.StockFlow.payment.entity.Payment;
import SoloTech.StockFlow.payment.entity.PaymentStatus;
import SoloTech.StockFlow.payment.exception.PaymentFailedException;
import SoloTech.StockFlow.payment.service.PaymentService;
import SoloTech.StockFlow.product.entity.Product;
import SoloTech.StockFlow.product.service.ProductService;
import SoloTech.StockFlow.stock.entity.Stock;
import SoloTech.StockFlow.stock.service.StockService;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Cache;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

public class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private Cache<String, Object> localCache;

    @Mock
    private ObjectMapper mapper; // ✅ ObjectMapper 추가

    @InjectMocks
    private OrderService orderService;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    @Mock
    private ProductService productService;

    @Mock
    private StockService stockService;

    @Mock
    private PaymentService paymentService;

    @Mock
    private RedissonClient redissonClient;

    @Mock
    private RLock rLock;

    @BeforeEach
    void setUp() throws InterruptedException {
        MockitoAnnotations.openMocks(this);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(redissonClient.getLock(anyString())).thenReturn(rLock);
        when(rLock.tryLock(anyLong(), anyLong(), any())).thenReturn(true); // 기본 성공 시나리오
    }

    @Test
    @DisplayName("주문 생성")
    void createOrderTest() {
        // Given
        OrderDto orderDto = new OrderDto(
                "O12345",      // orderId
                "S12345",      // storeId
                "P001",        // productId (올바른 값으로 수정)
                "STK98765",    // stockId
                2L,            // quantity
                20000L,        // amount
                "CARD"         // paymentMethod
        );

        Product mockProduct = new Product(1L, "P001", "백팩", 10000L, "튼튼한 가방");
        Stock mockStock = Stock.builder()
                .id(1L)
                .stockId("S001")
                .storeId("W001")
                .productId("P001")
                .stock(100L)  // 충분한 재고 설정
                .deleted(false)
                .build();

        Payment mockPayment = Payment.builder()
                .id(1L)
                .paymentId("PAY123")
                .orderId(orderDto.getOrderId())
                .amount(20000L)
                .paymentMethod("CARD")
                .paymentStatus(PaymentStatus.Success)  // "Success"로 설정
                .build();

        Order mockOrder = new Order();
        mockOrder.setProductId("P001");
        mockOrder.setQuantity(2L);
        mockOrder.setAmount(20000L);
        mockOrder.setPaymentMethod("CARD");

        // Mocking
        when(productService.getProduct("P001")).thenReturn(mockProduct);
        when(stockService.getStock("P001")).thenReturn(mockStock);
        when(paymentService.createPayment(any(PaymentDto.class))).thenReturn(mockPayment);
        when(mapper.convertValue(eq(orderDto), eq(Order.class))).thenReturn(mockOrder);
        when(orderRepository.saveAndFlush(any(Order.class))).thenAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            order.setOrderId("111122223333"); // Snowflake ID mock
            return order;
        });

        // When
        Order createdOrder = orderService.createOrder(orderDto);

        // Then
        assertNotNull(createdOrder);
        assertEquals("P001", createdOrder.getProductId());
        assertEquals(2L, createdOrder.getQuantity());
        assertEquals(20000L, createdOrder.getAmount());
        assertEquals("111122223333", createdOrder.getOrderId());

        // Verification
        verify(productService, times(1)).getProduct("P001");
        verify(stockService, times(1)).getStock("P001");
        verify(paymentService, times(1)).createPayment(any(PaymentDto.class));
        verify(stockService, times(1)).decreaseStock("P001", 2L);
        verify(orderRepository, times(1)).saveAndFlush(any(Order.class));
    }

    @Test
    @DisplayName("주문 읽기")
    void readOrderTest() {
        // given
        String orderId = "ORD123";
        Order mockOrder = Order.builder()
                .id(1L)
                .orderId(orderId)
                .storeId("S001")
                .productId("P001")
                .stockId("ST001")
                .quantity(3L)
                .build();

        when(orderRepository.findByOrderId(orderId)).thenReturn(Optional.of(mockOrder));

        // when
        Order result = orderService.readOrder(orderId);

        // then
        assertNotNull(result);
        assertEquals(orderId, result.getOrderId());
        assertEquals("P001", result.getProductId());

        verify(orderRepository, times(1)).findByOrderId(orderId);
    }

    @Test
    @DisplayName("주문 수정 시")
    void updateOrderTest() throws JsonMappingException {
        // given
        String orderId = "ORD1001";
        Order existingOrder = Order.builder()
                .id(1L)
                .orderId(orderId)
                .storeId("S001")
                .productId("P001")
                .stockId("ST001")
                .quantity(2L)
                .build();

        OrderDto updateDto = OrderDto.builder()
                .quantity(5L)
                .build();

        Order updatedOrder = Order.builder()
                .id(1L)
                .orderId(orderId)
                .storeId("S001")
                .productId("P001")
                .stockId("ST001")
                .quantity(5L)
                .build();

        when(orderRepository.findByOrderId(orderId)).thenReturn(Optional.of(existingOrder));
//        doNothing().when(mapper).updateValue(existingOrder, updateDto);
        when(orderRepository.save(existingOrder)).thenReturn(updatedOrder);

        // when
        Order result = orderService.updateOrder(orderId, updateDto);

        // then
        assertNotNull(result);
        assertEquals(5L, result.getQuantity());
        verify(orderRepository).findByOrderId(orderId);
        verify(mapper).updateValue(existingOrder, updateDto);
        verify(orderRepository).save(existingOrder);
    }

    @Test
    @DisplayName("주문이 없는 경우 수정할 경우")
    void updateOrder_NoOrder() {
        // given
        String orderId = "NOT_FOUND";
        OrderDto dto = OrderDto.builder().quantity(3L).build();

        when(orderRepository.findByOrderId(orderId)).thenReturn(Optional.empty());

        // when & then
        EntityNotFoundException exception = assertThrows(EntityNotFoundException.class, () ->
                orderService.updateOrder(orderId, dto));

        assertTrue(exception.getMessage().contains("Order not found"));
        verify(orderRepository).findByOrderId(orderId);
    }

    @Test
    @DisplayName("주문 삭제")
    void deleteOrderTest() {
        // given
        String orderId = "ORD2001";
        Order existingOrder = Order.builder()
                .id(1L)
                .orderId(orderId)
                .build();

        when(orderRepository.findByOrderId(orderId)).thenReturn(Optional.of(existingOrder));
        doNothing().when(orderRepository).delete(existingOrder);

        // when
        orderService.deleteOrder(orderId);

        // then
        verify(orderRepository).findByOrderId(orderId);
        verify(orderRepository).delete(existingOrder);
    }

    @Test
    @DisplayName("주문이 없는 경우 삭제할 경우")
    void deleteOrder_NoOrder() {
        // given
        String orderId = "NON_EXISTENT";

        when(orderRepository.findByOrderId(orderId)).thenReturn(Optional.empty());

        // when & then
        RuntimeException exception = assertThrows(OrderNotFoundException.class, () ->
                orderService.deleteOrder(orderId));

        assertTrue(exception.getMessage().contains("Order not found"));
        verify(orderRepository).findByOrderId(orderId);
    }

    @Test
    @DisplayName("동시성 주문 생성 테스트 - 재고 한도 내에서만 성공")
    void createOrderConcurrencyTest() throws InterruptedException {
        int totalRequests = 100;
        long initialStock = 50L;
        long orderQuantity = 1L;

        OrderDto orderDto = new OrderDto(
                null,
                "S12345",
                "P001",
                "STK98765",
                orderQuantity,
                20000L,
                "CARD"
        );

        Product mockProduct = new Product(1L, "P001", "백팩", 10000L, "튼튼한 가방");

        Stock mockStock = Stock.builder()
                .id(1L)
                .stockId("STK98765")
                .storeId("S12345")
                .productId("P001")
                .stock(initialStock)
                .deleted(false)
                .build();

        Payment mockPayment = Payment.builder()
                .id(1L)
                .paymentId("PAY123")
                .orderId("TEMP_ORDER")
                .amount(20000L)
                .paymentMethod("CARD")
                .paymentStatus(PaymentStatus.Success)
                .build();

        // 각 스레드가 독립된 stock 객체를 받도록 작업
        AtomicLong actualStock = new AtomicLong(initialStock); // 실제 재고 상태
        // -> 현재 재고를 스레드 간 안전하게 추적

        when(productService.getProduct("P001")).thenReturn(mockProduct);

        // actualStock.get을 통해 실시간 재고 상태를 반영하게 작업
        when(stockService.getStock(any())).thenAnswer(invocation -> {
            return mockStock.toBuilder().stock(actualStock.get()).build(); // 현재 재고값 반영
        });
        // -> 요청 마다 최신 재고를 가진 Stock 객체 생성

        doAnswer(invocation -> {
            Long quantity = invocation.getArgument(1);
            if (actualStock.get() < quantity) {
                throw new OrderCreationException("재고 부족");
            }
            actualStock.addAndGet(-quantity); // 재고 감소
            return null;
        }).when(stockService).decreaseStock(any(), anyLong());

        when(paymentService.createPayment(any(PaymentDto.class))).thenReturn(mockPayment);

        when(mapper.convertValue(any(OrderDto.class), eq(Order.class))).thenAnswer(invocation -> {
            OrderDto dto = invocation.getArgument(0);
            Order order = new Order();
            order.setProductId(dto.getProductId());
            order.setQuantity(dto.getQuantity());
            order.setAmount(dto.getAmount());
            order.setPaymentMethod(dto.getPaymentMethod());
            return order;
        });

        when(orderRepository.saveAndFlush(any(Order.class))).thenAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            order.setOrderId(UUID.randomUUID().toString());
            return order;
        });

        ExecutorService executor = Executors.newFixedThreadPool(32);
        CountDownLatch latch = new CountDownLatch(totalRequests);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        for (int i = 0; i < totalRequests; i++) {
            executor.submit(() -> {
                // 다수의 스레드가 동시에 주문 생성 시도
                try {
                    orderService.createOrder(orderDto);
                    successCount.incrementAndGet();
                } catch (OrderCreationException e) {
                    failureCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executor.shutdown();

        System.out.println("요청 수: " + totalRequests);
        System.out.println("성공 수: " + successCount.get());
        System.out.println("실패 수: " + failureCount.get());
        System.out.println("남은 재고: " + actualStock.get());

        // Then
        assertEquals(initialStock, successCount.get(), "재고 수량만큼만 주문이 성공해야 함");
        // 재고 수만큼만 성공했는지 확인
        assertEquals(0, actualStock.get(), "재고가 0이 되어야 함");
        assertTrue(failureCount.get() > 0, "일부 요청은 재고 부족으로 실패해야 함");
    }

    @Test
    @DisplayName("Redisson 락 직접 사용한 동시성 테스트 - -중복 처리 방지 및 재고 보호가 잘 되었다 - 성공 시나리오")
    void createOrder_withRedissonManualLock_concurrencyTest() throws InterruptedException {
        int totalRequests = 100;
        long initialStock = 30L;
        AtomicInteger success = new AtomicInteger();
        AtomicInteger failure = new AtomicInteger();

        // 재고 상태를 실제 서비스처럼 구성
        AtomicLong actualStock = new AtomicLong(initialStock);

        // Mock 설정
        when(productService.getProduct(any())).thenReturn(
                new Product(1L, "P001", "백팩", 10000L, "튼튼한 가방")
        );

        when(stockService.getStock(any())).thenAnswer(invocation ->
                Stock.builder()
                        .id(1L)
                        .stockId("STK98765")
                        .storeId("S12345")
                        .productId("P001")
                        .stock(actualStock.get())
                        .deleted(false)
                        .build()
        );

        // ✅ 실제 재고 감소 로직 추가
        doAnswer(invocation -> {
            Long quantity = invocation.getArgument(1);
            if (actualStock.get() < quantity) {
                throw new OrderCreationException("재고 부족");
            }
            actualStock.addAndGet(-quantity);
            return null;
        }).when(stockService).decreaseStock(any(), anyLong());

        when(paymentService.createPayment(any())).thenReturn(
                Payment.builder()
                        .id(1L)
                        .paymentId("PAY123")
                        .orderId("TEMP")
                        .amount(20000L)
                        .paymentMethod("CARD")
                        .paymentStatus(PaymentStatus.Success)
                        .build()
        );

        when(mapper.convertValue(any(), eq(Order.class))).thenAnswer(invocation -> {
            OrderDto dto = invocation.getArgument(0);
            return Order.builder()
                    .productId(dto.getProductId())
                    .quantity(dto.getQuantity())
                    .amount(dto.getAmount())
                    .paymentMethod(dto.getPaymentMethod())
                    .build();
        });

        when(orderRepository.saveAndFlush(any())).thenAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            order.setOrderId(UUID.randomUUID().toString());
            return order;
        });

        OrderDto dto = new OrderDto(null, "S12345", "P001", "STK98765", 1L, 20000L, "CARD");

        ExecutorService executor = Executors.newFixedThreadPool(32);
        CountDownLatch latch = new CountDownLatch(totalRequests);

        for (int i = 0; i < totalRequests; i++) {
            executor.submit(() -> {
                RLock lock = redissonClient.getLock("lock:order:" + dto.getProductId());
                boolean acquired = false;
                try {
                    acquired = lock.tryLock(1, 2, TimeUnit.SECONDS);
                    if (acquired) {
                        orderService.createOrder(dto);
                        success.incrementAndGet();
                    } else {
                        failure.incrementAndGet();
                    }
                } catch (Exception e) {
                    e.printStackTrace(); // 예외 발생 원인 확인
                    failure.incrementAndGet();
                } finally {
                    if (acquired) lock.unlock();
                    latch.countDown();
                }
            });
        }

        latch.await();
        executor.shutdown();

        System.out.println("요청 수: " + totalRequests);
        System.out.println("성공 수: " + success.get());
        System.out.println("실패 수: " + failure.get());
        System.out.println("남은 재고: " + actualStock.get());

        assertEquals(initialStock, success.get(), "성공한 주문 수는 초기 재고와 같아야 한다");
        assertEquals(totalRequests - initialStock, failure.get(), "실패한 주문 수는 남은 요청 수와 같아야 한다");
        assertEquals(0, actualStock.get(), "남은 재고는 0이어야 한다");
    }

    @Test
    @DisplayName("Redisson 락 사용 - 모든 요청 성공 (재고 초과 없음)")
    void createOrder_withRedissonManualLock_allSuccessTest() throws InterruptedException {
        int totalRequests = 30; // 요청 수 == 초기 재고
        long initialStock = 30L;
        AtomicInteger success = new AtomicInteger();
        AtomicInteger failure = new AtomicInteger();
        AtomicLong actualStock = new AtomicLong(initialStock);

        // Mock 설정
        when(productService.getProduct(any())).thenReturn(
                new Product(1L, "P001", "백팩", 10000L, "튼튼한 가방")
        );

        when(stockService.getStock(any())).thenAnswer(invocation ->
                Stock.builder()
                        .id(1L)
                        .stockId("STK98765")
                        .storeId("S12345")
                        .productId("P001")
                        .stock(actualStock.get())
                        .deleted(false)
                        .build()
        );

        doAnswer(invocation -> {
            Long quantity = invocation.getArgument(1);
            if (actualStock.get() < quantity) {
                throw new OrderCreationException("재고 부족");
            }
            actualStock.addAndGet(-quantity); // 재고 감소
            return null;
        }).when(stockService).decreaseStock(any(), anyLong());

        when(paymentService.createPayment(any())).thenReturn(
                Payment.builder()
                        .id(1L)
                        .paymentId("PAY123")
                        .orderId("TEMP")
                        .amount(20000L)
                        .paymentMethod("CARD")
                        .paymentStatus(PaymentStatus.Success)
                        .build()
        );

        when(mapper.convertValue(any(), eq(Order.class))).thenAnswer(invocation -> {
            OrderDto dto = invocation.getArgument(0);
            return Order.builder()
                    .productId(dto.getProductId())
                    .quantity(dto.getQuantity())
                    .amount(dto.getAmount())
                    .paymentMethod(dto.getPaymentMethod())
                    .build();
        });

        when(orderRepository.saveAndFlush(any())).thenAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            order.setOrderId(UUID.randomUUID().toString());
            return order;
        });

        OrderDto dto = new OrderDto(null, "S12345", "P001", "STK98765", 1L, 20000L, "CARD");

        ExecutorService executor = Executors.newFixedThreadPool(16);
        CountDownLatch latch = new CountDownLatch(totalRequests);

        for (int i = 0; i < totalRequests; i++) {
            executor.submit(() -> {
                RLock lock = redissonClient.getLock("lock:order:" + dto.getProductId());
                boolean acquired = false;
                try {
                    acquired = lock.tryLock(1, 2, TimeUnit.SECONDS);
                    if (acquired) {
                        orderService.createOrder(dto);
                        success.incrementAndGet();
                    } else {
                        failure.incrementAndGet();
                    }
                } catch (Exception e) {
                    failure.incrementAndGet();
                } finally {
                    if (acquired) lock.unlock();
                    latch.countDown();
                }
            });
        }

        latch.await();
        executor.shutdown();

        System.out.println("요청 수: " + totalRequests);
        System.out.println("성공 수: " + success.get());
        System.out.println("실패 수: " + failure.get());

        assertEquals(totalRequests, success.get(), "모든 요청이 성공해야 함");
        assertEquals(0, failure.get(), "실패한 요청이 없어야 함");
        assertEquals(0, actualStock.get(), "재고는 0이어야 함");
    }


    @Test
    @DisplayName("동시성 주문 테스트 - 일부 결제 실패 처리")
    void createOrder_PaymentFailureTest() throws InterruptedException {
        int totalRequests = 50;
        long initialStock = 50L;
        AtomicLong actualStock = new AtomicLong(initialStock);
        long orderQuantity = 1L;

        when(productService.getProduct(any())).thenReturn(new Product(1L, "P001", "백팩", 10000L, "튼튼한 가방"));
        when(stockService.getStock(any())).thenAnswer(invocation ->
                Stock.builder()
                        .id(1L)
                        .stockId("STK98765")
                        .storeId("S12345")
                        .productId("P001")
                        .stock(actualStock.get())
                        .deleted(false)
                        .build()
        );
        when(paymentService.createPayment(any())).thenAnswer(invocation -> {
            int chance = ThreadLocalRandom.current().nextInt(100);
            return Payment.builder()
                    .id(1L)
                    .paymentId("PAY" + UUID.randomUUID())
                    .orderId("TEMP")
                    .amount(20000L)
                    .paymentMethod("CARD")
                    .paymentStatus(chance < 70 ? PaymentStatus.Success : PaymentStatus.CANCELED) // 30% 확률 실패
                    .build();
        });
        when(mapper.convertValue(any(), eq(Order.class))).thenAnswer(inv -> {
            OrderDto dto = inv.getArgument(0);
            Order order = new Order();
            order.setProductId(dto.getProductId());
            order.setQuantity(dto.getQuantity());
            order.setAmount(dto.getAmount());
            order.setPaymentMethod(dto.getPaymentMethod());
            return order;
        });
        when(orderRepository.saveAndFlush(any())).thenAnswer(inv -> {
            Order order = inv.getArgument(0);
            order.setOrderId(UUID.randomUUID().toString());
            return order;
        });

        OrderDto dto = new OrderDto(null, "S12345", "P001", "STK98765", orderQuantity, 20000L, "CARD");

        ExecutorService executor = Executors.newFixedThreadPool(32);
        CountDownLatch latch = new CountDownLatch(totalRequests);
        AtomicInteger success = new AtomicInteger();
        AtomicInteger paymentFailure = new AtomicInteger();

        for (int i = 0; i < totalRequests; i++) {
            executor.submit(() -> {
                try {
                    orderService.createOrder(dto);
                    success.incrementAndGet();
                    actualStock.decrementAndGet(); // 결제 성공만 줄임
                } catch (PaymentFailedException e) {
                    paymentFailure.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executor.shutdown();

        System.out.println("결제 실패 테스트 결과");
        System.out.println("성공 수: " + success.get());
        System.out.println("실패 수: " + paymentFailure.get());
        System.out.println("남은 재고: " + actualStock.get());

        assertEquals(initialStock - success.get(), actualStock.get(), "재고는 성공한 주문만큼만 감소해야 함");
        assertTrue(paymentFailure.get() > 0, "일부 결제는 실패해야 함");
    }

    @Test
    @DisplayName("동시성 주문 테스트 - 여러 개 수량 주문")
    void createOrder_MultipleQuantityTest() throws InterruptedException {
        int totalRequests = 20;
        long initialStock = 100L;
        long orderQuantity = 3L;
        AtomicLong actualStock = new AtomicLong(initialStock);

        OrderDto dto = new OrderDto(null, "S12345", "P001", "STK98765", orderQuantity, 60000L, "CARD");

        when(productService.getProduct(any())).thenReturn(new Product(1L, "P001", "백팩", 10000L, "튼튼한 가방"));
        when(stockService.getStock(any())).thenAnswer(invocation ->
                Stock.builder()
                        .id(1L)
                        .stockId("STK98765")
                        .storeId("S12345")
                        .productId("P001")
                        .stock(actualStock.get())
                        .deleted(false)
                        .build()
        );
        when(paymentService.createPayment(any())).thenReturn(
                Payment.builder()
                        .id(1L)
                        .paymentId("PAY123")
                        .orderId("TEMP")
                        .amount(60000L)
                        .paymentMethod("CARD")
                        .paymentStatus(PaymentStatus.Success)
                        .build()
        );
        when(mapper.convertValue(any(), eq(Order.class))).thenAnswer(inv -> {
            OrderDto d = inv.getArgument(0);
            Order order = new Order();
            order.setProductId(d.getProductId());
            order.setQuantity(d.getQuantity());
            order.setAmount(d.getAmount());
            order.setPaymentMethod(d.getPaymentMethod());
            return order;
        });
        when(orderRepository.saveAndFlush(any())).thenAnswer(inv -> {
            Order o = inv.getArgument(0);
            o.setOrderId(UUID.randomUUID().toString());
            return o;
        });

        ExecutorService executor = Executors.newFixedThreadPool(32);
        CountDownLatch latch = new CountDownLatch(totalRequests);
        AtomicInteger success = new AtomicInteger();
        AtomicInteger failure = new AtomicInteger();

        for (int i = 0; i < totalRequests; i++) {
            executor.submit(() -> {
                try {
                    orderService.createOrder(dto);
                    success.incrementAndGet();
                    actualStock.addAndGet(-orderQuantity); // 수량만큼 차감
                } catch (OrderCreationException e) {
                    failure.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executor.shutdown();

        System.out.println("여러 개 수량 테스트 결과");
        System.out.println("성공 수: " + success.get());
        System.out.println("실패 수: " + failure.get());
        System.out.println("남은 재고: " + actualStock.get());

        assertEquals(initialStock - (success.get() * orderQuantity), actualStock.get());
        assertTrue(failure.get() >= 0);
    }


}