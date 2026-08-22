import { orderApi } from '@/api/modules'
import type { Order, PaymentResult } from '@/types'

export interface PaymentAdapter {
  readonly provider: string
  pay(order: Order): Promise<PaymentResult>
}

class MockPaymentAdapter implements PaymentAdapter {
  readonly provider = 'mock'
  pay(order: Order): Promise<PaymentResult> {
    return orderApi.payment(order.number, order.payMethod || 1)
  }
}

export type RealPaymentInvoker = (payment: PaymentResult, order: Order) => Promise<void>
let realPaymentInvoker: RealPaymentInvoker | undefined

export function registerRealPaymentInvoker(invoker: RealPaymentInvoker): void {
  realPaymentInvoker = invoker
}

class RealPaymentAdapter implements PaymentAdapter {
  readonly provider = 'real'
  async pay(order: Order): Promise<PaymentResult> {
    const payment = await orderApi.payment(order.number, order.payMethod || 1)
    if (!realPaymentInvoker) throw new Error('真实支付适配器尚未配置')
    await realPaymentInvoker(payment, order)
    return payment
  }
}

export function paymentAdapter(): PaymentAdapter {
  return import.meta.env.VITE_PAYMENT_PROVIDER === 'real'
    ? new RealPaymentAdapter()
    : new MockPaymentAdapter()
}