package com.yida.mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;

@Mapper
public interface MessageConsumeMapper {
    @Insert("insert ignore into message_consume_record(consumer_name,event_id,order_id,consumed_time) " +
            "values(#{consumerName},#{eventId},#{orderId},#{consumedTime})")
    int insertIgnore(@Param("consumerName") String consumerName, @Param("eventId") String eventId,
                     @Param("orderId") Long orderId, @Param("consumedTime") LocalDateTime consumedTime);
}