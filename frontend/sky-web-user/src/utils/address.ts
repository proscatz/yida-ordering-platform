export type AddressField = 'consignee' | 'phone' | 'sex' | 'region' | 'detail' | 'label'

export interface SelectedAreaOption {
  text?: string
  value?: string | number
}

export interface AreaTarget {
  provinceCode: string
  provinceName: string
  cityCode: string
  cityName: string
  districtCode: string
  districtName: string
}

export const controlCharacters = /[\u0000-\u001f\u007f-\u009f]/

export function applyAreaSelection(target: AreaTarget, selectedOptions: SelectedAreaOption[]): boolean {
  if (selectedOptions.length !== 3) return false
  const [province, city, district] = selectedOptions
  if (!province?.value || !province.text || !city?.value || !city.text || !district?.value || !district.text) return false
  target.provinceCode = String(province.value)
  target.provinceName = province.text
  target.cityCode = String(city.value)
  target.cityName = city.text
  target.districtCode = String(district.value)
  target.districtName = district.text
  return true
}

export function normalizeAddressFieldErrors(errors: Record<string, string>): Partial<Record<AddressField, string>> {
  const normalized: Partial<Record<AddressField, string>> = {}
  const regionKeys = ['provinceCode', 'provinceName', 'cityCode', 'cityName', 'districtCode', 'districtName', 'region']
  const regionError = regionKeys.map((key) => errors[key]).find(Boolean)
  if (regionError) normalized.region = regionError
  for (const field of ['consignee', 'phone', 'sex', 'detail', 'label'] as AddressField[]) {
    if (errors[field]) normalized[field] = errors[field]
  }
  return normalized
}
