import { describe, expect, it } from 'vitest'
import {
  normalizeSetmealDishes,
  parseFlavorRecords,
  serializeFlavorDrafts,
  validateFlavorDrafts,
  validateImageFile,
  validateImageFileSignature,
  validateSetmealDishes,
} from './catalog'

describe('catalog form normalization', () => {
  it('parses JSON flavor values for edit backfill', () => {
    expect(parseFlavorRecords([{ id: 3, dishId: 9, name: '辣度', value: '["微辣","中辣"]' }])).toEqual([
      { key: '3-0', name: '辣度', values: ['微辣', '中辣'] },
    ])
  })

  it('preserves a malformed legacy flavor value instead of silently dropping it', () => {
    expect(parseFlavorRecords([{ name: '份量', value: '大份' }])[0]?.values).toEqual(['大份'])
  })

  it('rejects empty and duplicate flavor structures', () => {
    expect(validateFlavorDrafts([{ key: '1', name: '', values: ['热'] }])).toBe('规格名称不能为空')
    expect(validateFlavorDrafts([
      { key: '1', name: '温度', values: ['热'] },
      { key: '2', name: '温度', values: ['冷'] },
    ])).toContain('重复')
    expect(validateFlavorDrafts([{ key: '1', name: '辣度', values: ['微辣', '微辣'] }])).toContain('重复')
  })

  it('serializes only normalized fields required by the backend rebuild', () => {
    expect(serializeFlavorDrafts([{ key: 'old-id', name: ' 温度 ', values: [' 热 ', '冷'] }])).toEqual([
      { name: '温度', value: '["热","冷"]' },
    ])
  })

  it('removes old setmeal relation identifiers during edit backfill', () => {
    expect(normalizeSetmealDishes([{ id: 8, setmealId: 4, dishId: 2, name: '米饭', price: 3, copies: 2 }])).toEqual([
      { dishId: 2, name: '米饭', price: 3, copies: 2 },
    ])
  })

  it('validates setmeal selections, duplicates and positive integer copies', () => {
    expect(validateSetmealDishes([])).toContain('至少')
    expect(validateSetmealDishes([
      { dishId: 1, name: 'A', price: 1, copies: 1 },
      { dishId: 1, name: 'A', price: 1, copies: 1 },
    ])).toContain('重复')
    expect(validateSetmealDishes([{ dishId: 1, name: 'A', price: 1, copies: 0 }])).toContain('正整数')
  })
})

describe('image upload validation', () => {
  it('accepts supported images within five megabytes', () => {
    expect(validateImageFile(new File(['image'], 'dish.webp', { type: 'image/webp' }))).toBeNull()
  })

  it('rejects empty, oversized and mismatched image files', () => {
    expect(validateImageFile(new File([], 'empty.png', { type: 'image/png' }))).toContain('空文件')
    expect(validateImageFile(new File([new Uint8Array(5 * 1024 * 1024 + 1)], 'large.jpg', { type: 'image/jpeg' }))).toContain('5MB')
    expect(validateImageFile(new File(['image'], 'dish.jpg', { type: 'image/png' }))).toContain('仅支持')
  })

  it('accepts an image whose extension, MIME and signature agree', async () => {
    const bytes = new Uint8Array([0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a, 0, 0, 0, 0])
    await expect(validateImageFileSignature(new File([bytes], 'dish.png', { type: 'image/png' }))).resolves.toBeNull()
  })

  it('rejects forged content even when extension and declared MIME agree', async () => {
    await expect(validateImageFileSignature(new File(['not-an-image'], 'dish.png', { type: 'image/png' })))
      .resolves.toContain('内容与扩展名不一致')
  })
})
