import { describe, expect, it } from 'vitest'
import { safeResultMessage } from './http'

describe('safe server error messages', () => {
  it('keeps a structured safe backend message for upload failures', () => {
    expect(safeResultMessage({ code: 0, msg: '图片存储连接超时，请稍后重试' }))
      .toBe('图片存储连接超时，请稍后重试')
  })

  it('ignores HTML and malformed upstream responses', () => {
    expect(safeResultMessage('<html>bad gateway</html>')).toBeNull()
    expect(safeResultMessage({ code: 0, msg: '' })).toBeNull()
  })
})
