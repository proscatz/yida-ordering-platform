import type { DateTimeString, Money, PaginationQuery } from './api'
import type { EnableStatus } from './management'

export interface DishFlavorRecord {
  id?: number
  dishId?: number
  name: string
  value: string
}

export interface FlavorDraft {
  key: string
  name: string
  values: string[]
}

export interface Dish {
  id: number
  name: string
  categoryId: number
  categoryName: string
  price: Money
  image: string | null
  description: string | null
  status: EnableStatus
  updateTime: DateTimeString
  flavors?: DishFlavorRecord[]
}

export interface DishPayload {
  id?: number
  name: string
  categoryId: number | null
  price: Money
  image: string
  description: string
  status: EnableStatus
  flavors: DishFlavorRecord[]
}

export interface DishPageQuery extends PaginationQuery {
  name?: string
  categoryId?: number
  status?: EnableStatus
}

export interface SetmealDishRelation {
  id?: number
  setmealId?: number
  dishId: number
  name: string
  price: Money
  copies: number
}

export interface Setmeal {
  id: number
  categoryId: number
  categoryName: string
  name: string
  price: Money
  status: EnableStatus
  description: string | null
  image: string | null
  updateTime: DateTimeString
  setmealDishes?: SetmealDishRelation[]
}

export interface SetmealPayload {
  id?: number
  categoryId: number | null
  name: string
  price: Money
  status: EnableStatus
  description: string
  image: string
  setmealDishes: SetmealDishRelation[]
}

export interface SetmealPageQuery extends PaginationQuery {
  name?: string
  categoryId?: number
  status?: EnableStatus
}
