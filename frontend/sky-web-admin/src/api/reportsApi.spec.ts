import { beforeEach, describe, expect, it, vi } from 'vitest'

vi.mock('./http', () => ({ request: vi.fn(), download: vi.fn() }))

import { download, request } from './http'
import { reportApi } from './reports'

const requestMock = vi.mocked(request)
const downloadMock = vi.mocked(download)

describe('report API contracts', () => {
  beforeEach(() => {
    requestMock.mockReset()
    downloadMock.mockReset()
    requestMock.mockResolvedValue(undefined as never)
    downloadMock.mockResolvedValue({ blob: new Blob(), fileName: null, contentType: 'application/octet-stream' })
  })

  it('maps all statistics endpoints with yyyy-MM-dd range parameters', async () => {
    const params = { begin: '2026-08-15', end: '2026-08-21' }
    await reportApi.turnover(params)
    await reportApi.users(params)
    await reportApi.orders(params)
    await reportApi.top10(params)
    expect(requestMock).toHaveBeenNthCalledWith(1, { method: 'GET', url: '/report/turnoverStatistics', params })
    expect(requestMock).toHaveBeenNthCalledWith(2, { method: 'GET', url: '/report/userStatistics', params })
    expect(requestMock).toHaveBeenNthCalledWith(3, { method: 'GET', url: '/report/ordersStatistics', params })
    expect(requestMock).toHaveBeenNthCalledWith(4, { method: 'GET', url: '/report/top10', params })
  })

  it('requests export as a binary download', async () => {
    await reportApi.export()
    expect(downloadMock).toHaveBeenCalledWith({ method: 'GET', url: '/report/export' })
  })
})
