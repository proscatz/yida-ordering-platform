package com.yida.service.impl;

import com.yida.context.BaseContext;
import com.yida.dto.OrdersConfirmDTO;
import com.yida.dto.OrdersPaymentDTO;
import com.yida.dto.OrdersSubmitDTO;
import com.yida.entity.Orders;
import com.yida.entity.User;
import com.yida.exception.OrderBusinessException;
import com.yida.mapper.OrderDetailMapper;
import com.yida.mapper.OrderMapper;
import com.yida.mapper.ShoppingCartMapper;
import com.yida.mapper.UserMapper;
import com.yida.payment.PaymentGateway;
import com.yida.payment.PaymentRequest;
import com.yida.payment.PaymentRefundService;
import com.yida.service.support.OrderCreationResult;
import com.yida.vo.OrderSubmitVO;
import com.yida.vo.OrderPaymentVO;
import com.yida.websocket.WebSocketServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceImplTest {

    private static final long USER_ID = 42L;

    @Mock
    private OrderMapper orderMapper;
    @Mock
    private OrderDetailMapper orderDetailMapper;
    @Mock
    private ShoppingCartMapper shoppingCartMapper;
    @Mock
    private WebSocketServer webSocketServer;
    @Mock
    private OrderCreationService orderCreationService;
    @Mock
    private PaymentGateway paymentGateway;
    @Mock
    private PaymentRefundService paymentRefundService;
    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private OrderServiceImpl orderService;

    @BeforeEach
    void setCurrentUser() {
        BaseContext.setCurrentId(USER_ID);
    }

    @AfterEach
    void clearCurrentUser() {
        BaseContext.removeCurrentId();
    }

    @Test
    void submitRequiresIdempotencyRequestId() {
        OrdersSubmitDTO request = new OrdersSubmitDTO();

        assertThrows(OrderBusinessException.class, () -> orderService.submit(request));

        verify(orderCreationService, never()).create(any(), any());
    }

    @Test
    void submitCreatesOnceAndNotifiesAfterTransactionReturns() {
        OrdersSubmitDTO request = submitRequest();
        Orders created = order(99L, Orders.TO_BE_CONFIRMED, Orders.PAID);
        created.setNumber("order-99");
        created.setAmount(new BigDecimal("25.00"));
        created.setOrderTime(LocalDateTime.now());
        when(orderCreationService.create(request, USER_ID)).thenReturn(OrderCreationResult.created(created));

        OrderSubmitVO result = orderService.submit(request);

        assertEquals(99L, result.getId());
        assertEquals(new BigDecimal("25.00"), result.getOrderAmount());
        verify(webSocketServer).sendToAllClient(any());
    }

    @Test
    void repeatedRequestReturnsExistingOrderWithoutDuplicateNotification() {
        OrdersSubmitDTO request = submitRequest();
        Orders existing = order(99L, Orders.TO_BE_CONFIRMED, Orders.PAID);
        existing.setAmount(new BigDecimal("25.00"));
        when(orderCreationService.create(request, USER_ID)).thenReturn(OrderCreationResult.existing(existing));

        OrderSubmitVO result = orderService.submit(request);

        assertEquals(99L, result.getId());
        verify(webSocketServer, never()).sendToAllClient(any());
    }

    @Test
    void concurrentDuplicateRequestReturnsCommittedExistingOrder() {
        OrdersSubmitDTO request = submitRequest();
        Orders existing = order(99L, Orders.TO_BE_CONFIRMED, Orders.PAID);
        existing.setAmount(new BigDecimal("25.00"));
        when(orderCreationService.create(request, USER_ID))
                .thenThrow(new DuplicateKeyException("duplicate request"));
        when(orderMapper.getByUserIdAndRequestId(USER_ID, "request-1")).thenReturn(existing);

        OrderSubmitVO result = orderService.submit(request);

        assertEquals(99L, result.getId());
        verify(webSocketServer, never()).sendToAllClient(any());
    }

    @Test
    void orderNumberCollisionRetriesWithANewTransaction() {
        OrdersSubmitDTO request = submitRequest();
        Orders created = order(100L, Orders.TO_BE_CONFIRMED, Orders.PAID);
        created.setAmount(BigDecimal.TEN);
        when(orderCreationService.create(request, USER_ID))
                .thenThrow(new DuplicateKeyException("duplicate order number"))
                .thenReturn(OrderCreationResult.created(created));
        when(orderMapper.getByUserIdAndRequestId(USER_ID, "request-1")).thenReturn(null);

        OrderSubmitVO result = orderService.submit(request);

        assertEquals(100L, result.getId());
        verify(orderCreationService, times(2)).create(request, USER_ID);
        verify(webSocketServer).sendToAllClient(any());
    }

    @Test
    void userDetailsRejectsOrderOwnedByAnotherUser() {
        when(orderMapper.getByIdAndUserId(100L, USER_ID)).thenReturn(null);

        assertThrows(OrderBusinessException.class, () -> orderService.userDetails(100L));

        verify(orderDetailMapper, never()).getByOrderId(any());
    }

    @Test
    void repetitionRejectsOrderOwnedByAnotherUser() {
        when(orderMapper.getByIdAndUserId(100L, USER_ID)).thenReturn(null);

        assertThrows(OrderBusinessException.class, () -> orderService.repetition(100L));

        verify(shoppingCartMapper, never()).insertBatch(any());
    }

    @Test
    void reminderRejectsOrderOwnedByAnotherUser() {
        when(orderMapper.getByIdAndUserId(100L, USER_ID)).thenReturn(null);

        assertThrows(OrderBusinessException.class, () -> orderService.reminder(100L));

        verify(webSocketServer, never()).sendToAllClient(any());
    }

    @Test
    void confirmMovesOrderFromWaitingToConfirmedAtomically() {
        OrdersConfirmDTO request = new OrdersConfirmDTO();
        request.setId(100L);
        when(orderMapper.getById(100L)).thenReturn(order(100L, Orders.TO_BE_CONFIRMED, Orders.PAID));
        when(orderMapper.updateByIdAndStatus(any(), eq(Orders.TO_BE_CONFIRMED))).thenReturn(1);

        orderService.confirm(request);

        assertAtomicStatusUpdate(Orders.TO_BE_CONFIRMED, Orders.CONFIRMED, false);
    }

    @Test
    void deliveryMovesConfirmedOrderAtomically() {
        when(orderMapper.getById(100L)).thenReturn(order(100L, Orders.CONFIRMED, Orders.PAID));
        when(orderMapper.updateByIdAndStatus(any(), eq(Orders.CONFIRMED))).thenReturn(1);

        orderService.delivery(100L);

        assertAtomicStatusUpdate(Orders.CONFIRMED, Orders.DELIVERY_IN_PROGRESS, false);
    }

    @Test
    void completeMovesDeliveringOrderAtomically() {
        when(orderMapper.getById(100L)).thenReturn(order(100L, Orders.DELIVERY_IN_PROGRESS, Orders.PAID));
        when(orderMapper.updateByIdAndStatus(any(), eq(Orders.DELIVERY_IN_PROGRESS))).thenReturn(1);

        orderService.complete(100L);

        assertAtomicStatusUpdate(Orders.DELIVERY_IN_PROGRESS, Orders.COMPLETED, true);
    }

    @Test
    void staleStatusUpdateIsRejectedWhenNoRowMatchesExpectedStatus() {
        when(orderMapper.getById(100L)).thenReturn(order(100L, Orders.CONFIRMED, Orders.PAID));
        when(orderMapper.updateByIdAndStatus(any(), eq(Orders.CONFIRMED))).thenReturn(0);

        assertThrows(OrderBusinessException.class, () -> orderService.delivery(100L));
    }

    @Test
    void invalidStateMachineTransitionIsRejectedBeforeUpdate() {
        when(orderMapper.getById(100L)).thenReturn(order(100L, Orders.TO_BE_CONFIRMED, Orders.PAID));

        assertThrows(OrderBusinessException.class, () -> orderService.delivery(100L));

        verify(orderMapper, never()).updateByIdAndStatus(any(), any());
    }

    @Test
    void userCancelUsesOwnershipAndExpectedStatusInUpdate() {
        when(orderMapper.getByIdAndUserId(100L, USER_ID))
                .thenReturn(order(100L, Orders.TO_BE_CONFIRMED, Orders.PAID));
        when(orderMapper.updateByIdAndUserIdAndStatus(any(), eq(USER_ID), eq(Orders.TO_BE_CONFIRMED)))
                .thenReturn(1);

        orderService.userCancelById(100L);

        ArgumentCaptor<Orders> captor = ArgumentCaptor.forClass(Orders.class);
        verify(orderMapper).updateByIdAndUserIdAndStatus(
                captor.capture(), eq(USER_ID), eq(Orders.TO_BE_CONFIRMED));
        Orders update = captor.getValue();
        assertEquals(Orders.CANCELLED, update.getStatus());
        assertEquals(Orders.REFUND, update.getPayStatus());
        assertEquals("用户取消", update.getCancelReason());
        assertNotNull(update.getCancelTime());
    }

    @Test
    void mockPaymentUsesOwnedPendingOrderAndLocalAmount() {
        Orders order = order(100L, Orders.PENDING_PAYMENT, Orders.UN_PAID);
        order.setNumber("order-100"); order.setAmount(new BigDecimal("12.34")); order.setPayMethod(1);
        OrdersPaymentDTO request = new OrdersPaymentDTO(); request.setOrderNumber("order-100"); request.setPayMethod(1);
        OrderPaymentVO gatewayResult = OrderPaymentVO.builder().signType("MOCK").build();
        when(orderMapper.getByNumberAndUserId("order-100", USER_ID)).thenReturn(order);
        when(userMapper.getById(USER_ID)).thenReturn(User.builder().id(USER_ID).build());
        when(paymentGateway.createPayment(any())).thenReturn(gatewayResult);
        when(paymentGateway.completesSynchronously()).thenReturn(true);
        when(orderMapper.getByNumber("order-100")).thenReturn(order);
        when(orderMapper.updateByIdAndStatus(any(), eq(Orders.PENDING_PAYMENT))).thenReturn(1);

        assertEquals(gatewayResult, orderService.payment(request));

        ArgumentCaptor<PaymentRequest> captor = ArgumentCaptor.forClass(PaymentRequest.class);
        verify(paymentGateway).createPayment(captor.capture());
        assertEquals(new BigDecimal("12.34"), captor.getValue().getAmount());
        verify(orderMapper).updateByIdAndStatus(any(), eq(Orders.PENDING_PAYMENT));
    }
    @Test
    void alreadyPaidCallbackIsIdempotent() {
        when(orderMapper.getByNumber("order-100"))
                .thenReturn(order(100L, Orders.TO_BE_CONFIRMED, Orders.PAID));

        orderService.paySuccess("order-100");

        verify(orderMapper, never()).updateByIdAndStatus(any(), any());
    }

    @Test
    void duplicateCallbacksWithStaleReadsUpdateOnlyOnceAndReturnSuccess() {
        Orders pending = order(100L, Orders.PENDING_PAYMENT, Orders.UN_PAID);
        Orders paid = order(100L, Orders.TO_BE_CONFIRMED, Orders.PAID);
        when(orderMapper.getByNumber("order-100")).thenReturn(pending, pending, paid);
        when(orderMapper.updateByIdAndStatus(any(), eq(Orders.PENDING_PAYMENT))).thenReturn(1, 0);

        orderService.paySuccess("order-100");
        orderService.paySuccess("order-100");

        verify(orderMapper, times(2)).updateByIdAndStatus(any(), eq(Orders.PENDING_PAYMENT));
    }

    private void assertAtomicStatusUpdate(Integer expectedStatus, Integer targetStatus, boolean deliveryTimeExpected) {
        ArgumentCaptor<Orders> captor = ArgumentCaptor.forClass(Orders.class);
        verify(orderMapper).updateByIdAndStatus(captor.capture(), eq(expectedStatus));
        assertEquals(targetStatus, captor.getValue().getStatus());
        if (deliveryTimeExpected) {
            assertNotNull(captor.getValue().getDeliveryTime());
        }
    }

    private OrdersSubmitDTO submitRequest() {
        OrdersSubmitDTO request = new OrdersSubmitDTO();
        request.setRequestId("request-1");
        request.setAddressBookId(8L);
        return request;
    }

    private Orders order(Long id, Integer status, Integer payStatus) {
        return Orders.builder()
                .id(id)
                .status(status)
                .payStatus(payStatus)
                .userId(USER_ID)
                .build();
    }
}