import { describe, expect, it, vi } from 'vitest'
import {
  LEGACY_CACHE_PREFIX,
  MIGRATION_MARKER,
  isLegacyAdminCacheName,
  migrateLegacyAdminServiceWorker,
  type LegacyWorkerRegistration,
} from './serviceWorkerMigration'

function registration(scope: string, scriptURL: string) {
  return {
    scope,
    active: { scriptURL },
    unregister: vi.fn(async () => true),
  } satisfies LegacyWorkerRegistration
}

describe('service worker migration', () => {
  it('only recognizes the confirmed legacy admin cache prefix', () => {
    expect(isLegacyAdminCacheName(LEGACY_CACHE_PREFIX)).toBe(true)
    expect(isLegacyAdminCacheName(`${LEGACY_CACHE_PREFIX}-precache-v2-http://localhost/`)).toBe(true)
    expect(isLegacyAdminCacheName('workbox-precache-v2-http://localhost/')).toBe(false)
    expect(isLegacyAdminCacheName('yida-user-runtime')).toBe(false)
  })

  it('deletes only legacy caches and unregisters only the matching root worker', async () => {
    const marker = new Map<string, string>()
    const rootWorker = registration('http://localhost/', 'http://localhost/service-worker.js')
    const userWorker = registration('http://localhost/user/', 'http://localhost/user/service-worker.js')
    const deleteCache = vi.fn(async () => true)

    const result = await migrateLegacyAdminServiceWorker({
      origin: 'http://localhost',
      markerStorage: {
        getItem: (key) => marker.get(key) ?? null,
        setItem: (key, value) => marker.set(key, value),
      },
      getRegistrations: async () => [rootWorker, userWorker],
      getCacheNames: async () => [
        `${LEGACY_CACHE_PREFIX}-precache-v2-http://localhost/`,
        'yida-user-runtime',
      ],
      deleteCache,
      now: () => '2026-08-21T00:00:00.000Z',
    })

    expect(deleteCache).toHaveBeenCalledOnce()
    expect(deleteCache).toHaveBeenCalledWith(`${LEGACY_CACHE_PREFIX}-precache-v2-http://localhost/`)
    expect(rootWorker.unregister).toHaveBeenCalledOnce()
    expect(userWorker.unregister).not.toHaveBeenCalled()
    expect(result.deletedCacheNames).toEqual([`${LEGACY_CACHE_PREFIX}-precache-v2-http://localhost/`])
    expect(result.unregisteredScopes).toEqual(['http://localhost/'])
    expect(marker.has(MIGRATION_MARKER)).toBe(true)
  })

  it('does not repeat cache or registration scans after completion', async () => {
    const getCacheNames = vi.fn(async () => [])
    const getRegistrations = vi.fn(async () => [])
    const result = await migrateLegacyAdminServiceWorker({
      origin: 'http://localhost',
      markerStorage: {
        getItem: () => 'completed',
        setItem: vi.fn(),
      },
      getRegistrations,
      getCacheNames,
      deleteCache: vi.fn(async () => true),
    })

    expect(result.skipped).toBe(true)
    expect(getCacheNames).not.toHaveBeenCalled()
    expect(getRegistrations).not.toHaveBeenCalled()
  })

  it('leaves caches with unknown ownership untouched', async () => {
    const deleteCache = vi.fn(async () => true)
    await migrateLegacyAdminServiceWorker({
      origin: 'http://localhost',
      markerStorage: { getItem: () => null, setItem: vi.fn() },
      getRegistrations: async () => [],
      getCacheNames: async () => ['precache-unowned', 'runtime-another-app'],
      deleteCache,
    })

    expect(deleteCache).not.toHaveBeenCalled()
  })
})
