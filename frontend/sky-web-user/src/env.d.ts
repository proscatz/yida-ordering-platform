/// <reference types="vite/client" />

interface ImportMetaEnv {
  readonly VITE_API_TARGET?: string
  readonly VITE_PAYMENT_PROVIDER?: 'mock' | 'real'
}

interface ImportMeta {
  readonly env: ImportMetaEnv
}