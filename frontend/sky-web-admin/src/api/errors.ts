export type RequestErrorKind = 'auth' | 'business' | 'canceled' | 'duplicate' | 'http' | 'network' | 'service'

export class AppError extends Error {
  readonly kind: RequestErrorKind
  readonly status?: number
  readonly businessCode?: number
  readonly original?: unknown

  constructor(kind: RequestErrorKind, message: string, options?: { status?: number; businessCode?: number; cause?: unknown }) {
    super(message)
    this.name = 'AppError'
    this.kind = kind
    this.status = options?.status
    this.businessCode = options?.businessCode
    this.original = options?.cause
  }
}

export function isAppError(error: unknown): error is AppError {
  return error instanceof AppError
}

export function userFacingError(error: unknown, fallback = '操作失败，请稍后重试') {
  if (isAppError(error)) return error.message
  return error instanceof Error && error.message ? error.message : fallback
}
