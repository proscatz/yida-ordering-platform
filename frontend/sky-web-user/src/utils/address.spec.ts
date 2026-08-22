import { describe, expect, it } from 'vitest'
import { useCascaderAreaData } from '@vant/area-data'
import { applyAreaSelection, controlCharacters, normalizeAddressFieldErrors } from './address'

describe('address utilities', () => {
  it('uses a complete offline province-city-district hierarchy', () => {
    const provinces = useCascaderAreaData()
    const beijing = provinces.find((item) => item.value === '110000')
    const city = beijing?.children?.find((item) => item.value === '110100')
    expect(provinces.length).toBeGreaterThanOrEqual(34)
    expect(city?.children?.some((item) => item.value === '110101')).toBe(true)
  })

  it('fills both administrative names and codes', () => {
    const target = { provinceCode: '', provinceName: '', cityCode: '', cityName: '', districtCode: '', districtName: '' }
    expect(applyAreaSelection(target, [
      { text: '北京市', value: '110000' },
      { text: '北京市', value: '110100' },
      { text: '东城区', value: '110101' },
    ])).toBe(true)
    expect(target).toEqual({
      provinceCode: '110000', provinceName: '北京市', cityCode: '110100', cityName: '北京市',
      districtCode: '110101', districtName: '东城区',
    })
  })

  it('rejects incomplete cascader selections', () => {
    const target = { provinceCode: '', provinceName: '', cityCode: '', cityName: '', districtCode: '', districtName: '' }
    expect(applyAreaSelection(target, [{ text: '北京市', value: '110000' }])).toBe(false)
  })

  it('maps all backend region errors to the visible region field', () => {
    expect(normalizeAddressFieldErrors({ districtCode: '省市区信息不匹配', phone: '手机号不正确' }))
      .toEqual({ region: '省市区信息不匹配', phone: '手机号不正确' })
  })

  it('detects control characters', () => {
    expect(controlCharacters.test('正常地址')).toBe(false)
    expect(controlCharacters.test('异常\u0000地址')).toBe(true)
  })
})
