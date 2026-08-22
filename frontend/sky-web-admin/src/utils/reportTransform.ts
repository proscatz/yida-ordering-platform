import type {
  OrderChartData,
  OrderReportVO,
  ReportRange,
  SalesTop10ReportVO,
  Top10ChartData,
  TurnoverChartData,
  TurnoverReportVO,
  UserChartData,
  UserReportVO,
} from '@/types/reports'

function splitCsv(value: string | null | undefined) {
  if (!value?.trim()) return []
  return value.split(',').map((item) => item.trim())
}

function numericCsv(value: string | null | undefined) {
  return splitCsv(value).map((item) => {
    const parsed = Number(item)
    return Number.isFinite(parsed) ? parsed : 0
  })
}

function finiteNumber(value: number | null | undefined) {
  return typeof value === 'number' && Number.isFinite(value) ? value : 0
}

export function formatLocalDate(date: Date) {
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

function isValidIsoDate(value: string) {
  if (!/^\d{4}-\d{2}-\d{2}$/.test(value)) return false
  const date = new Date(`${value}T00:00:00Z`)
  return !Number.isNaN(date.getTime()) && date.toISOString().slice(0, 10) === value
}

export function validateReportRange(range: ReportRange) {
  if (!isValidIsoDate(range.begin) || !isValidIsoDate(range.end)) return '请选择完整的开始和结束日期'
  if (range.begin > range.end) return '开始日期不能晚于结束日期'
  return null
}

export function defaultReportRange(today = new Date()): ReportRange {
  const end = new Date(today.getFullYear(), today.getMonth(), today.getDate())
  const begin = new Date(end)
  begin.setDate(begin.getDate() - 6)
  return { begin: formatLocalDate(begin), end: formatLocalDate(end) }
}

export function createDateRange(range: ReportRange) {
  if (validateReportRange(range)) return []
  const result: string[] = []
  const cursor = new Date(`${range.begin}T00:00:00Z`)
  const end = new Date(`${range.end}T00:00:00Z`)
  while (cursor <= end) {
    result.push(cursor.toISOString().slice(0, 10))
    cursor.setUTCDate(cursor.getUTCDate() + 1)
  }
  return result
}

function alignValues(responseDates: string[], values: number[], range: ReportRange) {
  const expectedDates = createDateRange(range)
  const byDate = new Map(responseDates.map((date, index) => [date, values[index] ?? 0]))
  return { dates: expectedDates, values: expectedDates.map((date) => byDate.get(date) ?? 0) }
}

export function transformTurnoverReport(vo: TurnoverReportVO, range: ReportRange): TurnoverChartData {
  const responseDates = splitCsv(vo.dateList)
  const aligned = alignValues(responseDates, numericCsv(vo.turnoverList), range)
  return { ...aligned, hasData: responseDates.length > 0 }
}

export function transformUserReport(vo: UserReportVO, range: ReportRange): UserChartData {
  const responseDates = splitCsv(vo.dateList)
  const total = alignValues(responseDates, numericCsv(vo.totalUserList), range)
  const added = alignValues(responseDates, numericCsv(vo.newUserList), range)
  return { dates: total.dates, totalUsers: total.values, newUsers: added.values, hasData: responseDates.length > 0 }
}

export function safeCompletionRate(rate: number | null | undefined, total: number, valid: number) {
  if (total <= 0) return 0
  const candidate = typeof rate === 'number' && Number.isFinite(rate) ? rate : valid / total
  return Math.min(1, Math.max(0, candidate))
}

export function transformOrderReport(vo: OrderReportVO, range: ReportRange): OrderChartData {
  const responseDates = splitCsv(vo.dateList)
  const counts = alignValues(responseDates, numericCsv(vo.orderCountList), range)
  const validCounts = alignValues(responseDates, numericCsv(vo.validOrderCountList), range)
  const totalOrderCount = finiteNumber(vo.totalOrderCount)
  const validOrderCount = finiteNumber(vo.validOrderCount)
  return {
    dates: counts.dates,
    orderCounts: counts.values,
    validOrderCounts: validCounts.values,
    totalOrderCount,
    validOrderCount,
    completionRate: safeCompletionRate(vo.orderCompletionRate, totalOrderCount, validOrderCount),
    hasData: responseDates.length > 0,
  }
}

export function transformTop10Report(vo: SalesTop10ReportVO): Top10ChartData {
  const names = splitCsv(vo.nameList)
  const values = numericCsv(vo.numberList)
  return { names, values: names.map((_, index) => values[index] ?? 0), hasData: names.length > 0 }
}
