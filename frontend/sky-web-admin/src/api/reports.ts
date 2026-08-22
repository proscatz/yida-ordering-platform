import { download, request } from './http'
import type { FileDownload } from '@/types/api'
import type {
  OrderReportVO,
  ReportRange,
  SalesTop10ReportVO,
  TurnoverReportVO,
  UserReportVO,
} from '@/types/reports'

export const reportApi = {
  turnover: (params: ReportRange) => request<TurnoverReportVO>({ method: 'GET', url: '/report/turnoverStatistics', params }),
  users: (params: ReportRange) => request<UserReportVO>({ method: 'GET', url: '/report/userStatistics', params }),
  orders: (params: ReportRange) => request<OrderReportVO>({ method: 'GET', url: '/report/ordersStatistics', params }),
  top10: (params: ReportRange) => request<SalesTop10ReportVO>({ method: 'GET', url: '/report/top10', params }),
  export: (): Promise<FileDownload> => download({ method: 'GET', url: '/report/export' }),
}
