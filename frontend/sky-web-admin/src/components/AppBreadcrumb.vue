<template>
  <div class="breadcrumb" aria-label="面包屑">
    <span>管理中心</span>
    <Right class="breadcrumb__separator" />
    <strong>{{ currentTitle }}</strong>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { Right } from '@element-plus/icons-vue'

const route = useRoute()
const currentTitle = computed(() => String(route.meta.title || '工作台'))
</script>
