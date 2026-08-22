<template>
  <div class="report-chart" :style="{ minHeight: `${height}px` }" v-loading="loading">
    <div ref="chartElement" class="report-chart__canvas" :style="{ height: `${height}px` }" />
    <div v-if="!loading && error" class="report-chart__state report-chart__state--error">
      <strong>图表加载失败</strong><span>{{ error }}</span><el-button link type="primary" @click="$emit('retry')">重新加载</el-button>
    </div>
    <div v-else-if="!loading && empty" class="report-chart__state">
      <strong>{{ emptyTitle }}</strong><span>{{ emptyDescription }}</span>
    </div>
  </div>
</template>

<script setup lang="ts">
import { nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { init, use, type ECharts, type EChartsCoreOption } from 'echarts/core'
import { BarChart, LineChart, PieChart } from 'echarts/charts'
import { AriaComponent, GridComponent, LegendComponent, TitleComponent, TooltipComponent } from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'

use([LineChart, BarChart, PieChart, GridComponent, TooltipComponent, LegendComponent, TitleComponent, AriaComponent, CanvasRenderer])

const props = withDefaults(defineProps<{
  option: EChartsCoreOption
  loading?: boolean
  empty?: boolean
  error?: string
  height?: number
  emptyTitle?: string
  emptyDescription?: string
}>(), {
  loading: false,
  empty: false,
  error: '',
  height: 320,
  emptyTitle: '暂无统计数据',
  emptyDescription: '当前日期范围没有可展示的数据。',
})
defineEmits<{ retry: [] }>()

const chartElement = ref<HTMLElement>()
let chart: ECharts | null = null
let resizeObserver: ResizeObserver | null = null

function disposeChart() {
  chart?.dispose()
  chart = null
  if (chartElement.value) {
    chartElement.value.replaceChildren()
    chartElement.value.removeAttribute('_echarts_instance_')
    chartElement.value.removeAttribute('role')
    chartElement.value.removeAttribute('aria-label')
  }
}

async function renderChart() {
  await nextTick()
  if (!chartElement.value || props.loading || props.empty || props.error) {
    disposeChart()
    return
  }
  chart ||= init(chartElement.value)
  chart.setOption(props.option, { notMerge: true })
  chart.resize()
}

function resizeChart() {
  chart?.resize()
}

watch(() => [props.option, props.loading, props.empty, props.error], renderChart, { deep: true })

onMounted(() => {
  if (typeof ResizeObserver !== 'undefined' && chartElement.value) {
    resizeObserver = new ResizeObserver(resizeChart)
    resizeObserver.observe(chartElement.value)
  } else {
    window.addEventListener('resize', resizeChart)
  }
  void renderChart()
})

onBeforeUnmount(() => {
  resizeObserver?.disconnect()
  window.removeEventListener('resize', resizeChart)
  disposeChart()
})
</script>

<style scoped>
.report-chart { position: relative; width: 100%; }
.report-chart__canvas { width: 100%; }
.report-chart__state { position: absolute; inset: 0; padding: 24px; display: grid; place-content: center; justify-items: center; gap: 8px; border-radius: 16px; color: var(--muted); background: linear-gradient(135deg,rgba(223,243,239,.55),rgba(243,245,241,.9)); text-align: center; }
.report-chart__state strong { color: var(--ink); font-size: 14px; }
.report-chart__state span { max-width: 320px; font-size: 11px; line-height: 1.6; }
.report-chart__state--error { background: #fff2f2; }
</style>
