self.addEventListener('install', () => {
  self.skipWaiting()
})

const legacyCachePrefix = ['vue', 'typescript', 'admin', 'template'].join('-')

function isLegacyAdminCache(name) {
  return name === legacyCachePrefix || name.startsWith(`${legacyCachePrefix}-`)
}

self.addEventListener('activate', (event) => {
  event.waitUntil((async () => {
    await self.clients.claim()

    const cacheNames = await caches.keys()
    const obsoleteCacheNames = cacheNames.filter(isLegacyAdminCache)
    await Promise.all(obsoleteCacheNames.map((name) => caches.delete(name)))
    await self.registration.unregister()

    const windows = await self.clients.matchAll({ type: 'window' })
    await Promise.all(windows.map((client) => client.navigate(client.url)))
  })())
})
