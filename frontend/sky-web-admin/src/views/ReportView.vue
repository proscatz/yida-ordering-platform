<template>
  <div class="content-page">
    <header class="page-heading">
      <div><span class="eyebrow">经营分析</span><h1>数据统计</h1><p>查看营业、用户、订单与商品销量趋势。</p></div>
      <div class="report-actions">
        <el-date-picker
          v-model="dateRange"
          type="daterange"
          value-format="YYYY-MM-DD"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
          range-separator="至"
          :clearable="false"
          unlink-panels
        />
        <el-button type="primary" :icon="Search" :loading="loading" @click="applyRange">查询</el-button>
        <el-button :icon="Download" :loading="exporting" @click="exportReport">导出近30日</el-button>
      </div>
    </header>

    <el-alert
      title="页面图表按所选日期范围统计；后端导出接口固定导出昨天以前的近30日经营数据。"
      type="info"
      :closable="false"
      show-icon
      class="report-tip"
    />

    <section class="metric-grid report-metrics" aria-label="所选区间经营指标">
      <article class="metric-card"><span class="metric-card__icon"><TrendCharts /></span><div><span>营业额</span><strong>{{ formatMoney(periodTurnover) }}</strong><small>已完成订单收入</small></div></article>
      <article class="metric-card"><span class="metric-card__icon"><CircleCheck /></span><div><span>有效订单</span><strong>{{ orderData.validOrderCount }}</strong><small>已完成订单数</small></div></article>
      <article class="metric-card"><span class="metric-card__icon"><DataAnalysis /></span><div><span>订单完成率</span><strong>{{ formatPercent(orderData.completionRate) }}</strong><small>有效订单 / 全部订单</small></div></article>
      <article class="metric-card"><span class="metric-card__icon"><UserFilled /></span><div><span>新增用户</span><strong>{{ periodNewUsers }}</strong><small>区间新增用户数</small></div></article>
    </section>

    <section class="report-grid">
      <article class="surface-card chart-card chart-card--wide">
        <div class="card-heading"><div><span class="eyebrow">收入趋势</span><h2>营业额趋势</h2></div><small>单位：元</small></div>
        <ReportChart :option="turnoverOption" :loading="loading" :empty="!turnoverData.hasData" :error="errors.turnover" @retry="loadReports" />
      </article>

      <article class="surface-card chart-card">
        <div class="card-heading"><div><span class="eyebrow">用户增长</span><h2>用户趋势</h2></div><small>累计与新增</small></div>
        <ReportChart :option="userOption" :loading="loading" :empty="!userData.hasData" :error="errors.users" @retry="loadReports" />
      </article>

      <article class="surface-card chart-card">
        <div class="card-heading"><div><span class="eyebrow">履约趋势</span><h2>订单趋势</h2></div><small>全部与有效</small></div>
        <ReportChart :option="orderOption" :loading="loading" :empty="!orderData.hasData" :error="errors.orders" @retry="loadReports" />
      </article>

      <article class="surface-card chart-card completion-chart-card">
        <div class="card-heading"><div><span class="eyebrow">服务质量</span><h2>订单完成率</h2></div><small>{{ orderData.validOrderCount }} / {{ orderData.totalOrderCount }} 笔</small></div>
        <ReportChart :option="completionOption" :loading="loading" :empty="!orderData.hasData" :error="errors.orders" :height="300" @retry="loadReports" />
      </article>

      <article class="surface-card chart-card">
        <div class="card-heading"><div><span class="eyebrow">商品表现</span><h2>销量 Top 10</h2></div><small>{{ topSeller }}</small></div>
        <ReportChart :option="top10Option" :loading="loading" :empty="!top10Data.hasData" :error="errors.top10" :height="300" @retry="loadReports" />
      </article>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { CircleCheck, DataAnalysis, Download, Search, TrendCharts, UserFilled } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import type { EChartsCoreOption } from 'echarts/core'
import ReportChart from '@/components/ReportChart.vue'
import { reportApi } from '@/api/reports'
import { userFacingError } from '@/api/errors'
import { withWriteLock } from '@/utils/writeLock'
import { formatMoney, formatPercent } from '@/utils/format'
import {
  defaultReportRange,
  transformOrderReport,
  transformTop10Report,
  transformTurnoverReport,
  transformUserReport,
  validateReportRange,
} from '@/utils/reportTransform'
import type { OrderChartData, ReportRange, Top10ChartData, TurnoverChartData, UserChartData } from '@/types/reports'

