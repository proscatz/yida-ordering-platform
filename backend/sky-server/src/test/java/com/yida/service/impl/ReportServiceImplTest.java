package com.yida.service.impl;

import com.yida.mapper.OrderMapper;
import com.yida.mapper.UserMapper;
import com.yida.service.WorkspaceService;
import com.yida.vo.BusinessDataVO;
import com.yida.vo.OrderReportVO;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.ByteArrayInputStream;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportServiceImplTest {
    @Mock
    private OrderMapper orderMapper;
    @Mock
    private UserMapper userMapper;
    @Mock
    private WorkspaceService workspaceService;
    @InjectMocks
    private ReportServiceImpl reportService;

    @Test
    void orderStatisticsReturnsZeroCompletionRateWhenThereAreNoOrders() {
        when(orderMapper.countByMap(any())).thenReturn(0);
        LocalDate date = LocalDate.of(2026, 8, 22);

        OrderReportVO result = reportService.getOrderStatistics(date, date);

        assertEquals(0, result.getTotalOrderCount());
        assertEquals(0, result.getValidOrderCount());
        assertEquals(0.0, result.getOrderCompletionRate());
        assertEquals("0", result.getOrderCountList());
        assertEquals("0", result.getValidOrderCountList());
    }

    @Test
    void exportProducesReadableWorkbookAndDownloadHeaders() throws Exception {
        BusinessDataVO data = BusinessDataVO.builder()
                .turnover(120.50)
                .validOrderCount(3)
                .orderCompletionRate(0.75)
                .unitPrice(40.17)
                .newUsers(2)
                .build();
        when(workspaceService.getBusinessData(any(), any())).thenReturn(data);
        MockHttpServletResponse response = new MockHttpServletResponse();

        reportService.exportBusinessData(response);

        assertEquals("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", response.getContentType());
        assertTrue(response.getHeader("Content-Disposition").startsWith("attachment; filename*=UTF-8''"));
        assertTrue(response.getContentAsByteArray().length > 1_000);
        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(response.getContentAsByteArray()))) {
            assertNotNull(workbook.getSheet("运营数据"));
            assertEquals("驿达点餐运营数据报表", workbook.getSheet("运营数据").getRow(0).getCell(0).getStringCellValue());
            assertEquals(120.50, workbook.getSheet("运营数据").getRow(4).getCell(0).getNumericCellValue());
            assertEquals(36, workbook.getSheet("运营数据").getLastRowNum());
        }
        verify(workspaceService, times(31)).getBusinessData(any(), any());
    }
}
