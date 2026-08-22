import type { Address, Money } from '@/types'

export function money(value: Money | undefined): string {
  return Number(value ?? 0).toFixed(2)
}

export function fullAddress(address?: Partial<Address>): string {
  if (!address) return ''
  return [address.provinceName, address.cityName, address.districtName, address.detail]
    .filter(Boolean)
    .join('')
}

export function formatTime(value?: string): string {
  if (!value) return '--'
  return value.replace('T', ' ').slice(0, 16)
}

export const orderStatusMap: Record<number, string> = {
  1: '待支付',
  2: '待接单',
  3: '制作中',
  4: '配送中',
  5: '已完成',
  6: '已取消',
}

export function createRequestId(): string {
  if (typeof crypto !== 'undefined' && 'randomUUID' in crypto) return crypto.randomUUID()
  return `web-${Date.now()}-${Math.random().toString(16).slice(2)}`
}

export function parseFlavorOptions(value?: string): string[] {
  if (!value) return []
  try {
    const parsed: unknown = JSON.parse(value)
    return Array.isArray(parsed) ? parsed.map(String) : []
  } catch {
    return value.split(',').map((item) => item.trim()).filter(Boolean)
  }
}