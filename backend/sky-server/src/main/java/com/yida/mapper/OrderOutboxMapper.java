package com.yida.mapper;

import com.yida.messaging.outbox.OrderOutbox;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface OrderOutboxMapper {
    @Insert("insert into order_outbox(event_id,event_type,aggregate_id,payload,status,retry_count,next_retry_time,created_time,updated_time) " +
            "values(#{eventId},#{eventType},#{aggregateId},#{payload},#{status},#{retryCount},#{nextRetryTime},#{createdTime},#{updatedTime})")
    int insert(OrderOutbox outbox);

    @Select("select * from order_outbox where status in ('PENDING','FAILED') and next_retry_time <= #{now} " +
            "order by id limit #{limit}")
    List<OrderOutbox> findReady(@Param("now") LocalDateTime now, @Param("limit") int limit);

    @Update("update order_outbox set status='SENDING',updated_time=#{now} where id=#{id} and status in ('PENDING','FAILED')")
    int markSending(@Param("id") Long id, @Param("now") LocalDateTime now);

    @Update("update order_outbox set status='SENT',sent_time=#{now},updated_time=#{now},last_error=null where id=#{id} and status='SENDING'")
    int markSent(@Param("id") Long id, @Param("now") LocalDateTime now);

    @Update("update order_outbox set status='FAILED',retry_count=retry_count+1,next_retry_time=#{nextRetryTime}," +
            "updated_time=#{now},last_error=#{lastError} where id=#{id} and status='SENDING'")
    int markFailed(@Param("id") Long id, @Param("nextRetryTime") LocalDateTime nextRetryTime,
                   @Param("now") LocalDateTime now, @Param("lastError") String lastError);

    @Update("update order_outbox set status='FAILED',next_retry_time=#{now},updated_time=#{now}," +
            "last_error='发送进程中断，重新投递' where status='SENDING' and updated_time < #{staleBefore}")
    int recoverStale(@Param("staleBefore") LocalDateTime staleBefore, @Param("now") LocalDateTime now);
}