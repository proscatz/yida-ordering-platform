<template>
  <header class="page-header">
    <div class="page-header__inner">
      <button type="button" class="back-button" aria-label="返回" @click="goBack">
        <van-icon name="arrow-left" />
      </button>
      <div class="page-header__title">{{ title }}</div>
      <button v-if="action" type="button" class="page-header__action" @click="$emit('action')">{{ action }}</button>
      <span v-else />
    </div>
  </header>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router'
import { Icon as VanIcon } from 'vant'

defineProps<{ title: string; action?: string }>()
defineEmits<{ action: [] }>()
const router = useRouter()
function goBack() { window.history.length > 1 ? router.back() : void router.push('/') }
</script>

<style scoped>
button { border: 0; background: transparent; cursor: pointer; }
.back-button { width: 42px; height: 42px; border-radius: 14px; color: var(--ink); font-size: 20px; }
.back-button:active { background: #eef3f0; }
</style>