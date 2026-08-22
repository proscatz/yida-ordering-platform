<template>
  <el-dialog
    :model-value="modelValue"
    class="confirm-action-dialog"
    width="min(430px, calc(100vw - 30px))"
    :close-on-click-modal="!loading"
    :close-on-press-escape="!loading"
    :show-close="!loading"
    align-center
    @update:model-value="$emit('update:modelValue', $event)"
  >
    <div class="confirm-action">
      <span class="confirm-action__icon" :class="`confirm-action__icon--${tone}`"><WarningFilled /></span>
      <h3>{{ title }}</h3>
      <p>{{ description }}</p>
    </div>
    <template #footer>
      <el-button :disabled="loading" @click="$emit('update:modelValue', false)">暂不操作</el-button>
      <el-button :type="tone === 'danger' ? 'danger' : 'primary'" :loading="loading" @click="$emit('confirm')">
        {{ confirmText }}
      </el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { WarningFilled } from '@element-plus/icons-vue'

withDefaults(defineProps<{
  modelValue: boolean
  title: string
  description: string
  confirmText?: string
  tone?: 'danger' | 'primary' | 'warning'
  loading?: boolean
}>(), {
  confirmText: '确认操作',
  tone: 'warning',
  loading: false,
})

defineEmits<{ 'update:modelValue': [value: boolean]; confirm: [] }>()
</script>

<style scoped>
.confirm-action { padding: 4px 12px 6px; text-align: center; }
.confirm-action__icon { width: 58px; height: 58px; margin: 0 auto 16px; display: grid; place-items: center; border-radius: 19px; color: #bc6a2d; background: var(--accent-soft); }
.confirm-action__icon--danger { color: #b83e47; background: #ffeaec; }
.confirm-action__icon--primary { color: var(--brand); background: var(--brand-soft); }
.confirm-action__icon svg { width: 27px; }
.confirm-action h3 { margin: 0 0 9px; color: var(--ink); font-size: 20px; }
.confirm-action p { margin: 0; color: var(--muted); font-size: 13px; line-height: 1.7; }
</style>
