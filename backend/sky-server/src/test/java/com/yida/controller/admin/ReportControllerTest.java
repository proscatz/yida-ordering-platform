package com.yida.controller.admin;

import com.yida.result.Result;
import com.yida.service.ReportService;
import com.yida.vo.TurnoverReportVO;
import org.junit.jupiter.api.Test;

import javax.servlet.http.HttpServletResponse;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ReportControllerTest {

    @Test
    void reportServiceIsRequiredAndStatisticsDelegateToIt() {
        ReportService reportService = mock(ReportService.class);
        ReportController controller = new ReportController(reportService);
        LocalDate begin = LocalDate.of(2026, 8, 1);
        LocalDate end = LocalDate.of(2026, 8, 7);
        TurnoverReportVO expected = TurnoverReportVO.builder()
                .dateList("2026-08-01")
                .turnoverList("0.0")
                .build();
        when(reportService.getTurnoverStatistics(begin, end)).thenReturn(expected);

        Result<TurnoverReportVO> result = controller.turnoverStatistics(begin, end);

        assertSame(expected, result.getData());
        verify(reportService).getTurnoverStatistics(begin, end);
    }

    @Test
    void exportDelegatesToInjectedService() {
        ReportService reportService = mock(ReportService.class);
        ReportController controller = new ReportController(reportService);
        HttpServletResponse response = mock(HttpServletResponse.class);

        controller.export(response);

        verify(reportService).exportBusinessData(response);
    }
}
