import { request } from './http'
import type { PageResult } from '@/types/api'
import type { Employee, EmployeePageQuery, EmployeePayload, EmployeeProfile, EnableStatus } from '@/types/management'

export const employeeApi = {
  page: (params: EmployeePageQuery) => request<PageResult<Employee>>({ method: 'GET', url: '/employee/page', params }),
  detail: (id: number) => request<Employee>({ method: 'GET', url: `/employee/${id}` }),
  me: () => request<EmployeeProfile>({ method: 'GET', url: '/employee/me' }),
  create: (data: EmployeePayload) => request<void>({ method: 'POST', url: '/employee', data }),
  update: (data: EmployeePayload) => request<void>({ method: 'PUT', url: '/employee', data }),
  setStatus: (id: number, status: EnableStatus) => request<void>({
    method: 'POST',
    url: `/employee/status/${status}`,
    params: { id },
  }),
}
