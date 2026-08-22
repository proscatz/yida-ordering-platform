import type { AxiosProgressEvent } from 'axios'
import { request } from './http'

export function uploadImage(file: File, onProgress?: (percentage: number) => void) {
  const data = new FormData()
  data.append('file', file)
  data.append('scope', import.meta.env.DEV ? 'diagnostic' : 'catalog')
  return request<string>({
    method: 'POST',
    url: '/common/upload',
    data,
    onUploadProgress: (event: AxiosProgressEvent) => {
      if (event.total) onProgress?.(Math.min(100, Math.round((event.loaded / event.total) * 100)))
    },
  })
}
