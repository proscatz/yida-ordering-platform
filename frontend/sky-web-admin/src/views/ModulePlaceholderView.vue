<template>
  <div class="content-page">
    <header class="page-heading">
      <div>
        <span class="eyebrow">{{ eyebrow }}</span>
        <h1>{{ title }}</h1>
        <p>{{ description }}</p>
      </div>
    </header>
    <section class="surface-card placeholder-card">
      <EmptyState title="框架已经准备好" description="本模块将在对应业务阶段接入服务端数据。">
        <el-button type="primary" plain @click="router.push('/workspace')">返回工作台</el-button>
      </EmptyState>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import EmptyState from '@/components/EmptyState.vue'

const route = useRoute()
const router = useRouter()
const title = computed(() => String(route.meta.title || '功能模块'))
const eyebrow = computed(() => String(route.meta.eyebrow || '管理中心'))
const description = computed(() => String(route.meta.description || '相关功能将在后续阶段接入。'))
</script>
