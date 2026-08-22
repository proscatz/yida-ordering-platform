package com.yida.service.impl;

import com.alibaba.fastjson.JSON;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.github.xiaoymin.knife4j.core.util.CollectionUtils;
import com.yida.constant.MessageConstant;
import com.yida.context.BaseContext;
import com.yida.dto.OrdersCancelDTO;
import com.yida.dto.OrdersConfirmDTO;
import com.yida.dto.OrdersPageQueryDTO;
import com.yida.dto.OrdersPaymentDTO;
import com.yida.dto.OrdersRejectionDTO;
import com.yida.dto.OrdersSubmitDTO;
import com.yida.entity.OrderDetail;
import com.yida.entity.Orders;
import com.yida.entity.ShoppingCart;
import com.yida.exception.OrderBusinessException;
import com.yida.mapper.OrderDetailMapper;
import com.yida.mapper.OrderMapper;
import com.yida.mapper.ShoppingCartMapper;
import com.yida.mapper.UserMapper;
import com.yida.entity.User;
import com.yida.payment.PaymentGateway;
import com.yida.payment.PaymentRequest;
import com.yida.payment.PaymentRefundService;
import com.yida.result.PageResult;
import com.yida.service.OrderService;
import com.yida.service.support.OrderCreationResult;
import com.yida.service.support.OrderStateMachine;
import com.yida.vo.OrderPaymentVO;
import com.yida.vo.OrderStatisticsVO;
import com.yida.vo.OrderSubmitVO;
import com.yida.vo.OrderVO;
import com.yida.websocket.WebSocketServer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
public class OrderServiceImpl implements OrderService {

    private static final int ORDER_NUMBER_RETRY_LIMIT = 3;
    private static final int MAX_REQUEST_ID_LENGTH = 64;

    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private OrderDetailMapper orderDetailMapper;
    @Autowired
    private ShoppingCartMapper shoppingCartMapper;
    @Autowired
    private WebSocketServer webSocketServer;
    @Autowired
    private OrderCreationService orderCreationService;
    @Autowired
    private PaymentGateway paymentGateway;
    @Autowired
    private PaymentRefundService paymentRefundService;
    @Autowired
    private UserMapper userMapper;

    @Override
    public OrderSubmitVO submit(OrdersSubmitDTO request) {
        Long userId = BaseContext.getCurrentId();
        String requestId = normalizeRequestId(request.getRequestId());
        request.setRequestId(requestId);

        for (int attempt = 1; attempt <= ORDER_NUMBER_RETRY_LIMIT; attempt++) {
            try {
                OrderCreationResult result = orderCreationService.create(request, userId);
                if (result.isCreated()) {
                    sendNewOrderMessage(result.getOrder());
                }
                return toSubmitVO(result.getOrder());
            } catch (DuplicateKeyException ex) {
                Orders existing = orderMapper.getByUserIdAndRequestId(userId, requestId);
                if (existing != null) {
                    return toSubmitVO(existing);
                }
                if (attempt == ORDER_NUMBER_RETRY_LIMIT) {
                    throw new OrderBusinessException(MessageConstant.ORDER_SUBMIT_CONFLICT);
                }
            }
        }
        throw new OrderBusinessException(MessageConstant.ORDER_SUBMIT_CONFLICT);
    }

    @Override
    public OrderPaymentVO payment(OrdersPaymentDTO request) {
        Long userId = BaseContext.getCurrentId();
        Orders order = orderMapper.getByNumberAndUserId(request.getOrderNumber(), userId);
        if (order == null) throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        if (!Orders.PENDING_PAYMENT.equals(order.getStatus()) || !Orders.UN_PAID.equals(order.getPayStatus()))
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        if (order.getAmount() == null || order.getAmount().signum() <= 0)
            throw new OrderBusinessException(MessageConstant.ORDER_AMOUNT_ERROR);
        if (request.getPayMethod() != null && !request.getPayMethod().equals(order.getPayMethod()))
            throw new OrderBusinessException(MessageConstant.PAYMENT_METHOD_MISMATCH);
        User user = userMapper.getById(userId);
        if (user == null) throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        OrderPaymentVO result = paymentGateway.createPayment(PaymentRequest.builder().orderNumber(order.getNumber())
                .amount(order.getAmount()).description("驿达点餐订单").openid(user.getOpenid()).build());
        if (paymentGateway.completesSynchronously()) paySuccess(order.getNumber());
        return result;
    }

