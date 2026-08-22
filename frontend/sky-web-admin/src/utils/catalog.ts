import type { DishFlavorRecord, FlavorDraft, SetmealDishRelation } from '@/types/catalog'

export const imageUploadRules = {
  maxBytes: 5 * 1024 * 1024,
  allowed: new Map<string, Set<string>>([
    ['jpg', new Set(['image/jpeg'])],
    ['jpeg', new Set(['image/jpeg'])],
    ['png', new Set(['image/png'])],
    ['webp', new Set(['image/webp'])],
    ['gif', new Set(['image/gif'])],
  ]),
}

export function validateImageFile(file: File): string | null {
  if (!file.size) return '不能上传空文件'
  if (file.size > imageUploadRules.maxBytes) return '图片大小不能超过 5MB'
  const extension = file.name.split('.').pop()?.toLowerCase()
  if (!extension || !imageUploadRules.allowed.get(extension)?.has(file.type)) {
    return '仅支持 JPG、PNG、WEBP 或 GIF 图片'
  }
  return null
}

export async function validateImageFileSignature(file: File): Promise<string | null> {
  const basicError = validateImageFile(file)
  if (basicError) return basicError

  const extension = file.name.split('.').pop()?.toLowerCase()
  const expectedMime = extension ? imageUploadRules.allowed.get(extension) : undefined
  const bytes = await readBlobBytes(file.slice(0, 12))
  const detectedMime = detectImageMime(bytes)
  if (!detectedMime || !expectedMime?.has(detectedMime)) return '图片内容与扩展名不一致'
  return null
}

export function detectImageMime(bytes: Uint8Array): string | null {
  if (startsWith(bytes, [0xff, 0xd8, 0xff])) return 'image/jpeg'
  if (startsWith(bytes, [0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a])) return 'image/png'
  const ascii = (offset: number, text: string) => [...text].every((char, index) => bytes[offset + index] === char.charCodeAt(0))
  if (bytes.length >= 6 && (ascii(0, 'GIF87a') || ascii(0, 'GIF89a'))) return 'image/gif'
  if (bytes.length >= 12 && ascii(0, 'RIFF') && ascii(8, 'WEBP')) return 'image/webp'
  return null
}

function startsWith(bytes: Uint8Array, signature: number[]) {
  return bytes.length >= signature.length && signature.every((value, index) => bytes[index] === value)
}

function readBlobBytes(blob: Blob): Promise<Uint8Array> {
  if (typeof blob.arrayBuffer === 'function') {
    return blob.arrayBuffer().then((buffer) => new Uint8Array(buffer))
  }
  return new Promise((resolve, reject) => {
    const reader = new FileReader()
    reader.onload = () => reader.result instanceof ArrayBuffer
      ? resolve(new Uint8Array(reader.result))
      : reject(new Error('无法读取图片内容'))
    reader.onerror = () => reject(new Error('无法读取图片内容'))
    reader.readAsArrayBuffer(blob)
  })
}

export function parseFlavorRecords(records: DishFlavorRecord[] = []): FlavorDraft[] {
  return records.map((record, index) => {
    let values: string[] = []
    try {
      const parsed = JSON.parse(record.value) as unknown
      values = Array.isArray(parsed) ? parsed.filter((item): item is string => typeof item === 'string') : []
    } catch {
      values = record.value.trim() ? [record.value.trim()] : []
    }
    return { key: `${record.id ?? 'new'}-${index}`, name: record.name, values }
  })
}

export function validateFlavorDrafts(drafts: FlavorDraft[]): string | null {
  const names = new Set<string>()
  for (const draft of drafts) {
    const name = draft.name.trim()
    if (!name) return '规格名称不能为空'
    const normalizedName = name.toLocaleLowerCase()
    if (names.has(normalizedName)) return `规格名称“${name}”重复`
    names.add(normalizedName)
    if (!draft.values.length) return `请为“${name}”添加至少一个规格值`
    const values = draft.values.map((value) => value.trim())
    if (values.some((value) => !value)) return `“${name}”中存在空规格值`
    if (new Set(values.map((value) => value.toLocaleLowerCase())).size !== values.length) return `“${name}”中存在重复规格值`
  }
  return null
}

export function serializeFlavorDrafts(drafts: FlavorDraft[]): DishFlavorRecord[] {
  return drafts.map((draft) => ({ name: draft.name.trim(), value: JSON.stringify(draft.values.map((value) => value.trim())) }))
}

export function normalizeSetmealDishes(relations: SetmealDishRelation[] = []): SetmealDishRelation[] {
  return relations.map((relation) => ({
    dishId: relation.dishId,
    name: relation.name,
    price: relation.price,
    copies: relation.copies,
  }))
}

export function validateSetmealDishes(relations: SetmealDishRelation[]): string | null {
  if (!relations.length) return '请至少选择一道套餐菜品'
  if (new Set(relations.map((relation) => relation.dishId)).size !== relations.length) return '套餐中不能重复添加同一道菜品'
  if (relations.some((relation) => !Number.isInteger(relation.copies) || relation.copies < 1)) return '套餐菜品份数必须为正整数'
  return null
}
