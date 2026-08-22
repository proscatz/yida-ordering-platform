<template>
  <div class="page home-page">
    <header class="mobile-brand"><BrandLogo /></header>
    <section class="hero">
      <div>
        <span class="hero__eyebrow">TODAY’S PICK</span>
        <h1>预约一餐，<br />把时间留给重要的事。</h1>
        <div class="shop-chip" :class="{ closed: shopStatus !== 1 }">
          <span class="status-dot" />{{ shopStatus === 1 ? '门店营业中，可预约下单' : '门店休息中，请稍后再来' }}
        </div>
      </div>
      <div class="hero__mark">驿<br />达</div>
    </section>

    <section class="ordering-area page-card">
      <aside class="category-list" aria-label="餐品分类">
        <button v-for="category in categories" :key="String(category.id)" type="button"
          :class="{ active: String(category.id) === String(activeCategoryId) }" @click="activeCategoryId = category.id">
          <span>{{ category.name }}</span><small>{{ category.type === 1 ? '单品' : '套餐' }}</small>
        </button>
      </aside>

      <div class="product-panel">
        <div class="product-panel__header">
          <div><h2>{{ activeCategory?.name || '今日餐单' }}</h2><p>{{ activeCategory?.type === 2 ? '搭配省心，按时享用' : '现点现做，安心选味' }}</p></div>
          <span>{{ products.length }} 款</span>
        </div>
        <div v-if="loadingProducts" class="product-grid">
          <div v-for="i in 4" :key="i" class="product-card"><van-skeleton image :row="2" /></div>
        </div>
        <EmptyState v-else-if="!products.length" title="本分类正在备餐" description="看看其他分类吧" icon="shop-collect-o" />
        <div v-else class="product-grid">
          <article v-for="product in products" :key="`${product.kind}-${product.item.id}`" class="product-card" @click="openProduct(product)">
            <div class="product-card__image"><ProductImage :src="product.item.image" :alt="product.item.name" /></div>
            <div class="product-card__body">
              <div><h3>{{ product.item.name }}</h3><p>{{ product.item.description || (product.kind === 'setmeal' ? '精选搭配套餐' : '新鲜制作，按约履约') }}</p></div>
              <div class="product-card__foot">
                <span class="price">{{ money(product.item.price) }}</span>
                <button type="button" class="add-button" aria-label="加入购物车" :disabled="shopStatus !== 1" @click.stop="quickAdd(product)">
                  <van-icon name="plus" />
                </button>
              </div>
            </div>
          </article>
        </div>
      </div>
    </section>

    <button class="cart-bar" type="button" @click="showCart = true">
      <span class="cart-bar__icon"><van-icon name="shopping-cart-o" /><van-badge v-if="cart.count" :content="cart.count" /></span>
      <span class="cart-bar__price"><small>合计</small><strong class="price">{{ money(cart.total) }}</strong></span>
      <span class="cart-bar__action" :class="{ disabled: !cart.count || shopStatus !== 1 }" @click.stop="goCheckout">
        {{ cart.count ? '去结算' : '请选择餐品' }}
      </span>
    </button>

    <van-popup v-model:show="showFlavor" round position="bottom" class="detail-popup safe-bottom">
      <div v-if="selectedDish" class="detail-content">
        <div class="detail-cover"><ProductImage :src="selectedDish.image" :alt="selectedDish.name" /></div>
        <h2>{{ selectedDish.name }}</h2><p class="muted">{{ selectedDish.description }}</p>
        <div v-for="flavor in selectedDish.flavors" :key="String(flavor.id)" class="flavor-group">
          <h4>{{ flavor.name }}</h4>
          <button v-for="option in parseFlavorOptions(flavor.value)" :key="option" type="button"
            :class="{ active: flavorSelection[String(flavor.id)] === option }" @click="flavorSelection[String(flavor.id)] = option">{{ option }}</button>
        </div>
        <div class="popup-submit"><span class="price">{{ money(selectedDish.price) }}</span><van-button type="primary" class="brand-button" :loading="adding" @click="addSelectedDish">加入购物车</van-button></div>
      </div>
    </van-popup>

    <van-popup v-model:show="showSetmeal" round position="bottom" class="detail-popup safe-bottom">
      <div v-if="selectedSetmeal" class="detail-content">
        <div class="detail-cover"><ProductImage :src="selectedSetmeal.image" :alt="selectedSetmeal.name" /></div>
        <h2>{{ selectedSetmeal.name }}</h2><p class="muted">{{ selectedSetmeal.description || '精选组合，省心搭配' }}</p>
        <h4>套餐包含</h4>
        <div v-if="loadingSetmeal" class="muted">正在加载套餐内容…</div>
        <div v-for="item in setmealDishes" :key="item.name" class="setmeal-item"><span>{{ item.name }}</span><b>× {{ item.copies }}</b></div>
        <div class="popup-submit"><span class="price">{{ money(selectedSetmeal.price) }}</span><van-button type="primary" class="brand-button" :loading="adding" @click="addSelectedSetmeal">加入购物车</van-button></div>
      </div>
    </van-popup>

    <van-popup v-model:show="showCart" round position="bottom" class="cart-popup safe-bottom">
      <div class="cart-popup__header"><div><h2>已选餐品</h2><span>{{ cart.count }} 件</span></div><button type="button" @click="cleanCart"><van-icon name="delete-o" /> 清空</button></div>
      <EmptyState v-if="!cart.items.length" title="购物车还是空的" description="选几样喜欢的餐品吧" icon="shopping-cart-o" />
      <div v-else class="cart-items">
        <div v-for="item in cart.items" :key="String(item.id)" class="cart-item">
          <div class="cart-item__image"><ProductImage :src="item.image" :alt="item.name" /></div>
          <div class="cart-item__info"><strong>{{ item.name }}</strong><small>{{ item.dishFlavor || '标准规格' }}</small><span class="price">{{ money(item.amount) }}</span></div>
          <div class="quantity"><button @click="changeQuantity(item, false)">−</button><span>{{ item.number }}</span><button @click="changeQuantity(item, true)">+</button></div>
        </div>
      </div>
      <div class="cart-checkout"><div><small>合计</small><strong class="price">{{ money(cart.total) }}</strong></div><van-button type="primary" class="brand-button" :disabled="!cart.count" @click="goCheckout">确认餐品</van-button></div>
    </van-popup>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { Badge as VanBadge, Button as VanButton, Icon as VanIcon, Popup as VanPopup, Skeleton as VanSkeleton, showConfirmDialog, showToast } from 'vant'