    @Override
    public void paySuccess(String outTradeNo) {
        Orders ordersDB = orderMapper.getByNumber(outTradeNo);
        if (ordersDB == null) {
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }
        if (Orders.PAID.equals(ordersDB.getPayStatus())) {
            return;
        }

        OrderStateMachine.requireTransition(ordersDB.getStatus(), Orders.TO_BE_CONFIRMED);
        Orders update = Orders.builder()
                .id(ordersDB.getId())
                .status(Orders.TO_BE_CONFIRMED)
                .payStatus(Orders.PAID)
                .checkoutTime(LocalDateTime.now())
                .build();
        int affectedRows = orderMapper.updateByIdAndStatus(update, ordersDB.getStatus());
        if (affectedRows == 1) {
            return;
        }

        Orders latest = orderMapper.getByNumber(outTradeNo);
        if (latest != null
                && Orders.PAID.equals(latest.getPayStatus())
                && Orders.TO_BE_CONFIRMED.equals(latest.getStatus())) {
            return;
        }
        throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
    }

    @Override
    public PageResult pageQuery4User(int pageNum, int pageSize, Integer status) {
        PageHelper.startPage(pageNum, pageSize);
        OrdersPageQueryDTO query = new OrdersPageQueryDTO();
        query.setUserId(BaseContext.getCurrentId());
        query.setStatus(status);
        Page<Orders> page = orderMapper.pageQuery(query);

        List<OrderVO> list = new ArrayList<>();
        if (page != null && page.getTotal() > 0) {
            for (Orders orders : page) {
                list.add(buildOrderVO(orders));
            }
        }
        return new PageResult(page == null ? 0 : page.getTotal(), list);
    }

    @Override
    public OrderVO details(Long id) {
        Orders orders = orderMapper.getById(id);
        if (orders == null) {
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }
        return buildOrderVO(orders);
    }

    @Override
    public OrderVO userDetails(Long id) {
        return buildOrderVO(getUserOrder(id));
    }

    @Override
    public void userCancelById(Long id) {
        Long userId = BaseContext.getCurrentId();
        Orders ordersDB = getUserOrder(id);
        if (!Orders.PENDING_PAYMENT.equals(ordersDB.getStatus())
                && !Orders.TO_BE_CONFIRMED.equals(ordersDB.getStatus())) {
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }

        refundIfPaid(ordersDB);
        Orders update = cancellationUpdate(ordersDB, "用户取消", null);
        updateOwnedState(ordersDB, update, userId);
    }

    @Override
    public void repetition(Long id) {
        Long userId = BaseContext.getCurrentId();
        getUserOrder(id);
        List<OrderDetail> orderDetailList = orderDetailMapper.getByOrderId(id);
        List<ShoppingCart> shoppingCartList = orderDetailList.stream().map(detail -> {
            ShoppingCart shoppingCart = new ShoppingCart();
            BeanUtils.copyProperties(detail, shoppingCart, "id");
            shoppingCart.setUserId(userId);
            shoppingCart.setCreateTime(LocalDateTime.now());
            return shoppingCart;
        }).collect(Collectors.toList());
        if (!shoppingCartList.isEmpty()) {
            shoppingCartMapper.insertBatch(shoppingCartList);
        }
    }

    @Override
    public PageResult conditionSearch(OrdersPageQueryDTO ordersPageQueryDTO) {
        PageHelper.startPage(ordersPageQueryDTO.getPage(), ordersPageQueryDTO.getPageSize());
        Page<Orders> page = orderMapper.pageQuery(ordersPageQueryDTO);
        return new PageResult(page.getTotal(), getOrderVOList(page));
    }

    private List<OrderVO> getOrderVOList(Page<Orders> page) {
        List<OrderVO> orderVOList = new ArrayList<>();
        List<Orders> ordersList = page.getResult();
        if (!CollectionUtils.isEmpty(ordersList)) {
            for (Orders orders : ordersList) {
                OrderVO orderVO = new OrderVO();
                BeanUtils.copyProperties(orders, orderVO);
                orderVO.setOrderDishes(getOrderDishesStr(orders));
                orderVOList.add(orderVO);
            }
        }
        return orderVOList;
    }

    private String getOrderDishesStr(Orders orders) {
        List<OrderDetail> orderDetailList = orderDetailMapper.getByOrderId(orders.getId());
        List<String> orderDishList = orderDetailList.stream()
                .map(detail -> detail.getName() + "*" + detail.getNumber() + ";")
                .collect(Collectors.toList());
        return String.join("", orderDishList);
    }

    @Override
    public OrderStatisticsVO statistics() {
        OrderStatisticsVO statistics = new OrderStatisticsVO();
        statistics.setToBeConfirmed(orderMapper.countStatus(Orders.TO_BE_CONFIRMED));
        statistics.setConfirmed(orderMapper.countStatus(Orders.CONFIRMED));
        statistics.setDeliveryInProgress(orderMapper.countStatus(Orders.DELIVERY_IN_PROGRESS));
        return statistics;
    }

