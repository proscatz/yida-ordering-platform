package com.yida.service;
import com.yida.dto.ShoppingCartDTO;
import com.yida.entity.ShoppingCart;

import java.util.List;

public interface ShoppingCartService {
    void addShoppingCart(ShoppingCartDTO shoppingCartDTO);

    List<ShoppingCart> list();

    void sub(ShoppingCartDTO shoppingCartDTO);

    void clean();
}