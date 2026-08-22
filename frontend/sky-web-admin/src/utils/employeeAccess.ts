import type { AdminRole } from '@/types/api'
import type { Employee } from '@/types/management'

export function canManageEmployees(role?: AdminRole) {
  return role === 'ADMIN'
}

export function employeeStatusBlockReason(employee: Employee, currentEmployeeId?: number) {
  if (employee.id === currentEmployeeId) return '不能修改自己的启用状态'
  if (employee.role === 'ADMIN') return '管理员账号不能通过员工状态操作修改'
  return ''
}

export function employeeEditBlockReason(employee: Employee) {
  return employee.role === 'ADMIN' ? '管理员资料不能通过普通员工编辑入口修改' : ''
}
