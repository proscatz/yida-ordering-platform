package com.yida.service.impl;

import com.yida.context.BaseContext;
import com.yida.dto.ShoppingCartDTO;
import com.yida.entity.Dish;
import com.yida.entity.ShoppingCart;
import com.yida.mapper.DishMapper;
import com.yida.mapper.SetmealMapper;
import com.yida.mapper.ShoppingCartMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ShoppingCartServiceImplTest {

    private static final long USER_ID = 42L;

    @Mock
    private ShoppingCartMapper shoppingCartMapper;
    @Mock
    private DishMapper dishMapper;
    @Mock
    private SetmealMapper setmealMapper;

    @InjectMocks
    private ShoppingCartServiceImpl shoppingCartService;

    @BeforeEach
    void setCurrentUser() {
        BaseContext.setCurrentId(USER_ID);
    }

    @AfterEach
    void clearCurrentUser() {
        BaseContext.removeCurrentId();
    }

    @Test
    void addIncrementsExistingCartItem() {
        ShoppingCartDTO request = dishRequest(10L);
        ShoppingCart existing = ShoppingCart.builder().id(7L).number(2).build();
        when(shoppingCartMapper.list(any(ShoppingCart.class))).thenReturn(Collections.singletonList(existing));

        shoppingCartService.addShoppingCart(request);

        ArgumentCaptor<ShoppingCart> captor = ArgumentCaptor.forClass(ShoppingCart.class);
        verify(shoppingCartMapper).updateNumberById(captor.capture());
        assertEquals(3, captor.getValue().getNumber());
        verify(shoppingCartMapper, never()).insert(any());
    }

    @Test
    void addCreatesNewDishCartItemFromServerSideDishData() {
        ShoppingCartDTO request = dishRequest(10L);
        Dish dish = Dish.builder()
                .id(10L)
                .name("测试菜品")
                .image("dish.png")
                .price(new BigDecimal("12.50"))
                .build();
        when(shoppingCartMapper.list(any(ShoppingCart.class))).thenReturn(Collections.emptyList());
        when(dishMapper.getById(10L)).thenReturn(dish);

        shoppingCartService.addShoppingCart(request);

        ArgumentCaptor<ShoppingCart> captor = ArgumentCaptor.forClass(ShoppingCart.class);
        verify(shoppingCartMapper).insert(captor.capture());
        ShoppingCart inserted = captor.getValue();
        assertEquals(USER_ID, inserted.getUserId());
        assertEquals(10L, inserted.getDishId());
        assertEquals("测试菜品", inserted.getName());
        assertEquals(new BigDecimal("12.50"), inserted.getAmount());
        assertEquals(1, inserted.getNumber());
        assertNotNull(inserted.getCreateTime());
    }

    @Test
    void subDeletesItemWhenQuantityIsOne() {
        ShoppingCartDTO request = dishRequest(10L);
        ShoppingCart existing = ShoppingCart.builder().id(7L).number(1).build();
        when(shoppingCartMapper.list(any(ShoppingCart.class))).thenReturn(Collections.singletonList(existing));

        shoppingCartService.sub(request);

        verify(shoppingCartMapper).deleteById(7L);
        verify(shoppingCartMapper, never()).updateNumberById(any());
    }

    @Test
    void subDecrementsItemWhenQuantityIsGreaterThanOne() {
        ShoppingCartDTO request = dishRequest(10L);
        ShoppingCart existing = ShoppingCart.builder().id(7L).number(2).build();
        when(shoppingCartMapper.list(any(ShoppingCart.class))).thenReturn(Collections.singletonList(existing));

        shoppingCartService.sub(request);

        ArgumentCaptor<ShoppingCart> captor = ArgumentCaptor.forClass(ShoppingCart.class);
        verify(shoppingCartMapper).updateNumberById(captor.capture());
        assertEquals(1, captor.getValue().getNumber());
        verify(shoppingCartMapper, never()).deleteById(any());
    }

    @Test
    void cleanDeletesOnlyCurrentUsersCart() {
        shoppingCartService.clean();

        verify(shoppingCartMapper).deleteByUserId(USER_ID);
    }

    private ShoppingCartDTO dishRequest(Long dishId) {
        ShoppingCartDTO request = new ShoppingCartDTO();
        request.setDishId(dishId);
        request.setDishFlavor("微辣");
        return request;
    }
}