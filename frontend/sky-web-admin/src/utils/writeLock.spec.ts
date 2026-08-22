import { beforeEach, describe, expect, it } from 'vitest'
import { resetWriteLocksForTests, withWriteLock } from './writeLock'

describe('explicit write locks', () => {
  beforeEach(resetWriteLocksForTests)

  it('blocks only another write with the same explicit key', async () => {
    let finish!: (value: string) => void
    const first = withWriteLock('dish:save:42', () => new Promise<string>((resolve) => { finish = resolve }))

    await expect(withWriteLock('dish:save:42', async () => 'duplicate')).rejects.toMatchObject({ kind: 'duplicate' })
    await expect(withWriteLock('dish:save:43', async () => 'different-resource')).resolves.toBe('different-resource')

    finish('saved')
    await expect(first).resolves.toBe('saved')
    await expect(withWriteLock('dish:save:42', async () => 'next-write')).resolves.toBe('next-write')
  })
})
