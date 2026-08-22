import { beforeEach, describe, expect, it, vi } from 'vitest'

vi.mock('./http', () => ({ request: vi.fn() }))

import { request } from './http'
import { orderApi } from './orders'

const requestMock = vi.mocked(request)

describe('order API contracts', () => {
  beforeEach(() => {
    requestMock.mockReset()
    requestMock.mockResolvedValue(undefined as never)
  })

  it('maps list, statistics and detail endpoints', async () => {
    await orderApi.page({ page: 1, pageSize: 10, status: 2, beginTime: '2026-08-01 00:00:00' })
    await orderApi.statistics()
    await orderApi.detail(9)
    expect(requestMock).toHaveBeenNthCalledWith(1, expect.objectContaining({ method: 'GET', url: '/order/conditionSearch' }))
    expect(requestMock).toHaveBeenNthCalledWith(2, { method: 'GET', url: '/order/statistics' })
    expect(requestMock).toHaveBeenNthCalledWith(3, { method: 'GET', url: '/order/details/9' })
  })

  it('maps every status operation to the backend controller contract', async () => {
    await orderApi.confirm({ id: 9, status: 3 })
    await orderApi.reject({ id: 9, rejectionReason: '已售罄' })
    await orderApi.cancel({ id: 9, cancelReason: '协商取消' })
    await orderApi.deliver(9)
    await orderApi.complete(9)
    expect(requestMock).toHaveBeenNthCalledWith(1, { method: 'PUT', url: '/order/confirm', data: { id: 9, status: 3 } })
    expect(requestMock).toHaveBeenNthCalledWith(4, { method: 'PUT', url: '/order/delivery/9' })
    expect(requestMock).toHaveBeenNthCalledWith(5, { method: 'PUT', url: '/order/complete/9' })
  })
})