import BrandLogo from '@/components/BrandLogo.vue'
import ProductImage from '@/components/ProductImage.vue'
import EmptyState from '@/components/EmptyState.vue'
import { catalogApi } from '@/api/modules'
import { errorMessage } from '@/api/http'
import { useCartStore } from '@/stores/cart'
import { money, parseFlavorOptions } from '@/utils/format'
import type { CartItem, Category, Dish, DishItem, Id, Setmeal } from '@/types'

type Product = { kind: 'dish'; item: Dish } | { kind: 'setmeal'; item: Setmeal }
const router = useRouter()
const cart = useCartStore()
const categories = ref<Category[]>([])
const activeCategoryId = ref<Id>('')
const activeCategory = computed(() => categories.value.find((item) => String(item.id) === String(activeCategoryId.value)))
const products = ref<Product[]>([])
const shopStatus = ref(0)
const loadingProducts = ref(false)
const showFlavor = ref(false)
const showSetmeal = ref(false)
const showCart = ref(false)
const selectedDish = ref<Dish>()
const selectedSetmeal = ref<Setmeal>()
const setmealDishes = ref<DishItem[]>([])
const flavorSelection = ref<Record<string, string>>({})
const loadingSetmeal = ref(false)
const adding = ref(false)
let loadSequence = 0

