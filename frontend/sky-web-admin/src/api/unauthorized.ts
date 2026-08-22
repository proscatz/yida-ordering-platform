type UnauthorizedHandler = () => void | Promise<void>

let handler: UnauthorizedHandler | null = null
let handling: Promise<void> | null = null
let handledForCurrentSession = false

export function configureUnauthorizedHandler(nextHandler: UnauthorizedHandler) {
  handler = nextHandler
}

export function markAuthenticatedSession() {
  handledForCurrentSession = false
}

export function handleUnauthorized(): Promise<void> {
  if (handledForCurrentSession) return handling ?? Promise.resolve()
  if (handling) return handling

  handledForCurrentSession = true
  handling = Promise.resolve(handler?.()).finally(() => {
    handling = null
  })
  return handling
}

export function resetUnauthorizedStateForTests() {
  handler = null
  handling = null
  handledForCurrentSession = false
}
