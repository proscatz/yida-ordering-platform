package com.yida.controller.admin;

import com.yida.result.Result;
import com.yida.service.ReportService;
import com.yida.vo.OrderReportVO;
import com.yida.vo.SalesTop10ReportVO;
import com.yida.vo.TurnoverReportVO;
import com.yida.vo.UserReportVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;
import java.time.LocalDate;

@RestController
@RequestMapping("/admin/report")
@Slf4j
@RequiredArgsConstructor
public class ReportController {
    private final ReportService reportService;
    @GetMapping("/turnoverStatistics")
    public Result<TurnoverReportVO> turnoverStatistics(@DateTimeFormat(pattern="yyyy-MM-dd") LocalDate begin, @DateTimeFormat(pattern="yyyy-MM-dd")LocalDate end){
        log.info("查询营业额数据：{}到{}",begin,end);
        TurnoverReportVO vo=reportService.getTurnoverStatistics(begin,end);
        return Result.success(vo);
    }

    @GetMapping("/userStatistics")
    public Result<UserReportVO> userStatistics(@DateTimeFormat(pattern="yyyy-MM-dd") LocalDate begin, @DateTimeFormat(pattern="yyyy-MM-dd")LocalDate end){
        log.info("查询用户数据：{}到{}",begin,end);
        UserReportVO vo=reportService.getUserStatistics(begin,end);
        return Result.success(vo);
    }

    @GetMapping("/ordersStatistics")
    public Result<OrderReportVO> orderStatistics(@DateTimeFormat(pattern="yyyy-MM-dd") LocalDate begin, @DateTimeFormat(pattern="yyyy-MM-dd")LocalDate end){
        log.info("订单统计：{}到{}",begin,end);
        OrderReportVO vo=reportService.getOrderStatistics(begin,end);
        return Result.success(vo);
    }

    @GetMapping("/top10")
    public Result<SalesTop10ReportVO> top10(@DateTimeFormat(pattern="yyyy-MM-dd") LocalDate begin, @DateTimeFormat(pattern="yyyy-MM-dd")LocalDate end){
        log.info("查询top10商品：{}到{}",begin,end);
        SalesTop10ReportVO vo=reportService.getSalesTop10(begin,end);
        return Result.success(vo);
    }

    @GetMapping("/export")
    public void export(HttpServletResponse response){
        reportService.exportBusinessData(response);
    }
}