onMounted(async () => {
  try {
    const [categoryData, status] = await Promise.all([catalogApi.categories(), catalogApi.shopStatus(), cart.load()])
    categories.value = categoryData.filter((item) => item.status === 1)
    activeCategoryId.value = categories.value[0]?.id ?? ''
    shopStatus.value = status
  } catch (error) { showToast(errorMessage(error)) }
})

watch(activeCategoryId, async (id) => {
  if (id === '') return
  const sequence = ++loadSequence
  loadingProducts.value = true
  try {
    const category = activeCategory.value
    const data = category?.type === 2 ? await catalogApi.setmeals(id) : await catalogApi.dishes(id)
    if (sequence === loadSequence) products.value = category?.type === 2
      ? (data as Setmeal[]).map((item) => ({ kind: 'setmeal' as const, item }))
      : (data as Dish[]).map((item) => ({ kind: 'dish' as const, item }))
  } catch (error) { showToast(errorMessage(error)) }
  finally { if (sequence === loadSequence) loadingProducts.value = false }
})

function openProduct(product: Product) {
  if (product.kind === 'dish') openDish(product.item)
  else void openSetmeal(product.item)
}

async function quickAdd(product: Product) {
  if (shopStatus.value !== 1) return showToast('门店当前休息中')
  if (product.kind === 'dish' && product.item.flavors?.length) return openDish(product.item)
  adding.value = true
  try {
    if (product.kind === 'dish') await cart.addDish(product.item)
    else await cart.addSetmeal(product.item)
    showToast('已加入购物车')
  } catch (error) { showToast(errorMessage(error)) }
  finally { adding.value = false }
}

function openDish(dish: Dish) {
  selectedDish.value = dish
  flavorSelection.value = Object.fromEntries((dish.flavors || []).map((flavor) => [String(flavor.id), parseFlavorOptions(flavor.value)[0] || '']))
  showFlavor.value = true
}

async function openSetmeal(setmeal: Setmeal) {
  selectedSetmeal.value = setmeal
  setmealDishes.value = []
  showSetmeal.value = true
  loadingSetmeal.value = true
  try { setmealDishes.value = await catalogApi.setmealDishes(setmeal.id) }
  catch (error) { showToast(errorMessage(error)) }
  finally { loadingSetmeal.value = false }
}

async function addSelectedDish() {
  if (!selectedDish.value || adding.value) return
  const selections = (selectedDish.value.flavors || []).map((flavor) => flavorSelection.value[String(flavor.id)]).filter(Boolean)
  if (selections.length !== (selectedDish.value.flavors || []).length) return showToast('请选择完整规格')
  adding.value = true
  try { await cart.addDish(selectedDish.value, selections.join(',')); showFlavor.value = false; showToast('已加入购物车') }
  catch (error) { showToast(errorMessage(error)) }
  finally { adding.value = false }
}

async function addSelectedSetmeal() {
  if (!selectedSetmeal.value || adding.value) return
  adding.value = true
  try { await cart.addSetmeal(selectedSetmeal.value); showSetmeal.value = false; showToast('已加入购物车') }
  catch (error) { showToast(errorMessage(error)) }
  finally { adding.value = false }
}

async function changeQuantity(item: CartItem, increase: boolean) {
  try { increase ? await cart.increase(item) : await cart.decrease(item) }
  catch (error) { showToast(errorMessage(error)) }
}

async function cleanCart() {
  if (!cart.count) return
  try { await showConfirmDialog({ title: '清空购物车', message: '确定移除所有已选餐品吗？' }); await cart.clean() }
  catch (error) { if (error !== 'cancel') showToast(errorMessage(error)) }
}

function goCheckout() {
  if (!cart.count) return showToast('请先选择餐品')
  if (shopStatus.value !== 1) return showToast('门店当前休息中')
  showCart.value = false
  void router.push('/checkout')
}
</script>

