import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { AxiosError, AxiosHeaders, type AxiosAdapter } from 'axios'
import { ADMIN_TOKEN_HEADER, download, http, request } from './http'
import { configureUnauthorizedHandler, resetUnauthorizedStateForTests } from './unauthorized'
import { clearSession, writeSession } from '@/utils/session'

const originalAdapter = http.defaults.adapter

function successAdapter(data: unknown, headers: Record<string, string> = {}): AxiosAdapter {
  return async (config) => ({ data, headers, status: 200, statusText: 'OK', config })
}

function rejectedAdapter(status: number, data?: unknown): AxiosAdapter {
  return async (config) => {
    throw new AxiosError(
      'Request failed',
      AxiosError.ERR_BAD_RESPONSE,
      config,
      undefined,
      { data, headers: {}, status, statusText: 'Error', config },
    )
  }
}

describe('HTTP request layer', () => {
  beforeEach(() => {
    clearSession()
    resetUnauthorizedStateForTests()
  })

  afterEach(() => {
    http.defaults.adapter = originalAdapter
  })

  it('unwraps a successful business response and sends the admin token header', async () => {
    writeSession({ id: 1, userName: 'operator', name: '运营员', role: 'EMPLOYEE', token: 'header-token' })
    let capturedHeaders = new AxiosHeaders()
    http.defaults.adapter = async (config) => {
      capturedHeaders = config.headers
      return { data: { code: 1, data: { ready: true } }, headers: {}, status: 200, statusText: 'OK', config }
    }

    await expect(request<{ ready: boolean }>({ method: 'GET', url: '/check' })).resolves.toEqual({ ready: true })
    expect(capturedHeaders.get(ADMIN_TOKEN_HEADER)).toBe('header-token')
  })

  it('classifies a code-zero response as a business error', async () => {
    http.defaults.adapter = successAdapter({ code: 0, msg: '业务校验未通过' })

    await expect(request({ method: 'POST', url: '/check' })).rejects.toMatchObject({
      kind: 'business',
      message: '业务校验未通过',
      businessCode: 0,
    })
  })

  it('handles one unauthorized response once', async () => {
    const handler = vi.fn()
    configureUnauthorizedHandler(handler)
    http.defaults.adapter = rejectedAdapter(401)

    await expect(request({ method: 'GET', url: '/protected' })).rejects.toMatchObject({ kind: 'auth', status: 401 })
    expect(handler).toHaveBeenCalledTimes(1)
  })

  it('coalesces concurrent unauthorized responses into one lifecycle action', async () => {
    const handler = vi.fn(async () => Promise.resolve())
    configureUnauthorizedHandler(handler)
    http.defaults.adapter = rejectedAdapter(401)

    const results = await Promise.allSettled([
      request({ method: 'GET', url: '/protected/a' }),
      request({ method: 'GET', url: '/protected/b' }),
      request({ method: 'GET', url: '/protected/c' }),
    ])

    expect(results.every((result) => result.status === 'rejected')).toBe(true)
    expect(handler).toHaveBeenCalledTimes(1)
  })

  it('distinguishes a network failure from an HTTP response', async () => {
    http.defaults.adapter = async (config) => {
      throw new AxiosError('Network Error', AxiosError.ERR_NETWORK, config)
    }

    await expect(request({ method: 'GET', url: '/offline' })).rejects.toMatchObject({ kind: 'network' })
  })

  it('distinguishes a canceled request', async () => {
    http.defaults.adapter = async (config) => {
      throw new AxiosError('Canceled', AxiosError.ERR_CANCELED, config)
    }

    await expect(request({ method: 'GET', url: '/canceled' })).rejects.toMatchObject({ kind: 'canceled' })
  })

  it('maps unavailable gateway responses to a service error', async () => {
    http.defaults.adapter = rejectedAdapter(503)

    await expect(request({ method: 'GET', url: '/unavailable' })).rejects.toMatchObject({ kind: 'service', status: 503 })
  })

  it('normalizes a file download and decodes its name', async () => {
    const blob = new Blob(['report'], { type: 'text/plain' })
    http.defaults.adapter = successAdapter(blob, {
      'content-type': 'text/plain',
      'content-disposition': "attachment; filename*=UTF-8''%E6%8A%A5%E8%A1%A8.txt",
    })

    await expect(download({ method: 'GET', url: '/report' })).resolves.toMatchObject({
      blob,
      fileName: '报表.txt',
      contentType: 'text/plain',
    })
  })
})
