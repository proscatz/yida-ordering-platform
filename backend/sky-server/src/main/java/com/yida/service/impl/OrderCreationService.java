package com.yida.service.impl;

import com.alibaba.fastjson.JSON;

import com.yida.constant.MessageConstant;
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
import com.yida.mapper.SetmealMapper;
import com.yida.mapper.ShoppingCartMapper;
import com.yida.messaging.OrderCreatedEvent;
import com.yida.messaging.OrderMessagingConstants;
import com.yida.messaging.outbox.OrderOutbox;
import com.yida.service.support.OrderCreationResult;
import com.yida.service.support.OrderNumberGenerator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * 承载一次下单数据库事务：订单、明细和购物车必须一起提交或一起回滚。
 */
@Service
public class OrderCreationService {

    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private OrderDetailMapper orderDetailMapper;
    @Autowired
    private AddressBookMapper addressBookMapper;
    @Autowired
    private ShoppingCartMapper shoppingCartMapper;
    @Autowired
    private DishMapper dishMapper;
    @Autowired
    private SetmealMapper setmealMapper;
    @Autowired
    private OrderNumberGenerator orderNumberGenerator;
    @Autowired
    private OrderOutboxMapper orderOutboxMapper;
    @Value("${yida.order.timeout-millis:900000}")
    private long orderTimeoutMillis = 900_000L;

    @Transactional(rollbackFor = Exception.class)
    public OrderCreationResult create(OrdersSubmitDTO request, Long userId) {
        Orders existing = orderMapper.getByUserIdAndRequestId(userId, request.getRequestId());
        if (existing != null) {
            return OrderCreationResult.existing(existing);
        }

        AddressBook addressBook = addressBookMapper.getByIdAndUserId(request.getAddressBookId(), userId);
        if (addressBook == null) {
            throw new AddressBookBusinessException(MessageConstant.ADDRESS_BOOK_IS_NULL);
        }

        List<ShoppingCart> cartItems = shoppingCartMapper.listByUserIdForUpdate(userId);
        Orders committedWhileWaiting = orderMapper.getByUserIdAndRequestIdForUpdate(userId, request.getRequestId());
        if (committedWhileWaiting != null) {
            return OrderCreationResult.existing(committedWhileWaiting);
        }
        if (cartItems == null || cartItems.isEmpty()) {
            throw new ShoppingCartBusinessException(MessageConstant.SHOPPING_CART_IS_NULL);
        }

        List<ShoppingCart> orderedCartItems = new ArrayList<>(cartItems);
        orderedCartItems.sort(Comparator.comparing(this::productLockKey));

        List<OrderDetail> details = new ArrayList<>(orderedCartItems.size());
        BigDecimal goodsAmount = BigDecimal.ZERO;
        for (ShoppingCart cartItem : orderedCartItems) {
            validateQuantity(cartItem.getNumber());
            PricedItem pricedItem = loadCurrentPricedItem(cartItem);
            goodsAmount = goodsAmount.add(pricedItem.price.multiply(BigDecimal.valueOf(cartItem.getNumber())));
            details.add(OrderDetail.builder()
                    .name(pricedItem.name)
                    .image(pricedItem.image)
                    .dishId(cartItem.getDishId())
                    .setmealId(cartItem.getSetmealId())
                    .dishFlavor(cartItem.getDishFlavor())
                    .number(cartItem.getNumber())
                    .amount(pricedItem.price)
                    .build());
        }

        int packAmount = request.getPackAmount() == null ? 0 : request.getPackAmount();
        if (packAmount < 0) {
            throw new OrderBusinessException(MessageConstant.ORDER_AMOUNT_ERROR);
        }
        BigDecimal orderAmount = goodsAmount.add(BigDecimal.valueOf(packAmount));
        if (orderAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new OrderBusinessException(MessageConstant.ORDER_AMOUNT_ERROR);
        }

        LocalDateTime now = LocalDateTime.now();
        Orders order = Orders.builder()
                .number(orderNumberGenerator.next())
                .requestId(request.getRequestId())
                .status(Orders.PENDING_PAYMENT)
                .userId(userId)
                .addressBookId(request.getAddressBookId())
                .orderTime(now)
                .checkoutTime(now)
                .payMethod(request.getPayMethod())
                .payStatus(Orders.UN_PAID)
                .amount(orderAmount)
                .remark(request.getRemark())
                .phone(addressBook.getPhone())
                .address(buildAddress(addressBook))
                .consignee(addressBook.getConsignee())
                .estimatedDeliveryTime(request.getEstimatedDeliveryTime())
                .deliveryStatus(request.getDeliveryStatus())
                .packAmount(packAmount)
                .tablewareNumber(request.getTablewareNumber() == null ? 0 : request.getTablewareNumber())
                .tablewareStatus(request.getTablewareStatus())
                .build();
        orderMapper.insert(order);

        for (OrderDetail detail : details) {
            detail.setOrderId(order.getId());
        }
        orderDetailMapper.insertBatch(details);
        shoppingCartMapper.deleteByUserId(userId);

        String eventId = UUID.randomUUID().toString();
        OrderCreatedEvent event = OrderCreatedEvent.builder()
                .eventId(eventId)
                .orderId(order.getId())
                .orderNumber(order.getNumber())
                .timeoutAtEpochMilli(System.currentTimeMillis() + orderTimeoutMillis)
                .build();
        OrderOutbox outbox = OrderOutbox.builder()
                .eventId(eventId)
                .eventType(OrderMessagingConstants.ORDER_CREATED_EVENT)
                .aggregateId(order.getId())
                .payload(JSON.toJSONString(event))
                .status(OrderOutbox.PENDING)
                .retryCount(0)
                .nextRetryTime(now)
                .createdTime(now)
                .updatedTime(now)
                .build();
        orderOutboxMapper.insert(outbox);
        return OrderCreationResult.created(order);
    }

