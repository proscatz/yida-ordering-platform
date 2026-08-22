import { AppError } from '@/api/errors'

const activeWrites = new Map<string, Promise<unknown>>()

export function withWriteLock<T>(key: string, action: () => Promise<T>): Promise<T> {
  if (activeWrites.has(key)) {
    return Promise.reject(new AppError('duplicate', '操作正在处理中，请勿重复提交'))
  }

  const task = Promise.resolve().then(action)
  activeWrites.set(key, task)
  return task.finally(() => {
    if (activeWrites.get(key) === task) activeWrites.delete(key)
  })
}

export function resetWriteLocksForTests() {
  activeWrites.clear()
}
