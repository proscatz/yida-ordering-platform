import { beforeEach, describe, expect, it, vi } from 'vitest'

vi.mock('./http', () => ({ request: vi.fn() }))

import { categoryApi } from './categories'
import { employeeApi } from './employees'
import { request } from './http'
import { shopApi } from './shop'
import { workspaceApi } from './workspace'

const requestMock = vi.mocked(request)

describe('management API contracts', () => {
  beforeEach(() => {
    requestMock.mockReset()
    requestMock.mockResolvedValue(undefined as never)
  })

  it('maps all workspace overview endpoints', async () => {
    await Promise.all([
      workspaceApi.businessData(),
      workspaceApi.orderOverview(),
      workspaceApi.dishOverview(),
      workspaceApi.setmealOverview(),
    ])

    expect(requestMock.mock.calls.map(([config]) => config.url)).toEqual([
      '/workspace/businessData',
      '/workspace/overviewOrders',
      '/workspace/overviewDishes',
      '/workspace/overviewSetmeals',
    ])
  })

  it('uses the path status contract for shop changes', async () => {
    await shopApi.getStatus()
    await shopApi.setStatus(1)

    expect(requestMock).toHaveBeenNthCalledWith(1, { method: 'GET', url: '/shop/status' })
    expect(requestMock).toHaveBeenNthCalledWith(2, { method: 'PUT', url: '/shop/1' })
  })

  it('maps employee pagination, detail, save and status parameters', async () => {
    await employeeApi.page({ page: 2, pageSize: 20, name: '张' })
    await employeeApi.me()
    await employeeApi.detail(9)
    await employeeApi.create({ username: 'tester', name: '测试员', phone: '13800000000', sex: '1', idNumber: '110101199001011234' })
    await employeeApi.setStatus(9, 0)

    expect(requestMock).toHaveBeenNthCalledWith(1, { method: 'GET', url: '/employee/page', params: { page: 2, pageSize: 20, name: '张' } })
    expect(requestMock).toHaveBeenNthCalledWith(2, { method: 'GET', url: '/employee/me' })
    expect(requestMock).toHaveBeenNthCalledWith(3, { method: 'GET', url: '/employee/9' })
    expect(requestMock).toHaveBeenNthCalledWith(5, { method: 'POST', url: '/employee/status/0', params: { id: 9 } })
  })

  it('maps category filters and type list without changing URL contracts', async () => {
    await categoryApi.page({ page: 1, pageSize: 10, type: 2 })
    await categoryApi.list(1)

    expect(requestMock).toHaveBeenNthCalledWith(1, { method: 'GET', url: '/category/page', params: { page: 1, pageSize: 10, type: 2 } })
    expect(requestMock).toHaveBeenNthCalledWith(2, { method: 'GET', url: '/category/list', params: { type: 1 } })
  })

  it('maps category update, status and delete operations', async () => {
    await categoryApi.update({ id: 5, type: 1, name: '热销', sort: 3 })
    await categoryApi.setStatus(5, 0)
    await categoryApi.remove(5)

    expect(requestMock).toHaveBeenNthCalledWith(1, { method: 'PUT', url: '/category', data: { id: 5, type: 1, name: '热销', sort: 3 } })
    expect(requestMock).toHaveBeenNthCalledWith(2, { method: 'POST', url: '/category/status/0', params: { id: 5 } })
    expect(requestMock).toHaveBeenNthCalledWith(3, { method: 'DELETE', url: '/category', params: { id: 5 } })
  })
})