const BRAND = '#0f766e'
const BRAND_DARK = '#0a514d'
const ACCENT = '#ff8a4c'
const MUTED = '#8aa09d'
const initialRange = defaultReportRange()
const dateRange = ref<[string, string]>([initialRange.begin, initialRange.end])
const activeRange = ref<ReportRange>(initialRange)
const loading = ref(false)
const exporting = ref(false)
const errors = reactive({ turnover: '', users: '', orders: '', top10: '' })
const turnoverData = ref<TurnoverChartData>(transformTurnoverReport({ dateList: null, turnoverList: null }, initialRange))
const userData = ref<UserChartData>(transformUserReport({ dateList: null, totalUserList: null, newUserList: null }, initialRange))
const orderData = ref<OrderChartData>(transformOrderReport({ dateList: null, orderCountList: null, validOrderCountList: null, totalOrderCount: 0, validOrderCount: 0, orderCompletionRate: 0 }, initialRange))
const top10Data = ref<Top10ChartData>(transformTop10Report({ nameList: null, numberList: null }))

const periodTurnover = computed(() => turnoverData.value.values.reduce((sum, value) => sum + value, 0))
const periodNewUsers = computed(() => userData.value.newUsers.reduce((sum, value) => sum + value, 0))
const topSeller = computed(() => top10Data.value.names[0] ? `TOP 1：${top10Data.value.names[0]}` : '暂无销量')
const shortDates = (dates: string[]) => dates.map((date) => date.slice(5))

const commonAxis = {
  axisLine: { lineStyle: { color: '#dce5e2' } },
  axisTick: { show: false },
  axisLabel: { color: '#738581', fontSize: 10 },
}
const commonTooltip = { trigger: 'axis' as const, backgroundColor: 'rgba(10,81,77,.94)', borderWidth: 0, textStyle: { color: '#fff' } }

const turnoverOption = computed<EChartsCoreOption>(() => ({
  aria: { enabled: true, decal: { show: true } },
  tooltip: { ...commonTooltip, valueFormatter: (value: unknown) => formatMoney(Number(value) || 0) },
  grid: { left: 18, right: 18, top: 28, bottom: 12, containLabel: true },
  xAxis: { type: 'category', boundaryGap: false, data: shortDates(turnoverData.value.dates), ...commonAxis },
  yAxis: { type: 'value', ...commonAxis, splitLine: { lineStyle: { color: '#edf1ef' } } },
  series: [{ type: 'line', name: '营业额', data: turnoverData.value.values, smooth: true, symbolSize: 7, lineStyle: { width: 3, color: BRAND }, itemStyle: { color: BRAND }, areaStyle: { color: 'rgba(15,118,110,.13)' } }],
}))

const userOption = computed<EChartsCoreOption>(() => ({
  aria: { enabled: true }, tooltip: commonTooltip,
  legend: { top: 0, right: 0, textStyle: { color: '#738581', fontSize: 10 } },
  grid: { left: 14, right: 14, top: 42, bottom: 12, containLabel: true },
  xAxis: { type: 'category', data: shortDates(userData.value.dates), ...commonAxis },
  yAxis: { type: 'value', minInterval: 1, ...commonAxis, splitLine: { lineStyle: { color: '#edf1ef' } } },
  series: [
    { type: 'line', name: '用户总量', data: userData.value.totalUsers, smooth: true, itemStyle: { color: BRAND }, lineStyle: { color: BRAND, width: 3 } },
    { type: 'bar', name: '新增用户', data: userData.value.newUsers, barMaxWidth: 18, itemStyle: { color: ACCENT, borderRadius: [5, 5, 0, 0] } },
  ],
}))

const orderOption = computed<EChartsCoreOption>(() => ({
  aria: { enabled: true }, tooltip: commonTooltip,
  legend: { top: 0, right: 0, textStyle: { color: '#738581', fontSize: 10 } },
  grid: { left: 14, right: 14, top: 42, bottom: 12, containLabel: true },
  xAxis: { type: 'category', data: shortDates(orderData.value.dates), ...commonAxis },
  yAxis: { type: 'value', minInterval: 1, ...commonAxis, splitLine: { lineStyle: { color: '#edf1ef' } } },
  series: [
    { type: 'line', name: '全部订单', data: orderData.value.orderCounts, smooth: true, itemStyle: { color: MUTED }, lineStyle: { color: MUTED, width: 2 } },
    { type: 'line', name: '有效订单', data: orderData.value.validOrderCounts, smooth: true, itemStyle: { color: BRAND }, lineStyle: { color: BRAND, width: 3 }, areaStyle: { color: 'rgba(15,118,110,.1)' } },
  ],
}))

const completionOption = computed<EChartsCoreOption>(() => {
  const completed = orderData.value.validOrderCount
  const unfinished = Math.max(0, orderData.value.totalOrderCount - completed)
  return {
    aria: { enabled: true }, tooltip: { trigger: 'item', formatter: '{b}：{c} 笔（{d}%）' },
    title: { text: formatPercent(orderData.value.completionRate), subtext: '订单完成率', left: 'center', top: '38%', textStyle: { color: BRAND_DARK, fontSize: 26, fontWeight: 800 }, subtextStyle: { color: '#738581', fontSize: 11 } },
    series: [{ type: 'pie', radius: ['62%', '80%'], center: ['50%', '52%'], label: { show: false }, emphasis: { scale: false }, data: [
      { name: '已完成', value: completed, itemStyle: { color: BRAND } },
      { name: '未完成', value: unfinished, itemStyle: { color: '#e7eeec' } },
    ] }],
  }
})

