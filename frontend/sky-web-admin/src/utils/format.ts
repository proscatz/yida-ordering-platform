import type { Optional } from '@/types/api'

export function formatMoney(value: Optional<number>) {
  return new Intl.NumberFormat('zh-CN', { style: 'currency', currency: 'CNY' }).format(value ?? 0)
}

export function formatPercent(value: Optional<number>) {
  return new Intl.NumberFormat('zh-CN', { style: 'percent', maximumFractionDigits: 1 }).format(value ?? 0)
}

export function formatDateTime(value: Optional<string>) {
  if (!value) return '--'
  const normalized = value.includes('T') ? value : value.replace(' ', 'T')
  const date = new Date(normalized)
  return Number.isNaN(date.getTime())
    ? value
    : new Intl.DateTimeFormat('zh-CN', { dateStyle: 'medium', timeStyle: 'short', hour12: false }).format(date)
}
