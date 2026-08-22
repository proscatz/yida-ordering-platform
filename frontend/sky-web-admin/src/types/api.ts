export interface ApiResult<T> {
  code: number
  msg?: string
  data?: T
}

export interface LoginPayload {
  username: string
  password: string
}

export interface AdminSession {
  id: number
  userName: string
  name: string
  role: AdminRole
  token: string
}

export type AdminRole = 'ADMIN' | 'EMPLOYEE'

export type Nullable<T> = T | null
export type Optional<T> = T | null | undefined
export type DateString = string
export type DateTimeString = string
export type Money = number

export interface PaginationQuery {
  page: number
  pageSize: number
}

export interface PageResult<T> {
  total: number
  records: T[]
}

export interface FileDownload {
  blob: Blob
  fileName: string | null
  contentType: string
}