    @Override
    public void confirm(OrdersConfirmDTO request) {
        Orders ordersDB = requireOrder(request.getId());
        Orders update = Orders.builder()
                .id(ordersDB.getId())
                .status(Orders.CONFIRMED)
                .build();
        updateState(ordersDB, update);
    }

    @Override
    public void rejection(OrdersRejectionDTO request) {
        Orders ordersDB = requireOrder(request.getId());
        refundIfPaid(ordersDB);
        Orders update = cancellationUpdate(ordersDB, null, request.getRejectionReason());
        updateState(ordersDB, update);
    }

    @Override
    public void cancel(OrdersCancelDTO request) {
        Orders ordersDB = requireOrder(request.getId());
        refundIfPaid(ordersDB);
        Orders update = cancellationUpdate(ordersDB, request.getCancelReason(), null);
        updateState(ordersDB, update);
    }

    @Override
    public void delivery(Long id) {
        Orders ordersDB = requireOrder(id);
        Orders update = Orders.builder()
                .id(ordersDB.getId())
                .status(Orders.DELIVERY_IN_PROGRESS)
                .build();
        updateState(ordersDB, update);
    }

    @Override
    public void complete(Long id) {
        Orders ordersDB = requireOrder(id);
        Orders update = Orders.builder()
                .id(ordersDB.getId())
                .status(Orders.COMPLETED)
                .deliveryTime(LocalDateTime.now())
                .build();
        updateState(ordersDB, update);
    }

    @Override
    public void reminder(Long id) {
        Orders order = getUserOrder(id);
        Map<String, Object> message = new HashMap<>();
        message.put("type", 2);
        message.put("orderId", id);
        message.put("status", order.getStatus());
        webSocketServer.sendToAllClient(JSON.toJSONString(message));
    }

    private void updateState(Orders current, Orders update) {
        OrderStateMachine.requireTransition(current.getStatus(), update.getStatus());
        int affectedRows = orderMapper.updateByIdAndStatus(update, current.getStatus());
        requireSingleRowUpdated(affectedRows);
    }

    private void updateOwnedState(Orders current, Orders update, Long userId) {
        OrderStateMachine.requireTransition(current.getStatus(), update.getStatus());
        int affectedRows = orderMapper.updateByIdAndUserIdAndStatus(update, userId, current.getStatus());
        requireSingleRowUpdated(affectedRows);
    }

    private void requireSingleRowUpdated(int affectedRows) {
        if (affectedRows != 1) {
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }
    }

    private void refundIfPaid(Orders order) {
        if (Orders.PAID.equals(order.getPayStatus())) paymentRefundService.refundLocalOrder(order);
    }

    private Orders cancellationUpdate(Orders current, String cancelReason, String rejectionReason) {
        Orders update = Orders.builder()
                .id(current.getId())
                .status(Orders.CANCELLED)
                .cancelReason(cancelReason)
                .rejectionReason(rejectionReason)
                .cancelTime(LocalDateTime.now())
                .build();
        if (Orders.PAID.equals(current.getPayStatus())) {
            update.setPayStatus(Orders.REFUND);
        }
        return update;
    }

    private Orders requireOrder(Long id) {
        Orders order = orderMapper.getById(id);
        if (order == null) {
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }
        return order;
    }

    private Orders getUserOrder(Long id) {
        Orders order = orderMapper.getByIdAndUserId(id, BaseContext.getCurrentId());
        if (order == null) {
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }
        return order;
    }

    private OrderVO buildOrderVO(Orders orders) {
        List<OrderDetail> orderDetailList = orderDetailMapper.getByOrderId(orders.getId());
        OrderVO orderVO = new OrderVO();
        BeanUtils.copyProperties(orders, orderVO);
        orderVO.setOrderDetailList(orderDetailList);
        return orderVO;
    }

    private String normalizeRequestId(String requestId) {
        if (requestId == null) {
            throw new OrderBusinessException(MessageConstant.ORDER_REQUEST_ID_REQUIRED);
        }
        String normalized = requestId.trim();
        if (normalized.isEmpty() || normalized.length() > MAX_REQUEST_ID_LENGTH) {
            throw new OrderBusinessException(MessageConstant.ORDER_REQUEST_ID_REQUIRED);
        }
        return normalized;
    }

    private OrderSubmitVO toSubmitVO(Orders order) {
        return OrderSubmitVO.builder()
                .id(order.getId())
                .orderAmount(order.getAmount())
                .orderTime(order.getOrderTime())
                .build();
    }

    private void sendNewOrderMessage(Orders order) {
        Map<String, Object> message = new HashMap<>();
        message.put("type", 1);
        message.put("orderId", order.getId());
        message.put("content", "订单号：" + order.getNumber());
        webSocketServer.sendToAllClient(JSON.toJSONString(message));
    }
}