    private PricedItem loadCurrentPricedItem(ShoppingCart cartItem) {
        boolean hasDish = cartItem.getDishId() != null;
        boolean hasSetmeal = cartItem.getSetmealId() != null;
        if (hasDish == hasSetmeal) {
            throw new OrderBusinessException(MessageConstant.ORDER_ITEM_NOT_FOUND);
        }

        if (hasDish) {
            Dish dish = dishMapper.getByIdForUpdate(cartItem.getDishId());
            if (dish == null || !StatusConstant.ENABLE.equals(dish.getStatus())) {
                throw new OrderBusinessException(MessageConstant.ORDER_ITEM_NOT_FOUND);
            }
            validatePrice(dish.getPrice());
            return new PricedItem(dish.getName(), dish.getImage(), dish.getPrice());
        }

        Setmeal setmeal = setmealMapper.getByIdForUpdate(cartItem.getSetmealId());
        if (setmeal == null || !StatusConstant.ENABLE.equals(setmeal.getStatus())) {
            throw new OrderBusinessException(MessageConstant.ORDER_ITEM_NOT_FOUND);
        }
        validatePrice(setmeal.getPrice());
        return new PricedItem(setmeal.getName(), setmeal.getImage(), setmeal.getPrice());
    }

    private void validateQuantity(Integer quantity) {
        if (quantity == null || quantity <= 0) {
            throw new OrderBusinessException(MessageConstant.ORDER_AMOUNT_ERROR);
        }
    }

    private void validatePrice(BigDecimal price) {
        if (price == null || price.compareTo(BigDecimal.ZERO) <= 0) {
            throw new OrderBusinessException(MessageConstant.ORDER_AMOUNT_ERROR);
        }
    }

    private String productLockKey(ShoppingCart cartItem) {
        if (cartItem.getDishId() != null) {
            return "D:" + cartItem.getDishId();
        }
        if (cartItem.getSetmealId() != null) {
            return "S:" + cartItem.getSetmealId();
        }
        return "Z:" + cartItem.getId();
    }

    private String buildAddress(AddressBook addressBook) {
        return valueOrEmpty(addressBook.getProvinceName())
                + valueOrEmpty(addressBook.getCityName())
                + valueOrEmpty(addressBook.getDistrictName())
                + valueOrEmpty(addressBook.getDetail());
    }

    private String valueOrEmpty(String value) {
        return value == null ? "" : value;
    }

    private static class PricedItem {
        private final String name;
        private final String image;
        private final BigDecimal price;

        private PricedItem(String name, String image, BigDecimal price) {
            this.name = name;
            this.image = image;
            this.price = price;
        }
    }
}