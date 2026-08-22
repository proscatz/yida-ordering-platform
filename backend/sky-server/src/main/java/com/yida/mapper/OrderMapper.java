package com.yida.mapper;

import com.github.pagehelper.Page;
import com.yida.dto.GoodsSalesDTO;
import com.yida.dto.OrdersPageQueryDTO;
import com.yida.entity.Orders;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Mapper
public interface OrderMapper {
    int insert(Orders order);

    @Select("select * from orders where number = #{orderNumber}")
    Orders getByNumber(String orderNumber);

    @Select("select * from orders where number = #{orderNumber} for update")
    Orders getByNumberForUpdate(String orderNumber);

    @Select("select * from orders where number = #{orderNumber} and user_id = #{userId}")
    Orders getByNumberAndUserId(@Param("orderNumber") String orderNumber, @Param("userId") Long userId);

    @Select("select * from orders where user_id = #{userId} and request_id = #{requestId}")
    Orders getByUserIdAndRequestId(@Param("userId") Long userId, @Param("requestId") String requestId);

    @Select("select * from orders where user_id = #{userId} and request_id = #{requestId} for update")
    Orders getByUserIdAndRequestIdForUpdate(@Param("userId") Long userId, @Param("requestId") String requestId);

    int updateByIdAndStatus(@Param("orders") Orders orders, @Param("expectedStatus") Integer expectedStatus);

    int updateByIdAndUserIdAndStatus(@Param("orders") Orders orders,
                                     @Param("userId") Long userId,
                                     @Param("expectedStatus") Integer expectedStatus);

    @Select("select * from orders where status = #{status} and order_time < #{orderTime}")
    List<Orders> getByStatusAndOrderTime(Integer status, LocalDateTime orderTime);

    Page<Orders> pageQuery(OrdersPageQueryDTO ordersPageQueryDTO);

    @Select("select * from orders where id = #{id}")
    Orders getById(Long id);

    @Select("select * from orders where id = #{id} and user_id = #{userId}")
    Orders getByIdAndUserId(@Param("id") Long id, @Param("userId") Long userId);

    @Select("select count(id) from orders where status = #{status}")
    Integer countStatus(Integer status);

    Double sumByMap(Map map);

    Integer countByMap(Map<String, Object> map);

    List<GoodsSalesDTO> getSalesTop10(LocalDateTime begin, LocalDateTime end);
}