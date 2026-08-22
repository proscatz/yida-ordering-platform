package com.yida.payment;
import org.apache.ibatis.annotations.*;
@Mapper
public interface PaymentRecordMapper {
    @Select("select * from payment_callback_record where provider = #{provider} and event_id = #{eventId}")
    PaymentCallbackRecord getCallback(@Param("provider") String provider, @Param("eventId") String eventId);
    @Insert("insert ignore into payment_callback_record(provider,event_id,order_number,transaction_id,amount_cent,raw_hash,create_time) values(#{provider},#{eventId},#{orderNumber},#{transactionId},#{amountCent},#{rawHash},#{createTime})")
    int insertCallback(PaymentCallbackRecord record);
    @Insert("insert ignore into payment_refund_record(provider,order_number,refund_number,amount_cent,create_time) values(#{provider},#{orderNumber},#{refundNumber},#{amountCent},now())")
    int insertRefund(@Param("provider") String provider, @Param("orderNumber") String orderNumber,
                     @Param("refundNumber") String refundNumber, @Param("amountCent") long amountCent);
}