import type { AdminRole, DateTimeString, Money, PaginationQuery } from './api'

export type EnableStatus = 0 | 1
export type CategoryType = 1 | 2

export interface BusinessData {
  turnover: Money
  validOrderCount: number
  orderCompletionRate: number
  unitPrice: Money
  newUsers: number
}

export interface OrderOverview {
  waitingOrders: number
  deliveredOrders: number
  completedOrders: number
  cancelledOrders: number
  allOrders: number
}

export interface ProductOverview {
  sold: number
  discontinued: number
}

export interface Employee {
  id: number
  username: string
  name: string
  phone: string
  sex: string
  idNumber: string
  status: EnableStatus
  role: AdminRole
  createTime: DateTimeString
  updateTime: DateTimeString
  createUser: number
  updateUser: number
}

export interface EmployeeProfile {
  id: number
  username: string
  name: string
  phone: string
  sex: string
  status: EnableStatus
  role: AdminRole
}

export interface EmployeePayload {
  id?: number
  username: string
  name: string
  phone: string
  sex: string
  idNumber: string
}

export interface EmployeePageQuery extends PaginationQuery {
  name?: string
}

export interface Category {
  id: number
  type: CategoryType
  name: string
  sort: number
  status: EnableStatus
  createTime: DateTimeString
  updateTime: DateTimeString
  createUser: number
  updateUser: number
}

export interface CategoryPayload {
  id?: number
  type: CategoryType
  name: string
  sort: number
}

export interface CategoryPageQuery extends PaginationQuery {
  name?: string
  type?: CategoryType
}
