export type Id = string | number
export type Money = string | number

export interface ApiResult<T> {
  code: number
  msg?: string
  data: T
}

export interface PageResult<T> {
  total: number
  records: T[]
}

export interface LoginPayload {
  username?: string
  phone?: string
  password: string
}

export interface LoginResult {
  id: Id
  openid?: string
  token: string
}

export interface Category {
  id: Id
  type: 1 | 2
  name: string
  sort: number
  status: number
}

export interface DishFlavor {
  id: Id
  dishId: Id
  name: string
  value: string
}

export interface Dish {
  id: Id
  categoryId: Id
  name: string
  price: Money
  image?: string
  description?: string
  status: number
  flavors?: DishFlavor[]
}

export interface Setmeal {
  id: Id
  categoryId: Id
  name: string
  price: Money
  image?: string
  description?: string
  status: number
}

export interface DishItem {
  name: string
  copies: number
  image?: string
  description?: string
}

export interface CartItem {
  id: Id
  name: string
  dishId?: Id
  setmealId?: Id
  dishFlavor?: string
  number: number
  amount: Money
  image?: string
}

export interface Address {
  id: Id
  consignee: string
  phone: string
  sex: '0' | '1'
  provinceCode: string
  provinceName: string
  cityCode: string
  cityName: string
  districtCode: string
  districtName: string
  detail: string
  label?: string
  isDefault: 0 | 1
}

export interface AddressPayload {
  consignee: string
  phone: string
  sex: '0' | '1'
  provinceCode: string
  provinceName: string
  cityCode: string
  cityName: string
  districtCode: string
  districtName: string
  detail: string
  label?: string
  isDefault: 0 | 1
}

export interface AddressUpdatePayload extends AddressPayload {
  id: Id
}

export interface OrderDetail {
  id: Id
  name: string
  orderId: Id
  dishId?: Id
  setmealId?: Id
  dishFlavor?: string
  number: number
  amount: Money
  image?: string
}

export interface Order {
  id: Id
  number: string
  requestId?: string
  status: number
  payStatus: number
  amount: Money
  payMethod: number
  addressBookId?: Id
  orderTime: string
  checkoutTime?: string
  phone?: string
  address?: string
  consignee?: string
  remark?: string
  cancelReason?: string
  rejectionReason?: string
  cancelTime?: string
  estimatedDeliveryTime?: string
  deliveryStatus?: number
  deliveryTime?: string
  packAmount?: number
  tablewareNumber?: number
  tablewareStatus?: number
  orderDishes?: string
  orderDetailList?: OrderDetail[]
}

export interface SubmitOrderPayload {
  requestId: string
  addressBookId: Id
  payMethod: number
  remark?: string
  estimatedDeliveryTime?: string
  deliveryStatus: number
  tablewareNumber: number
  tablewareStatus: number
  packAmount: number
}

export interface SubmitOrderResult {
  id: Id
  orderNumber?: string
  orderAmount: Money
  orderTime: string
}

export interface PaymentResult {
  nonceStr: string
  paySign: string
  timeStamp: string
  signType: string
  packageStr: string
}