const top10Option = computed<EChartsCoreOption>(() => ({
  aria: { enabled: true }, tooltip: { ...commonTooltip, trigger: 'axis', axisPointer: { type: 'shadow' } },
  grid: { left: 12, right: 22, top: 16, bottom: 12, containLabel: true },
  xAxis: { type: 'value', minInterval: 1, ...commonAxis, splitLine: { lineStyle: { color: '#edf1ef' } } },
  yAxis: { type: 'category', inverse: true, data: top10Data.value.names, ...commonAxis, axisLabel: { color: '#536b67', fontSize: 10, width: 100, overflow: 'truncate' } },
  series: [{ type: 'bar', name: '销量', data: top10Data.value.values, barMaxWidth: 19, itemStyle: { color: BRAND, borderRadius: [0, 7, 7, 0] }, label: { show: true, position: 'right', color: BRAND_DARK, fontWeight: 700 } }],
}))

function resultError(result: PromiseRejectedResult, fallback: string) {
  return userFacingError(result.reason, fallback)
}

async function loadReports() {
  loading.value = true
  Object.assign(errors, { turnover: '', users: '', orders: '', top10: '' })
  const range = activeRange.value
  const [turnover, users, orders, top10] = await Promise.allSettled([
    reportApi.turnover(range), reportApi.users(range), reportApi.orders(range), reportApi.top10(range),
  ])

  if (turnover.status === 'fulfilled') turnoverData.value = transformTurnoverReport(turnover.value, range)
  else { turnoverData.value = transformTurnoverReport({ dateList: null, turnoverList: null }, range); errors.turnover = resultError(turnover, '营业额数据加载失败') }
  if (users.status === 'fulfilled') userData.value = transformUserReport(users.value, range)
  else { userData.value = transformUserReport({ dateList: null, totalUserList: null, newUserList: null }, range); errors.users = resultError(users, '用户数据加载失败') }
  if (orders.status === 'fulfilled') orderData.value = transformOrderReport(orders.value, range)
  else { orderData.value = transformOrderReport({ dateList: null, orderCountList: null, validOrderCountList: null, totalOrderCount: 0, validOrderCount: 0, orderCompletionRate: 0 }, range); errors.orders = resultError(orders, '订单数据加载失败') }
  if (top10.status === 'fulfilled') top10Data.value = transformTop10Report(top10.value)
  else { top10Data.value = transformTop10Report({ nameList: null, numberList: null }); errors.top10 = resultError(top10, '销量排行加载失败') }
  loading.value = false
}

function applyRange() {
  const range = { begin: dateRange.value?.[0] || '', end: dateRange.value?.[1] || '' }
  const validation = validateReportRange(range)
  if (validation) { ElMessage.warning(validation); return }
  activeRange.value = range
  void loadReports()
}

async function exportReport() {
  if (exporting.value) return
  exporting.value = true
  try {
    const file = await withWriteLock('report:export', reportApi.export)
    if (!file.blob.size) throw new Error('导出文件为空')
    const fallback = `驿达点餐_运营数据报表_${new Date().toISOString().slice(0, 10)}.xlsx`
    const fileName = file.fileName?.split(/[\\/]/).pop() || fallback
    const url = URL.createObjectURL(file.blob)
    const anchor = document.createElement('a')
    anchor.href = url
    anchor.download = fileName
    anchor.click()
    setTimeout(() => URL.revokeObjectURL(url), 1_000)
    ElMessage.success('报表已开始下载')
  } catch (error) {
    ElMessage.error(userFacingError(error, '报表导出失败'))
  } finally {
    exporting.value = false
  }
}

onMounted(loadReports)
</script>

<style scoped>
.page-heading { align-items: center; }
.report-actions { display: flex; align-items: center; flex-wrap: wrap; justify-content: flex-end; gap: 10px; }
.report-actions :deep(.el-date-editor) { width: 280px; }
.report-tip { margin-bottom: 18px; border-radius: 14px; }
.report-metrics { margin-bottom: 20px; }
.report-grid { display: grid; grid-template-columns: repeat(2,minmax(0,1fr)); gap: 20px; }
.chart-card { min-width: 0; padding-bottom: 12px; overflow: hidden; }
.chart-card--wide { grid-column: 1 / -1; }
.card-heading { margin-bottom: 4px; }
.card-heading small { color: var(--muted); font-size: 10px; }
@media (max-width: 1080px) {
  .report-grid { grid-template-columns: 1fr; }
  .chart-card--wide { grid-column: auto; }
}
@media (max-width: 760px) {
  .page-heading { align-items: stretch; flex-direction: column; }
  .report-actions { justify-content: stretch; }
  .report-actions :deep(.el-date-editor),.report-actions :deep(.el-button) { width: 100%; margin: 0; }
}
</style>
