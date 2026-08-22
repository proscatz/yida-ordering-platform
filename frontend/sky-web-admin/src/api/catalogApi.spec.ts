import { beforeEach, describe, expect, it, vi } from 'vitest'
import { AppError } from './errors'

vi.mock('./http', () => ({ request: vi.fn() }))

import { dishApi } from './dishes'
import { request } from './http'
import { setmealApi } from './setmeals'
import { uploadImage } from './upload'

const requestMock = vi.mocked(request)

describe('catalog API contracts', () => {
  beforeEach(() => {
    requestMock.mockReset()
    requestMock.mockResolvedValue(undefined as never)
  })

  it('maps dish create, detail, update, status and delete operations', async () => {
    const payload = { name: '测试菜品', categoryId: 1, price: 12, image: 'https://example.test/a.jpg', description: '说明', status: 0 as const, flavors: [] }
    await dishApi.create(payload)
    await dishApi.detail(12)
    await dishApi.update({ ...payload, id: 12 })
    await dishApi.setStatus(12, 1)
    await dishApi.remove([12, 13])

    expect(requestMock).toHaveBeenNthCalledWith(1, { method: 'POST', url: '/dish', data: payload })
    expect(requestMock).toHaveBeenNthCalledWith(2, { method: 'GET', url: '/dish/12' })
    expect(requestMock).toHaveBeenNthCalledWith(4, { method: 'POST', url: '/dish/status/1', params: { id: 12 } })
    expect(requestMock).toHaveBeenNthCalledWith(5, { method: 'DELETE', url: '/dish', params: { ids: '12,13' } })
  })

  it('maps setmeal create, detail, update, status and delete operations', async () => {
    const payload = { name: '测试套餐', categoryId: 2, price: 20, status: 0 as const, description: '说明', image: 'https://example.test/s.jpg', setmealDishes: [{ dishId: 1, name: '菜品', price: 10, copies: 1 }] }
    await setmealApi.create(payload)
    await setmealApi.detail(5)
    await setmealApi.update({ ...payload, id: 5 })
    await setmealApi.setStatus(5, 0)
    await setmealApi.remove([5])

    expect(requestMock).toHaveBeenNthCalledWith(2, { method: 'GET', url: '/setmeal/5' })
    expect(requestMock).toHaveBeenNthCalledWith(4, { method: 'POST', url: '/setmeal/status/0', params: { id: 5 } })
    expect(requestMock).toHaveBeenNthCalledWith(5, { method: 'DELETE', url: '/setmeal', params: { ids: '5' } })
  })

  it('sends upload files as multipart form data', async () => {
    const file = new File(['image'], 'dish.png', { type: 'image/png' })
    await uploadImage(file)

    const config = requestMock.mock.calls[0]?.[0]
    expect(config?.url).toBe('/common/upload')
    expect(config?.data).toBeInstanceOf(FormData)
    expect((config?.data as FormData).get('file')).toBe(file)
  })

  it('keeps upload business failures visible to the form', async () => {
    requestMock.mockRejectedValueOnce(new AppError('business', '文件上传失败') as never)
    await expect(uploadImage(new File(['image'], 'dish.png', { type: 'image/png' }))).rejects.toMatchObject({ kind: 'business' })
  })
})
