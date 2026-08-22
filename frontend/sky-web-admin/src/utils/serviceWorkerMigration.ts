const LEGACY_CACHE_PREFIX = ['vue', 'typescript', 'admin', 'template'].join('-')
const MIGRATION_MARKER = 'yida:admin:sw-migration:v1'

interface WorkerReference {
  scriptURL: string
}

export interface LegacyWorkerRegistration {
  scope: string
  active?: WorkerReference | null
  waiting?: WorkerReference | null
  installing?: WorkerReference | null
  unregister: () => Promise<boolean>
}

interface MarkerStorage {
  getItem: (key: string) => string | null
  setItem: (key: string, value: string) => void
}

export interface ServiceWorkerMigrationDependencies {
  origin: string
  markerStorage: MarkerStorage
  getRegistrations: () => Promise<readonly LegacyWorkerRegistration[]>
  getCacheNames: () => Promise<string[]>
  deleteCache: (name: string) => Promise<boolean>
  now?: () => string
}

export interface ServiceWorkerMigrationResult {
  skipped: boolean
  deletedCacheNames: string[]
  unregisteredScopes: string[]
}

export function isLegacyAdminCacheName(name: string) {
  return name === LEGACY_CACHE_PREFIX || name.startsWith(`${LEGACY_CACHE_PREFIX}-`)
}

function isLegacyRootRegistration(registration: LegacyWorkerRegistration, origin: string) {
  const rootScope = new URL('/', origin).href
  if (registration.scope !== rootScope) return false

  return [registration.active, registration.waiting, registration.installing]
    .filter((worker): worker is WorkerReference => Boolean(worker))
    .some((worker) => {
      const script = new URL(worker.scriptURL, origin)
      return script.origin === origin && script.pathname === '/service-worker.js'
    })
}

export async function migrateLegacyAdminServiceWorker(
  dependencies: ServiceWorkerMigrationDependencies,
): Promise<ServiceWorkerMigrationResult> {
  if (dependencies.markerStorage.getItem(MIGRATION_MARKER)) {
    return { skipped: true, deletedCacheNames: [], unregisteredScopes: [] }
  }

  const [cacheNames, registrations] = await Promise.all([
    dependencies.getCacheNames(),
    dependencies.getRegistrations(),
  ])
  const legacyCacheNames = cacheNames.filter(isLegacyAdminCacheName)
  const deletedCacheNames: string[] = []
  for (const name of legacyCacheNames) {
    if (await dependencies.deleteCache(name)) deletedCacheNames.push(name)
  }

  const legacyRegistrations = registrations.filter((registration) => (
    isLegacyRootRegistration(registration, dependencies.origin)
  ))
  const unregisteredScopes: string[] = []
  for (const registration of legacyRegistrations) {
    if (await registration.unregister()) unregisteredScopes.push(registration.scope)
  }

  dependencies.markerStorage.setItem(MIGRATION_MARKER, JSON.stringify({
    completedAt: (dependencies.now ?? (() => new Date().toISOString()))(),
    deletedCacheNames,
    unregisteredScopes,
  }))

  return { skipped: false, deletedCacheNames, unregisteredScopes }
}

let migrationPromise: Promise<ServiceWorkerMigrationResult> | null = null

export function runLegacyServiceWorkerMigration() {
  if (migrationPromise) return migrationPromise
  if (typeof window === 'undefined' || !('serviceWorker' in navigator) || !('caches' in window)) {
    return Promise.resolve<ServiceWorkerMigrationResult>({
      skipped: true,
      deletedCacheNames: [],
      unregisteredScopes: [],
    })
  }

  migrationPromise = migrateLegacyAdminServiceWorker({
    origin: window.location.origin,
    markerStorage: window.localStorage,
    getRegistrations: () => navigator.serviceWorker.getRegistrations(),
    getCacheNames: () => window.caches.keys(),
    deleteCache: (name) => window.caches.delete(name),
  }).catch(() => ({ skipped: false, deletedCacheNames: [], unregisteredScopes: [] }))

  return migrationPromise
}

export { LEGACY_CACHE_PREFIX, MIGRATION_MARKER }
