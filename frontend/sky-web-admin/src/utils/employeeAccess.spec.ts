import { describe, expect, it } from 'vitest'
import { canManageEmployees, employeeEditBlockReason, employeeStatusBlockReason } from './employeeAccess'
import type { Employee } from '@/types/management'

const employee = (overrides: Partial<Employee> = {}): Employee => ({
  id: 2,
  username: 'staff',
  name: '普通员工',
  phone: '13800000000',
  sex: '1',
  idNumber: '110101199001011234',
  status: 1,
  role: 'EMPLOYEE',
  createTime: '2026-08-22 10:00:00',
  updateTime: '2026-08-22 10:00:00',
  createUser: 1,
  updateUser: 1,
  ...overrides,
})

describe('employee access presentation rules', () => {
  it('only exposes employee management to administrators', () => {
    expect(canManageEmployees('ADMIN')).toBe(true)
    expect(canManageEmployees('EMPLOYEE')).toBe(false)
  })

  it('blocks status changes for the current account', () => {
    expect(employeeStatusBlockReason(employee(), 2)).toContain('自己')
  })

  it('blocks status and profile changes for administrators', () => {
    const administrator = employee({ id: 1, role: 'ADMIN' })
    expect(employeeStatusBlockReason(administrator, 2)).toContain('管理员')
    expect(employeeEditBlockReason(administrator)).toContain('管理员')
  })

  it('allows an administrator UI to act on an ordinary employee', () => {
    expect(employeeStatusBlockReason(employee(), 1)).toBe('')
    expect(employeeEditBlockReason(employee())).toBe('')
  })
})
