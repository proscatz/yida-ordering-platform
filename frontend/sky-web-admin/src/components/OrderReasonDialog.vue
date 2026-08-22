<template>
  <el-dialog
    :model-value="modelValue"
    :title="action === 'reject' ? '填写拒单原因' : '填写取消原因'"
    width="min(480px, calc(100vw - 30px))"
    :close-on-click-modal="!loading"
    :close-on-press-escape="!loading"
    :show-close="!loading"
    align-center
    @update:model-value="$emit('update:modelValue', $event)"
  >
    <p class="reason-order">订单号：{{ orderNumber }}</p>
    <el-form label-position="top" @submit.prevent="submit">
      <el-form-item :label="action === 'reject' ? '常用拒单原因' : '常用取消原因'">
        <el-select v-model="preset" placeholder="请选择原因" @change="selectPreset">
          <el-option v-for="item in options" :key="item" :label="item" :value="item" />
        </el-select>
      </el-form-item>
      <el-form-item label="原因说明" required>
        <el-input v-model.trim="reason" type="textarea" :rows="4" maxlength="100" show-word-limit placeholder="请选择或填写具体原因" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button :disabled="loading" @click="$emit('update:modelValue', false)">暂不操作</el-button>
      <el-button :type="action === 'reject' ? 'danger' : 'warning'" :loading="loading" @click="submit">
        {{ action === 'reject' ? '确认拒单' : '确认取消' }}
      </el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'

const props = defineProps<{
  modelValue: boolean
  action: 'reject' | 'cancel'
  orderNumber: string
  loading?: boolean
}>()
const emit = defineEmits<{ 'update:modelValue': [value: boolean]; confirm: [reason: string] }>()
const preset = ref('')
const reason = ref('')
const rejectOptions = ['菜品已售罄', '门店临时无法接单', '配送能力不足', '其他原因']
const cancelOptions = ['与顾客协商取消', '无法按时履约', '运营计划调整', '其他原因']
const options = computed(() => props.action === 'reject' ? rejectOptions : cancelOptions)

watch(() => props.modelValue, (open) => {
  if (open) { preset.value = ''; reason.value = '' }
})

function selectPreset(value: string) {
  reason.value = value === '其他原因' ? '' : value
}

function submit() {
  if (!reason.value.trim()) {
    ElMessage.warning(props.action === 'reject' ? '请填写拒单原因' : '请填写取消原因')
    return
  }
  emit('confirm', reason.value.trim())
}
</script>

<style scoped>
.reason-order { margin: -4px 0 18px; padding: 11px 13px; border-radius: 12px; color: var(--muted); background: var(--surface-muted); font-size: 12px; }
:deep(.el-select) { width: 100%; }
</style>
