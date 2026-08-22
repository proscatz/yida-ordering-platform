export function createOrderDataRefresher(
  loadOrders: () => Promise<unknown>,
  loadStatistics: () => Promise<unknown>,
) {
  let active: Promise<void> | null = null

  return function refreshOrderData() {
    if (active) return active
    active = Promise.all([loadOrders(), loadStatistics()]).then(() => undefined)
    return active.finally(() => {
      active = null
    })
  }
}
