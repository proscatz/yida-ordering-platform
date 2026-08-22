import axios, { AxiosError, type AxiosRequestConfig, type AxiosResponse } from 'axios'
import type { ApiResult } from '@/types'

declare module 'axios' {
  export interface AxiosRequestConfig {
    dedupe?: boolean
  }
}

export class AppRequestError extends Error {
  constructor(
    message: string,
    public readonly kind: 'business' | 'network' | 'unauthorized' | 'duplicate',
    public readonly fieldErrors: Record<string, string> = {},
  ) {
    super(message)
  }
}

const pending = new Map<string, number>()
let unauthorizedHandler: (() => void) | undefined

export function setUnauthorizedHandler(handler: () => void): void {
  unauthorizedHandler = handler
}

const http = axios.create({
  baseURL: '/api',
  timeout: 12_000,
  headers: { 'Content-Type': 'application/json' },
})

function requestKey(config: AxiosRequestConfig): string {
  return `${config.method ?? 'get'}:${config.url ?? ''}:${JSON.stringify(config.params ?? {})}:${JSON.stringify(config.data ?? {})}`
}

function release(config?: AxiosRequestConfig): void {
  const key = config ? requestKey(config) : ''
  if (key) pending.delete(key)
}

http.interceptors.request.use((config) => {
  const token = localStorage.getItem('yida-user-token')
  if (token) config.headers.set('authentication', token)
  if (config.dedupe) {
    const key = requestKey(config)
    if (pending.has(key)) return Promise.reject(new AppRequestError('请求正在处理中，请勿重复提交', 'duplicate'))
    pending.set(key, Date.now())
  }
  return config
})

http.interceptors.response.use(
  (response: AxiosResponse<ApiResult<unknown>>) => {
    release(response.config)
    const body = response.data
    if (!body || body.code !== 1) {
      const fieldErrors = isFieldErrors(body?.data) ? body.data : {}
      throw new AppRequestError(body?.msg || '服务暂时不可用', 'business', fieldErrors)
    }
    return response
  },
  (error: AxiosError | AppRequestError) => {
    if (axios.isAxiosError(error)) {
      release(error.config)
      if (error.response?.status === 401) {
        unauthorizedHandler?.()
        return Promise.reject(new AppRequestError('登录状态已失效，请重新登录', 'unauthorized'))
      }
      if (error.response) {
        const body = error.response.data as Partial<ApiResult<unknown>> | undefined
        const fieldErrors = isFieldErrors(body?.data) ? body.data : {}
        return Promise.reject(new AppRequestError(body?.msg || '服务暂时不可用', 'business', fieldErrors))
      }
      return Promise.reject(new AppRequestError(
        error.code === 'ECONNABORTED' ? '请求超时，请稍后重试' : '网络连接异常，请检查后端服务',
        'network',
      ))
    }
    return Promise.reject(error)
  },
)

function isFieldErrors(value: unknown): value is Record<string, string> {
  return Boolean(value) && typeof value === 'object' && !Array.isArray(value)
    && Object.values(value as Record<string, unknown>).every((item) => typeof item === 'string')
}

export async function request<T>(config: AxiosRequestConfig): Promise<T> {
  const response = await http.request<ApiResult<T>>(config)
  return response.data.data
}

export function errorMessage(error: unknown): string {
  return error instanceof Error ? error.message : '操作失败，请稍后重试'
}