<style scoped>
.mobile-brand { margin-bottom: 16px; }
.hero { min-height: 230px; padding: 26px 24px; border-radius: 26px; display: flex; align-items: center; justify-content: space-between; overflow: hidden; color: white; background: linear-gradient(125deg,#0b5b56,#11877d); box-shadow: 0 18px 45px rgba(11,91,86,.2); }
.hero__eyebrow { color: #bfe9e0; font-size: 10px; font-weight: 700; letter-spacing: .18em; }
.hero h1 { margin: 10px 0 22px; font-size: clamp(28px,7vw,46px); line-height: 1.13; letter-spacing: -.05em; }
.hero__mark { flex: 0 0 104px; height: 150px; display: grid; place-items: center; border: 1px solid rgba(255,255,255,.2); border-radius: 50px 50px 18px 18px; color: #ffd0b2; font-size: 38px; font-weight: 900; line-height: 1.1; transform: rotate(6deg); background: rgba(255,255,255,.07); }
.shop-chip { display: inline-flex; align-items: center; gap: 8px; padding: 8px 12px; border-radius: 999px; color: #d7f5ee; background: rgba(255,255,255,.1); font-size: 12px; }
.shop-chip.closed { color: #ffe1cf; }
.status-dot { width: 7px; height: 7px; border-radius: 50%; background: #78e0b9; box-shadow: 0 0 0 4px rgba(120,224,185,.15); }
.closed .status-dot { background: #ff9b62; }
.ordering-area { margin-top: 16px; overflow: hidden; box-shadow: none; }
.category-list { display: flex; gap: 8px; padding: 14px; overflow-x: auto; border-bottom: 1px solid var(--line); scrollbar-width: none; }
.category-list button { flex: 0 0 auto; padding: 10px 14px; border: 0; border-radius: 13px; background: #f1f4f1; color: #536663; text-align: left; }
.category-list button span { display: block; font-weight: 700; }
.category-list button small { display: none; }
.category-list button.active { color: white; background: var(--brand); box-shadow: 0 7px 18px rgba(15,118,110,.2); }
.product-panel { padding: 18px 14px 90px; }
.product-panel__header { display: flex; align-items: start; justify-content: space-between; margin-bottom: 16px; }
.product-panel__header h2 { margin: 0; font-size: 20px; }
.product-panel__header p { margin: 5px 0 0; color: var(--muted); font-size: 12px; }
.product-panel__header > span { color: var(--muted); font-size: 12px; }
.product-grid { display: grid; gap: 12px; }
.product-card { min-height: 126px; display: grid; grid-template-columns: 112px 1fr; padding: 10px; border: 1px solid var(--line); border-radius: 18px; background: white; cursor: pointer; }
.product-card__image { width: 100%; height: 106px; overflow: hidden; border-radius: 13px; }
.product-card__body { min-width: 0; padding: 3px 4px 2px 13px; display: flex; flex-direction: column; justify-content: space-between; }
.product-card h3 { margin: 0 0 7px; font-size: 16px; }
.product-card p { margin: 0; display: -webkit-box; overflow: hidden; color: var(--muted); font-size: 12px; line-height: 1.5; -webkit-line-clamp: 2; -webkit-box-orient: vertical; }
.product-card__foot { display: flex; align-items: center; justify-content: space-between; }
.add-button { width: 34px; height: 34px; display: grid; place-items: center; border: 0; border-radius: 12px; color: white; background: var(--brand); box-shadow: 0 6px 14px rgba(15,118,110,.2); }
.add-button:disabled { background: #b8c2bf; box-shadow: none; }
.cart-bar { position: fixed; z-index: 16; left: 14px; right: 14px; bottom: calc(74px + env(safe-area-inset-bottom)); max-width: 680px; height: 64px; margin: auto; padding: 7px 7px 7px 12px; display: flex; align-items: center; border: 0; border-radius: 20px; color: white; background: #173836; box-shadow: 0 16px 35px rgba(23,56,54,.28); }
.cart-bar__icon { position: relative; width: 42px; height: 42px; display: grid; place-items: center; border-radius: 14px; color: #173836; background: #ffb27f; font-size: 23px; }
.cart-bar__icon :deep(.van-badge) { position: absolute; top: -6px; right: -7px; }
.cart-bar__price { flex: 1; padding-left: 12px; text-align: left; }
.cart-bar__price small { display: block; color: #a9b8b5; font-size: 10px; }
.cart-bar__price .price { color: white; font-size: 19px; }
.cart-bar__action { height: 50px; padding: 0 18px; display: grid; place-items: center; border-radius: 15px; color: #173836; background: #ff9b62; font-weight: 800; font-size: 14px; }
.cart-bar__action.disabled { color: #82918e; background: #405653; }
.detail-popup, .cart-popup { max-height: 86vh; overflow-y: auto; padding: 20px; }
.detail-content { max-width: 600px; margin: auto; }
.detail-cover { height: 190px; overflow: hidden; border-radius: 18px; }
.detail-content h2 { margin: 18px 0 6px; }
.detail-content h4 { margin: 20px 0 10px; }
.flavor-group button { margin: 0 8px 8px 0; padding: 9px 13px; border: 1px solid var(--line); border-radius: 11px; color: var(--muted); background: white; }
.flavor-group button.active { color: var(--brand); border-color: var(--brand); background: var(--brand-soft); }
.popup-submit { position: sticky; bottom: 0; padding-top: 14px; display: flex; align-items: center; justify-content: space-between; background: white; }
.popup-submit .price { font-size: 22px; }
.popup-submit .van-button { min-width: 150px; }
.setmeal-item { padding: 11px 0; display: flex; justify-content: space-between; border-bottom: 1px solid var(--line); }
.cart-popup__header, .cart-popup__header > div { display: flex; align-items: center; justify-content: space-between; }
.cart-popup__header h2 { margin: 0 8px 0 0; }
.cart-popup__header span { color: var(--muted); font-size: 12px; }
.cart-popup__header button { border: 0; color: var(--muted); background: transparent; }
.cart-items { max-height: 48vh; overflow-y: auto; margin-top: 12px; }
.cart-item { padding: 11px 0; display: grid; grid-template-columns: 58px 1fr auto; gap: 10px; align-items: center; border-bottom: 1px solid var(--line); }
.cart-item__image { width: 58px; height: 58px; overflow: hidden; border-radius: 12px; }
.cart-item__info { min-width: 0; }
.cart-item__info strong, .cart-item__info small, .cart-item__info span { display: block; }
.cart-item__info small { margin: 3px 0; color: var(--muted); font-size: 10px; }
.quantity { display: flex; align-items: center; gap: 9px; }
.quantity button { width: 29px; height: 29px; border: 1px solid var(--line); border-radius: 10px; color: var(--brand); background: white; font-size: 18px; }
.cart-checkout { padding-top: 14px; display: flex; align-items: center; justify-content: space-between; }
.cart-checkout small { display: block; color: var(--muted); }
.cart-checkout .price { font-size: 22px; }
.cart-checkout .van-button { min-width: 148px; }
@media (min-width: 700px) { .product-grid { grid-template-columns: repeat(2,minmax(0,1fr)); } }
@media (min-width: 900px) {
  .mobile-brand { display: none; }
  .hero { min-height: 280px; padding: 38px 42px; }
  .ordering-area { display: grid; grid-template-columns: 145px 1fr; margin-top: 22px; }
  .category-list { display: block; padding: 16px 10px; border: 0; border-right: 1px solid var(--line); overflow: visible; }
  .category-list button { width: 100%; margin-bottom: 8px; }
  .category-list button small { display: block; margin-top: 3px; opacity: .7; }
  .product-panel { padding: 22px 22px 110px; }
  .cart-bar { bottom: 40px; left: calc(50% - 170px); right: 40px; max-width: 650px; margin: 0; }
  .detail-popup, .cart-popup { width: min(620px,50vw); max-height: 88vh; left: auto; }
}
</style>