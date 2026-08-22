<template>
  <el-drawer
    :model-value="modelValue"
    class="entity-form-drawer"
    :title="title"
    :size="size"
    :close-on-click-modal="!submitting"
    :close-on-press-escape="!submitting"
    :show-close="!submitting"
    destroy-on-close
    :before-close="handleDrawerClose"
    @update:model-value="$emit('update:modelValue', $event)"
  >
    <div class="entity-form-drawer__body"><slot /></div>
    <template #footer>
      <div class="entity-form-drawer__footer">
        <el-button :disabled="submitting" @click="requestClose">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="$emit('submit')">{{ submitText }}</el-button>
      </div>
    </template>
  </el-drawer>
</template>

<script setup lang="ts">
const props = withDefaults(defineProps<{
  modelValue: boolean
  title: string
  submitting?: boolean
  submitText?: string
  size?: string
  beforeClose?: () => boolean | Promise<boolean>
}>(), {
  submitting: false,
  submitText: '保存',
  size: 'min(520px, 100vw)',
  beforeClose: () => true,
})
const emit = defineEmits<{ 'update:modelValue': [value: boolean]; submit: [] }>()

async function requestClose() {
  if (props.submitting || !(await props.beforeClose())) return
  emit('update:modelValue', false)
}

async function handleDrawerClose(done: () => void) {
  if (props.submitting || !(await props.beforeClose())) return
  done()
}
</script>

<style scoped>
.entity-form-drawer__body { padding: 4px 3px; }
.entity-form-drawer__footer { display: flex; justify-content: flex-end; gap: 10px; }
.entity-form-drawer__footer :deep(.el-button) { min-width: 96px; }
</style>
