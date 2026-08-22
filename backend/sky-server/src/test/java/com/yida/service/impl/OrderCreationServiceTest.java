package com.yida.service.impl;

import com.yida.constant.StatusConstant;
import com.yida.dto.OrdersSubmitDTO;
import com.yida.entity.AddressBook;
import com.yida.entity.Dish;
import com.yida.entity.OrderDetail;
import com.yida.entity.Orders;
import com.yida.entity.Setmeal;
import com.yida.entity.ShoppingCart;
import com.yida.exception.AddressBookBusinessException;
import com.yida.exception.OrderBusinessException;
import com.yida.exception.ShoppingCartBusinessException;
import com.yida.mapper.AddressBookMapper;
import com.yida.mapper.DishMapper;
import com.yida.mapper.OrderDetailMapper;
import com.yida.mapper.OrderMapper;
import com.yida.mapper.OrderOutboxMapper;
import com.yida.messaging.outbox.OrderOutbox;
import com.yida.mapper.SetmealMapper;
import com.yida.mapper.ShoppingCartMapper;
import com.yida.service.support.OrderCreationResult;
import com.yida.service.support.OrderNumberGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderCreationServiceTest {

    private static final long USER_ID = 42L;

    @Mock
    private OrderMapper orderMapper;
    @Mock
    private OrderDetailMapper orderDetailMapper;
    @Mock
    private AddressBookMapper addressBookMapper;
    @Mock
    private ShoppingCartMapper shoppingCartMapper;
    @Mock
    private DishMapper dishMapper;
    @Mock
    private SetmealMapper setmealMapper;
    @Mock
    private OrderNumberGenerator orderNumberGenerator;
    @Mock
    private OrderOutboxMapper orderOutboxMapper;

    @InjectMocks
    private OrderCreationService orderCreationService;

    @Test
    void repeatedRequestReturnsExistingOrderWithoutTouchingCart() {
        Orders existing = Orders.builder().id(99L).requestId("request-1").build();
        when(orderMapper.getByUserIdAndRequestId(USER_ID, "request-1")).thenReturn(existing);

        OrderCreationResult result = orderCreationService.create(submitRequest(), USER_ID);

        assertFalse(result.isCreated());
        assertSame(existing, result.getOrder());
        verify(addressBookMapper, never()).getByIdAndUserId(any(), any());
        verify(shoppingCartMapper, never()).listByUserIdForUpdate(any());
        verify(orderMapper, never()).insert(any());
    }

    @Test
    void rejectsAddressThatDoesNotBelongToCurrentUser() {
        OrdersSubmitDTO request = submitRequest();
        when(addressBookMapper.getByIdAndUserId(8L, USER_ID)).thenReturn(null);

        assertThrows(AddressBookBusinessException.class, () -> orderCreationService.create(request, USER_ID));

        verify(shoppingCartMapper, never()).listByUserIdForUpdate(any());
        verify(orderMapper, never()).insert(any());
    }

    @Test
    void requestCommittedWhileWaitingForCartLockReturnsExistingOrder() {
        Orders existing = Orders.builder().id(99L).requestId("request-1").build();
        when(addressBookMapper.getByIdAndUserId(8L, USER_ID)).thenReturn(address());
        when(shoppingCartMapper.listByUserIdForUpdate(USER_ID)).thenReturn(Collections.emptyList());
        when(orderMapper.getByUserIdAndRequestIdForUpdate(USER_ID, "request-1")).thenReturn(existing);

        OrderCreationResult result = orderCreationService.create(submitRequest(), USER_ID);

        assertFalse(result.isCreated());
        assertSame(existing, result.getOrder());
        verify(orderMapper, never()).insert(any());
    }
    @Test
    void rejectsEmptyShoppingCart() {
        when(addressBookMapper.getByIdAndUserId(8L, USER_ID)).thenReturn(address());
        when(shoppingCartMapper.listByUserIdForUpdate(USER_ID)).thenReturn(Collections.emptyList());

        assertThrows(ShoppingCartBusinessException.class,
                () -> orderCreationService.create(submitRequest(), USER_ID));

        verify(orderMapper, never()).insert(any());
        verify(orderDetailMapper, never()).insertBatch(any());
    }

    @Test
    void recalculatesCurrentDishAndSetmealPricesAndAddsPackAmount() {
        OrdersSubmitDTO request = submitRequest();
        request.setAmount(new BigDecimal("9999.00"));
        request.setPackAmount(5);
        ShoppingCart dishCart = ShoppingCart.builder()
                .id(1L).dishId(10L).number(2).amount(new BigDecimal("0.01")).dishFlavor("微辣").build();
        ShoppingCart setmealCart = ShoppingCart.builder()
                .id(2L).setmealId(20L).number(1).amount(new BigDecimal("999.00")).build();
        when(addressBookMapper.getByIdAndUserId(8L, USER_ID)).thenReturn(address());
        when(shoppingCartMapper.listByUserIdForUpdate(USER_ID)).thenReturn(Arrays.asList(setmealCart, dishCart));
        when(dishMapper.getByIdForUpdate(10L)).thenReturn(Dish.builder()
                .id(10L).name("当前菜品").image("dish.png")
                .price(new BigDecimal("12.50")).status(StatusConstant.ENABLE).build());
        when(setmealMapper.getByIdForUpdate(20L)).thenReturn(Setmeal.builder()
                .id(20L).name("当前套餐").image("setmeal.png")
                .price(new BigDecimal("30.00")).status(StatusConstant.ENABLE).build());
        when(orderNumberGenerator.next()).thenReturn("order-number-1");
        when(orderMapper.insert(any(Orders.class))).thenAnswer(invocation -> {
            invocation.<Orders>getArgument(0).setId(99L);
            return 1;
        });

        OrderCreationResult result = orderCreationService.create(request, USER_ID);

        assertTrue(result.isCreated());
        ArgumentCaptor<Orders> orderCaptor = ArgumentCaptor.forClass(Orders.class);
        verify(orderMapper).insert(orderCaptor.capture());
        Orders insertedOrder = orderCaptor.getValue();
        assertEquals("request-1", insertedOrder.getRequestId());
        assertEquals("order-number-1", insertedOrder.getNumber());
        assertEquals(new BigDecimal("60.00"), insertedOrder.getAmount());
        assertEquals(5, insertedOrder.getPackAmount());
        assertEquals(Orders.PENDING_PAYMENT, insertedOrder.getStatus());
        assertEquals(Orders.UN_PAID, insertedOrder.getPayStatus());

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<OrderDetail>> detailCaptor = ArgumentCaptor.forClass(List.class);
        verify(orderDetailMapper).insertBatch(detailCaptor.capture());
        List<OrderDetail> details = detailCaptor.getValue();
        assertEquals(2, details.size());
        assertEquals("当前菜品", details.get(0).getName());
        assertEquals(new BigDecimal("12.50"), details.get(0).getAmount());
        assertEquals(99L, details.get(0).getOrderId());
        assertEquals("当前套餐", details.get(1).getName());
        assertEquals(new BigDecimal("30.00"), details.get(1).getAmount());
        verify(shoppingCartMapper).deleteByUserId(USER_ID);
        ArgumentCaptor<OrderOutbox> outboxCaptor = ArgumentCaptor.forClass(OrderOutbox.class);
        verify(orderOutboxMapper).insert(outboxCaptor.capture());
        assertEquals(99L, outboxCaptor.getValue().getAggregateId());
        assertEquals(OrderOutbox.PENDING, outboxCaptor.getValue().getStatus());
    }

    @Test
    void rejectsNonPositiveCurrentProductPrice() {
        ShoppingCart cart = ShoppingCart.builder().id(1L).dishId(10L).number(1).build();
        when(addressBookMapper.getByIdAndUserId(8L, USER_ID)).thenReturn(address());
        when(shoppingCartMapper.listByUserIdForUpdate(USER_ID)).thenReturn(Collections.singletonList(cart));
        when(dishMapper.getByIdForUpdate(10L)).thenReturn(Dish.builder()
                .id(10L).price(new BigDecimal("-0.01")).status(StatusConstant.ENABLE).build());

        assertThrows(OrderBusinessException.class,
                () -> orderCreationService.create(submitRequest(), USER_ID));

        verify(orderMapper, never()).insert(any());
        verify(shoppingCartMapper, never()).deleteByUserId(any());
    }

    @Test
    void rejectsInvalidCartQuantity() {
        ShoppingCart cart = ShoppingCart.builder().id(1L).dishId(10L).number(0).build();
        when(addressBookMapper.getByIdAndUserId(8L, USER_ID)).thenReturn(address());
        when(shoppingCartMapper.listByUserIdForUpdate(USER_ID)).thenReturn(Collections.singletonList(cart));

        assertThrows(OrderBusinessException.class,
                () -> orderCreationService.create(submitRequest(), USER_ID));

        verify(dishMapper, never()).getByIdForUpdate(any());
        verify(orderMapper, never()).insert(any());
    }

    @Test
    void detailFailurePropagatesAndDoesNotClearCart() {
        ShoppingCart cart = ShoppingCart.builder().id(1L).dishId(10L).number(1).build();
        when(addressBookMapper.getByIdAndUserId(8L, USER_ID)).thenReturn(address());
        when(shoppingCartMapper.listByUserIdForUpdate(USER_ID)).thenReturn(Collections.singletonList(cart));
        when(dishMapper.getByIdForUpdate(10L)).thenReturn(Dish.builder()
                .id(10L).name("当前菜品").price(BigDecimal.TEN).status(StatusConstant.ENABLE).build());
        when(orderNumberGenerator.next()).thenReturn("order-number-1");
        when(orderMapper.insert(any(Orders.class))).thenAnswer(invocation -> {
            invocation.<Orders>getArgument(0).setId(99L);
            return 1;
        });
        org.mockito.Mockito.doThrow(new IllegalStateException("detail insert failed"))
                .when(orderDetailMapper).insertBatch(any());

        assertThrows(IllegalStateException.class,
                () -> orderCreationService.create(submitRequest(), USER_ID));

        verify(shoppingCartMapper, never()).deleteByUserId(any());
    }

    @Test
    void createMethodDefinesRollbackForAllExceptions() throws Exception {
        Transactional transactional = OrderCreationService.class
                .getMethod("create", OrdersSubmitDTO.class, Long.class)
                .getAnnotation(Transactional.class);

        assertNotNull(transactional);
        assertEquals(Exception.class, transactional.rollbackFor()[0]);
    }

    private OrdersSubmitDTO submitRequest() {
        OrdersSubmitDTO request = new OrdersSubmitDTO();
        request.setRequestId("request-1");
        request.setAddressBookId(8L);
        request.setPayMethod(1);
        return request;
    }

    private AddressBook address() {
        return AddressBook.builder()
                .id(8L)
                .userId(USER_ID)
                .consignee("测试用户")
                .phone("13800000000")
                .provinceName("测试省")
                .cityName("测试市")
                .districtName("测试区")
                .detail("测试路 1 号")
                .build();
    }
}