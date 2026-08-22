package com.yida.service.impl;

import com.yida.dto.GoodsSalesDTO;
import com.yida.entity.Orders;
import com.yida.mapper.OrderMapper;
import com.yida.mapper.UserMapper;
import com.yida.service.ReportService;
import com.yida.service.WorkspaceService;
import com.yida.vo.BusinessDataVO;
import com.yida.vo.OrderReportVO;
import com.yida.vo.SalesTop10ReportVO;
import com.yida.vo.TurnoverReportVO;
import com.yida.vo.UserReportVO;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ReportServiceImpl implements ReportService {
    private static final String EXCEL_CONTENT_TYPE = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private WorkspaceService workspaceService;

    @Override
    public TurnoverReportVO getTurnoverStatistics(LocalDate begin, LocalDate end) {
        List<LocalDate> list=new ArrayList<>();
        list.add(begin);
        while(!begin.equals(end)){
            begin=begin.plusDays(1);
            list.add(begin);
        }
        List<Double>TurnoverList=new ArrayList<>();
        for(LocalDate date:list){
            Map<String, Object> map=new HashMap();
            map.put("begin",LocalDateTime.of(date, LocalTime.MIN));
            map.put("end",LocalDateTime.of(date, LocalTime.MAX));
            map.put("status", Orders.COMPLETED);
            Double num=orderMapper.sumByMap(map);
            TurnoverList.add(num==null?0.0:num);
        }

        return TurnoverReportVO.builder()
                .dateList(StringUtils.join(list,","))
                .turnoverList(StringUtils.join(TurnoverList,","))
                .build();
    }

    @Override
    public UserReportVO getUserStatistics(LocalDate begin, LocalDate end) {
        List<LocalDate> list=new ArrayList<>();
        list.add(begin);
        while(!begin.equals(end)){
            begin=begin.plusDays(1);
            list.add(begin);
        }
        List<Integer> newUserList=new ArrayList<>();
        List<Integer> totalUserList=new ArrayList<>();
        for(LocalDate date:list){
            Map<String, Object> map=new HashMap();
            map.put("end",LocalDateTime.of(date, LocalTime.MAX));

            Integer totalUser = userMapper.countByMap(map);
            totalUserList.add(totalUser);

            map.put("begin",LocalDateTime.of(date, LocalTime.MIN));

            Integer newUser = userMapper.countByMap(map);
            newUserList.add(newUser);
        }

        return UserReportVO.builder()
                .dateList(StringUtils.join(list,","))
                .totalUserList(StringUtils.join(totalUserList,","))
                .newUserList(StringUtils.join(newUserList,","))
                .build();
    }

    @Override
    public OrderReportVO getOrderStatistics(LocalDate begin, LocalDate end) {
        List<LocalDate> list=new ArrayList<>();
        list.add(begin);
        while(!begin.equals(end)){
            begin=begin.plusDays(1);
            list.add(begin);
        }
        ArrayList<Integer> orderCountList = new ArrayList<>();
        ArrayList<Integer> validOrderCountList = new ArrayList<>();
        for(LocalDate date:list){
            Map<String, Object> map=new HashMap();
            map.put("end",LocalDateTime.of(date, LocalTime.MAX));
            map.put("begin",LocalDateTime.of(date, LocalTime.MIN));
            orderCountList.add(orderMapper.countByMap(map));
            map.put("status",Orders.COMPLETED);
            validOrderCountList.add(orderMapper.countByMap(map));
        }
        int totalOrderCount = orderCountList.stream().mapToInt(Integer::intValue).sum();
        int validOrderCount = validOrderCountList.stream().mapToInt(Integer::intValue).sum();
        double completionRate = totalOrderCount == 0 ? 0.0 : (double) validOrderCount / totalOrderCount;
        return OrderReportVO.builder()
                .dateList(StringUtils.join(list,","))
                .orderCountList(StringUtils.join(orderCountList,","))
                .validOrderCountList(StringUtils.join(validOrderCountList,","))
                .totalOrderCount(totalOrderCount)
                .validOrderCount(validOrderCount)
                .orderCompletionRate(completionRate)
                .build();
    }

    @Override
    public SalesTop10ReportVO getSalesTop10(LocalDate begin, LocalDate end) {
        List<GoodsSalesDTO>list=orderMapper.getSalesTop10(LocalDateTime.of(begin, LocalTime.MIN),LocalDateTime.of(end, LocalTime.MAX));
        List<String> nameCollect = list.stream().map(GoodsSalesDTO::getName).collect(Collectors.toList());
        List<Integer> numberCollect = list.stream().map(GoodsSalesDTO::getNumber).collect(Collectors.toList());
        String nameJoin = StringUtils.join(nameCollect, ",");
        String numberJoin = StringUtils.join(numberCollect, ",");
        return SalesTop10ReportVO.builder()
                .nameList(nameJoin)
                .numberList(numberJoin)
                .build();
    }

    @Override
    public void exportBusinessData(HttpServletResponse response) {
        LocalDate beginDate = LocalDate.now().plusDays(-30);
        LocalDate endDate = LocalDate.now().plusDays(-1);
        BusinessDataVO businessData = workspaceService.getBusinessData(LocalDateTime.of(beginDate, LocalTime.MIN), LocalDateTime.of(endDate, LocalTime.MAX));
        String fileName = "驿达点餐_运营数据报表_" + endDate + ".xlsx";
        String encodedFileName = URLEncoder.encode(fileName, StandardCharsets.UTF_8).replace("+", "%20");
        response.setContentType(EXCEL_CONTENT_TYPE);
        response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''" + encodedFileName);

        try (XSSFWorkbook excel = new XSSFWorkbook();
             ServletOutputStream outputStream = response.getOutputStream()) {
            XSSFSheet sheet = excel.createSheet("运营数据");
            CellStyle titleStyle = titleStyle(excel);
            CellStyle headerStyle = headerStyle(excel);
            CellStyle bodyStyle = bodyStyle(excel);

            Row titleRow = sheet.createRow(0);
            titleRow.setHeightInPoints(30);
            titleRow.createCell(0).setCellValue("驿达点餐运营数据报表");
            titleRow.getCell(0).setCellStyle(titleStyle);
            sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 5));

            Row rangeRow = sheet.createRow(1);
            rangeRow.createCell(0).setCellValue("统计时间：" + beginDate + " 至 " + endDate);
            rangeRow.getCell(0).setCellStyle(bodyStyle);
            sheet.addMergedRegion(new CellRangeAddress(1, 1, 0, 5));

            writeRow(sheet.createRow(3), headerStyle, "营业额（元）", "有效订单", "订单完成率", "平均客单价（元）", "新增用户", "统计天数");
            Row summaryRow = sheet.createRow(4);
            writeNumber(summaryRow, 0, safeDouble(businessData.getTurnover()), bodyStyle);
            writeNumber(summaryRow, 1, safeInteger(businessData.getValidOrderCount()), bodyStyle);
            writeNumber(summaryRow, 2, safeDouble(businessData.getOrderCompletionRate()), bodyStyle);
            writeNumber(summaryRow, 3, safeDouble(businessData.getUnitPrice()), bodyStyle);
            writeNumber(summaryRow, 4, safeInteger(businessData.getNewUsers()), bodyStyle);
            writeNumber(summaryRow, 5, 30, bodyStyle);

            writeRow(sheet.createRow(6), headerStyle, "日期", "营业额（元）", "有效订单", "订单完成率", "平均客单价（元）", "新增用户");
            for (int i = 0; i < 30; i++) {
                LocalDate date = beginDate.plusDays(i);
                BusinessDataVO dailyData = workspaceService.getBusinessData(LocalDateTime.of(date, LocalTime.MIN), LocalDateTime.of(date, LocalTime.MAX));
                Row row = sheet.createRow(7 + i);
                row.createCell(0).setCellValue(date.toString());
                row.getCell(0).setCellStyle(bodyStyle);
                writeNumber(row, 1, safeDouble(dailyData.getTurnover()), bodyStyle);
                writeNumber(row, 2, safeInteger(dailyData.getValidOrderCount()), bodyStyle);
                writeNumber(row, 3, safeDouble(dailyData.getOrderCompletionRate()), bodyStyle);
                writeNumber(row, 4, safeDouble(dailyData.getUnitPrice()), bodyStyle);
                writeNumber(row, 5, safeInteger(dailyData.getNewUsers()), bodyStyle);
            }

            sheet.createFreezePane(0, 7);
            sheet.setAutoFilter(new CellRangeAddress(6, 36, 0, 5));
            int[] widths = {15, 18, 14, 16, 20, 14};
            for (int i = 0; i < widths.length; i++) sheet.setColumnWidth(i, widths[i] * 256);

            excel.write(outputStream);
            outputStream.flush();
        } catch (IOException e) {
            throw new IllegalStateException("运营数据报表导出失败", e);
        }
    }

    private void writeRow(Row row, CellStyle style, String... values) {
        for (int i = 0; i < values.length; i++) {
            row.createCell(i).setCellValue(values[i]);
            row.getCell(i).setCellStyle(style);
        }
    }

    private void writeNumber(Row row, int column, double value, CellStyle style) {
        row.createCell(column).setCellValue(value);
        row.getCell(column).setCellStyle(style);
    }

    private double safeDouble(Double value) {
        return value == null ? 0.0 : value;
    }

    private int safeInteger(Integer value) {
        return value == null ? 0 : value;
    }

    private CellStyle titleStyle(XSSFWorkbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 18);
        font.setColor(IndexedColors.WHITE.getIndex());
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setFillForegroundColor(IndexedColors.TEAL.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        return style;
    }

    private CellStyle headerStyle(XSSFWorkbook workbook) {
        CellStyle style = bodyStyle(workbook);
        Font font = workbook.createFont();
        font.setBold(true);
        font.setColor(IndexedColors.WHITE.getIndex());
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.TEAL.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        return style;
    }

    private CellStyle bodyStyle(XSSFWorkbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        return style;
    }
}
