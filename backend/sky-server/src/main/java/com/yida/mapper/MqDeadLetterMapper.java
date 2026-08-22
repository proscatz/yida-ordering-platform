package com.yida.mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;

@Mapper
public interface MqDeadLetterMapper {
    @Insert("insert ignore into mq_dead_letter_record(event_id,queue_name,payload,created_time) " +
            "values(#{eventId},#{queueName},#{payload},#{createdTime})")
    int insertIgnore(@Param("eventId") String eventId, @Param("queueName") String queueName,
                     @Param("payload") String payload, @Param("createdTime") LocalDateTime createdTime);
}