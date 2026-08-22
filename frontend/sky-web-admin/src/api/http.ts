import axios, { AxiosError, type AxiosRequestConfig, type AxiosResponse } from 'axios'
import { AppError, isAppError } from './errors'
import { handleUnauthorized } from './unauthorized'
import { readSession } from '@/utils/session'
import type { ApiResult, FileDownload } from '@/types/api'

const ADMIN_TOKEN_HEADER = 'token'
const SERVICE_ERROR_STATUSES = new Set([502, 503, 504])

function isApiResult(value: unknown): value is ApiResult<unknown> {
  return typeof value === 'object' && value !== null && typeof (value as { code?: unknown }).code === 'number'
}

export function safeResultMessage(value: unknown): string | null {
  return isApiResult(value) && typeof value.msg === 'string' && value.msg.trim()
    ? value.msg.trim()
    : null
}

function fileNameFromDisposition(value?: string) {
  if (!value) return null
  const utf8 = value.match(/filename\*=UTF-8''([^;]+)/i)?.[1]
  if (utf8) {
    try {
      return decodeURIComponent(utf8)
    } catch {
      return utf8
    }
  }
  return value.match(/filename="?([^";]+)"?/i)?.[1] ?? null
}

function downloadFromResponse(response: AxiosResponse<Blob>): FileDownload {
  return {
    blob: response.data,
    fileName: fileNameFromDisposition(response.headers['content-disposition']),
    contentType: String(response.headers['content-type'] || response.data.type || 'application/octet-stream'),
  }
}

function normalizeAxiosError(error: AxiosError<ApiResult<unknown>>): AppError {
  if (axios.isCancel(error) || error.code === AxiosError.ERR_CANCELED) {
    return new AppError('canceled', '请求已取消', { cause: error })
  }

  const status = error.response?.status
  const safeServerMessage = safeResultMessage(error.response?.data)
  if (!error.response) {
    const message = error.code === AxiosError.ECONNABORTED
      ? '请求超时，请稍后重试'
      : '网络连接失败，请检查网络或确认服务已经启动'
    return new AppError('network', message, { cause: error })
  }
  if (status === 401) return new AppError('auth', '登录状态已失效，请重新登录', { status, cause: error })
  if (status && (SERVICE_ERROR_STATUSES.has(status) || status >= 500)) {
    return new AppError('service', safeServerMessage || '服务暂时不可用，请稍后重试', { status, cause: error })
  }
  return new AppError('http', safeServerMessage || `请求失败（${status ?? '未知状态'}）`, { status, cause: error })
}

export const http = axios.create({
  baseURL: '/api',
  timeout: 15_000,
  headers: { 'Content-Type': 'application/json' },
})

http.interceptors.request.use((config) => {
  const token = readSession()?.token
  if (token) config.headers.set(ADMIN_TOKEN_HEADER, token)
  if (config.data instanceof FormData) config.headers.delete('Content-Type')
  return config
})

http.interceptors.response.use(
  (response) => {
    if (response.config.responseType === 'blob') return downloadFromResponse(response as AxiosResponse<Blob>)
    if (!isApiResult(response.data)) return response.data
    if (response.data.code !== 1) {
      throw new AppError('business', response.data.msg || '业务处理失败', { businessCode: response.data.code })
    }
    return response.data.data
  },
  async (error: unknown) => {
    if (isAppError(error)) return Promise.reject(error)
    if (!axios.isAxiosError<ApiResult<unknown>>(error)) {
      return Promise.reject(new AppError('http', '请求处理失败', { cause: error }))
    }

    const normalized = normalizeAxiosError(error)
    if (normalized.kind === 'auth') await handleUnauthorized()
    return Promise.reject(normalized)
  },
)

export function request<T>(config: AxiosRequestConfig): Promise<T> {
  return http.request(config) as unknown as Promise<T>
}

export function download(config: AxiosRequestConfig): Promise<FileDownload> {
  return http.request({ ...config, responseType: 'blob' }) as unknown as Promise<FileDownload>
}

export { ADMIN_TOKEN_HEADER }
