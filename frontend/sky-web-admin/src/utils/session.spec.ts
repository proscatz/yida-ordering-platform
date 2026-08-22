import { beforeEach, describe, expect, it } from 'vitest'
import { readSession, sessionStorageKey } from './session'

describe('JWT session restoration', () => {
  beforeEach(() => localStorage.clear())

  it('rejects an expired persisted JWT session', () => {
    const payload = btoa(JSON.stringify({ exp: 1 }))
    localStorage.setItem(sessionStorageKey, JSON.stringify({
      id: 1,
      userName: 'expired',
      name: '过期账号',
      token: `header.${payload}.signature`,
    }))

    expect(readSession()).toBeNull()
    expect(localStorage.getItem(sessionStorageKey)).toBeNull()
  })
})
