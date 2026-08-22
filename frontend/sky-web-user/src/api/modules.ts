import { request } from './http'
import type {
  Address, AddressPayload, AddressUpdatePayload, CartItem, Category, Dish, DishItem, Id, LoginPayload, LoginResult,
  Order, PageResult, PaymentResult, Setmeal, SubmitOrderPayload, SubmitOrderResult,
} from '@/types'

export const authApi = {
  login: (payload: LoginPayload) => request<LoginResult>({ url: '/user/user/password-login', method: 'post', data: payload, dedupe: true }),
  logout: () => request<void>({ url: '/user/user/logout', method: 'post', dedupe: true }),
}

export const catalogApi = {
  categories: (type?: 1 | 2) => request<Category[]>({ url: '/user/category/list', params: { type } }),
  dishes: (categoryId: Id) => request<Dish[]>({ url: '/user/dish/list', params: { categoryId } }),
  setmeals: (categoryId: Id) => request<Setmeal[]>({ url: '/user/setmeal/list', params: { categoryId } }),
  setmealDishes: (id: Id) => request<DishItem[]>({ url: `/user/setmeal/dish/${id}` }),
  shopStatus: () => request<number>({ url: '/user/shop/status' }),
}

export const cartApi = {
  list: () => request<CartItem[]>({ url: '/user/shoppingCart/list' }),
  addDish: (dishId: Id, dishFlavor?: string) => request<void>({ url: '/user/shoppingCart/add', method: 'post', data: { dishId, dishFlavor } }),
  addSetmeal: (setmealId: Id) => request<void>({ url: '/user/shoppingCart/add', method: 'post', data: { setmealId } }),
  sub: (payload: { dishId?: Id; setmealId?: Id; dishFlavor?: string }) => request<void>({ url: '/user/shoppingCart/sub', method: 'post', data: payload }),
  clean: () => request<void>({ url: '/user/shoppingCart/clean', method: 'delete', dedupe: true }),
}

export const addressApi = {
  list: () => request<Address[]>({ url: '/user/addressBook/list' }),
  detail: (id: Id) => request<Address>({ url: `/user/addressBook/${id}` }),
  getDefault: () => request<Address>({ url: '/user/addressBook/default' }),
  create: (data: AddressPayload) => request<Id>({ url: '/user/addressBook', method: 'post', data, dedupe: true }),
  update: (data: AddressUpdatePayload) => request<void>({ url: '/user/addressBook', method: 'put', data, dedupe: true }),
  setDefault: (id: Id) => request<void>({ url: '/user/addressBook/default', method: 'put', data: { id }, dedupe: true }),
  remove: (id: Id) => request<void>({ url: '/user/addressBook', method: 'delete', params: { id }, dedupe: true }),
}

export const orderApi = {
  submit: (data: SubmitOrderPayload) => request<SubmitOrderResult>({ url: '/user/order/submit', method: 'post', data, dedupe: true }),
  payment: (orderNumber: string, payMethod: number) => request<PaymentResult>({ url: '/user/order/payment', method: 'put', data: { orderNumber, payMethod }, dedupe: true }),
  list: (page: number, pageSize: number, status?: number) => request<PageResult<Order>>({ url: '/user/order/historyOrders', params: { page, pageSize, status } }),
  detail: (id: Id) => request<Order>({ url: `/user/order/orderDetail/${id}` }),
  cancel: (id: Id) => request<void>({ url: `/user/order/cancel/${id}`, method: 'put', dedupe: true }),
  remind: (id: Id) => request<void>({ url: `/user/order/reminder/${id}`, dedupe: true }),
  repeat: (id: Id) => request<void>({ url: `/user/order/repetition/${id}`, method: 'post', dedupe: true }),
}
