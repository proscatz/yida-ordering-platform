export interface ReportRange {
  begin: string
  end: string
}

export interface TurnoverReportVO {
  dateList: string | null
  turnoverList: string | null
}

export interface UserReportVO {
  dateList: string | null
  totalUserList: string | null
  newUserList: string | null
}

export interface OrderReportVO {
  dateList: string | null
  orderCountList: string | null
  validOrderCountList: string | null
  totalOrderCount: number | null
  validOrderCount: number | null
  orderCompletionRate: number | null
}

export interface SalesTop10ReportVO {
  nameList: string | null
  numberList: string | null
}

export interface TurnoverChartData {
  dates: string[]
  values: number[]
  hasData: boolean
}

export interface UserChartData {
  dates: string[]
  totalUsers: number[]
  newUsers: number[]
  hasData: boolean
}

export interface OrderChartData {
  dates: string[]
  orderCounts: number[]
  validOrderCounts: number[]
  totalOrderCount: number
  validOrderCount: number
  completionRate: number
  hasData: boolean
}

export interface Top10ChartData {
  names: string[]
  values: number[]
  hasData: boolean
}
