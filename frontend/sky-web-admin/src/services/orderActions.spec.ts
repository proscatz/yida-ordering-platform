import { beforeEach, describe, expect, it, vi } from 'vitest'
import { AppError } from '@/api/errors'
import { resetWriteLocksForTests } from '@/utils/writeLock'

vi.mock('@/api/orders', () => ({
  orderApi: {
    confirm: vi.fn(), reject: vi.fn(), cancel: vi.fn(), deliver: vi.fn(), complete: vi.fn(),
  },
}))

import { orderApi } from '@/api/orders'
import { executeOrderAction, isOrderStateConflict, orderActionErrorMessage } from './orderActions'

describe('order status operations', () => {
  beforeEach(() => {
    resetWriteLocksForTests()
    vi.clearAllMocks()
    vi.mocked(orderApi.confirm).mockResolvedValue(undefined)
    vi.mocked(orderApi.reject).mockResolvedValue(undefined)
    vi.mocked(orderApi.cancel).mockResolvedValue(undefined)
    vi.mocked(orderApi.deliver).mockResolvedValue(undefined)
    vi.mocked(orderApi.complete).mockResolvedValue(undefined)
  })

  it('maps a valid transition to the exact backend payload', async () => {
    await executeOrderAction(18, 2, 'confirm')
    expect(orderApi.confirm).toHaveBeenCalledWith({ id: 18, status: 3 })

    await executeOrderAction(19, 2, 'reject', '菜品已售罄')
    expect(orderApi.reject).toHaveBeenCalledWith({ id: 19, rejectionReason: '菜品已售罄' })
  })

  it('blocks illegal transitions before sending a request', async () => {
    await expect(executeOrderAction(18, 5, 'deliver')).rejects.toMatchObject({
      kind: 'business', message: '当前订单状态不允许执行此操作',
    })
    expect(orderApi.deliver).not.toHaveBeenCalled()
  })

  it('requires rejection and cancellation reasons', async () => {
    await expect(executeOrderAction(18, 2, 'reject', '  ')).rejects.toThrow('请填写拒单原因')
    await expect(executeOrderAction(18, 3, 'cancel')).rejects.toThrow('请填写取消原因')
  })

  it('locks repeated clicks for the same order operation', async () => {
    let resolveRequest!: () => void
    vi.mocked(orderApi.confirm).mockImplementationOnce(() => new Promise<void>((resolve) => { resolveRequest = resolve }))
    const first = executeOrderAction(18, 2, 'confirm')
    await expect(executeOrderAction(18, 2, 'confirm')).rejects.toMatchObject({ kind: 'duplicate' })
    resolveRequest()
    await first
  })

  it('identifies server-side concurrent state conflicts without faking success', async () => {
    const conflict = new AppError('business', '订单状态错误')
    vi.mocked(orderApi.deliver).mockRejectedValueOnce(conflict)
    await expect(executeOrderAction(18, 3, 'deliver')).rejects.toBe(conflict)
    expect(isOrderStateConflict(conflict)).toBe(true)
    expect(orderActionErrorMessage(conflict)).toContain('服务端最新状态')
  })
})
