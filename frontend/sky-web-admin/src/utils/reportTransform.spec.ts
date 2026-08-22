import { describe, expect, it } from 'vitest'
import {
  createDateRange,
  defaultReportRange,
  safeCompletionRate,
  transformOrderReport,
  transformTop10Report,
  transformTurnoverReport,
  transformUserReport,
  validateReportRange,
} from './reportTransform'

const range = { begin: '2026-08-15', end: '2026-08-17' }

describe('report date ranges', () => {
  it('creates an inclusive default seven-day range in local time', () => {
    expect(defaultReportRange(new Date(2026, 7, 21, 12))).toEqual({ begin: '2026-08-15', end: '2026-08-21' })
  })

  it('validates reversed and invalid calendar dates', () => {
    expect(validateReportRange({ begin: '2026-08-18', end: '2026-08-17' })).toContain('不能晚于')
    expect(validateReportRange({ begin: '2026-02-30', end: '2026-03-01' })).toContain('完整')
    expect(createDateRange(range)).toEqual(['2026-08-15', '2026-08-16', '2026-08-17'])
  })
})

describe('report data alignment', () => {
  it('aligns partial turnover dates and fills missing or invalid values with zero', () => {
    expect(transformTurnoverReport({ dateList: '2026-08-15,2026-08-17', turnoverList: '18.5,invalid' }, range)).toEqual({
      dates: ['2026-08-15', '2026-08-16', '2026-08-17'], values: [18.5, 0, 0], hasData: true,
    })
  })

  it('distinguishes an empty response from a real series containing zero', () => {
    expect(transformTurnoverReport({ dateList: null, turnoverList: null }, range)).toMatchObject({ values: [0, 0, 0], hasData: false })
    expect(transformTurnoverReport({ dateList: '2026-08-15', turnoverList: '0' }, { begin: '2026-08-15', end: '2026-08-15' })).toMatchObject({ values: [0], hasData: true })
  })

  it('aligns cumulative and new-user series independently', () => {
    const result = transformUserReport({
      dateList: '2026-08-15,2026-08-17', totalUserList: '10,12', newUserList: '2',
    }, range)
    expect(result.totalUsers).toEqual([10, 0, 12])
    expect(result.newUsers).toEqual([2, 0, 0])
  })

  it('normalizes completion rate for zero totals, invalid values and boundaries', () => {
    expect(safeCompletionRate(Number.NaN, 0, 0)).toBe(0)
    expect(safeCompletionRate(Number.NaN, 10, 4)).toBe(0.4)
    expect(safeCompletionRate(1.4, 10, 10)).toBe(1)
    expect(safeCompletionRate(-0.2, 10, 0)).toBe(0)
  })

  it('keeps order zero values and pads missing daily counts', () => {
    const result = transformOrderReport({
      dateList: '2026-08-15,2026-08-17', orderCountList: '0,3', validOrderCountList: '0',
      totalOrderCount: 3, validOrderCount: 0, orderCompletionRate: 0,
    }, range)
    expect(result.orderCounts).toEqual([0, 0, 3])
    expect(result.validOrderCounts).toEqual([0, 0, 0])
    expect(result.completionRate).toBe(0)
    expect(result.hasData).toBe(true)
  })

  it('pads Top 10 value mismatches without inventing product names', () => {
    expect(transformTop10Report({ nameList: '鸡排饭,能量碗', numberList: '8' })).toEqual({
      names: ['鸡排饭', '能量碗'], values: [8, 0], hasData: true,
    })
    expect(transformTop10Report({ nameList: '', numberList: '3' }).hasData).toBe(false)
  })
})
