<template>
  <div class="flavor-editor">
    <div class="flavor-editor__heading"><div><strong>规格与口味</strong><span>每组至少保留一个可选值</span></div><el-button :icon="Plus" plain @click="addGroup">添加规格组</el-button></div>
    <div v-if="!modelValue.length" class="flavor-editor__empty">当前菜品没有规格，可按需添加口味、温度或份量。</div>
    <article v-for="(group, index) in modelValue" :key="group.key" class="flavor-group">
      <div class="flavor-group__top">
        <el-input :model-value="group.name" maxlength="20" placeholder="规格名称，如辣度" @update:model-value="updateName(index, $event)" />
        <el-button circle :icon="Delete" type="danger" plain aria-label="删除规格组" @click="removeGroup(index)" />
      </div>
      <div class="flavor-group__values">
        <el-tag v-for="(value, valueIndex) in group.values" :key="`${group.key}-${value}`" closable @close="removeValue(index, valueIndex)">{{ value }}</el-tag>
        <el-input
          v-model.trim="valueInputs[group.key]"
          class="flavor-group__value-input"
          maxlength="20"
          placeholder="输入规格值后回车"
          @keyup.enter="addValue(index)"
        />
        <el-button link type="primary" @click="addValue(index)">添加</el-button>
      </div>
    </article>
  </div>
</template>

<script setup lang="ts">
import { reactive } from 'vue'
import { Delete, Plus } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import type { FlavorDraft } from '@/types/catalog'

const props = defineProps<{ modelValue: FlavorDraft[] }>()
const emit = defineEmits<{ 'update:modelValue': [value: FlavorDraft[]] }>()
const valueInputs = reactive<Record<string, string>>({})

function createKey() {
  return typeof crypto !== 'undefined' && 'randomUUID' in crypto ? crypto.randomUUID() : `${Date.now()}-${Math.random()}`
}

function clone() { return props.modelValue.map((group) => ({ ...group, values: [...group.values] })) }
function addGroup() { emit('update:modelValue', [...clone(), { key: createKey(), name: '', values: [] }]) }
function removeGroup(index: number) { const next = clone(); next.splice(index, 1); emit('update:modelValue', next) }
function updateName(index: number, name: string) { const next = clone(); if (next[index]) next[index].name = name; emit('update:modelValue', next) }
function removeValue(groupIndex: number, valueIndex: number) { const next = clone(); next[groupIndex]?.values.splice(valueIndex, 1); emit('update:modelValue', next) }

function addValue(index: number) {
  const group = props.modelValue[index]
  if (!group) return
  const value = valueInputs[group.key]?.trim()
  if (!value) return
  if (group.values.some((current) => current.toLocaleLowerCase() === value.toLocaleLowerCase())) {
    ElMessage.warning('该规格值已经存在')
    return
  }
  const next = clone()
  next[index]?.values.push(value)
  valueInputs[group.key] = ''
  emit('update:modelValue', next)
}
</script>

<style scoped>
.flavor-editor { width: 100%; }
.flavor-editor__heading { margin-bottom: 12px; display: flex; align-items: center; justify-content: space-between; gap: 12px; }
.flavor-editor__heading strong,.flavor-editor__heading span { display: block; }
.flavor-editor__heading strong { font-size: 13px; }
.flavor-editor__heading span { margin-top: 3px; color: var(--muted); font-size: 10px; }
.flavor-editor__empty { padding: 25px 18px; border: 1px dashed #d8e2de; border-radius: 14px; color: var(--muted); background: #f8faf9; font-size: 11px; text-align: center; }
.flavor-group { margin-top: 10px; padding: 14px; border: 1px solid var(--line); border-radius: 15px; background: #fafcfb; }
.flavor-group__top { display: grid; grid-template-columns: 1fr 32px; align-items: center; gap: 10px; }
.flavor-group__values { margin-top: 12px; display: flex; align-items: center; flex-wrap: wrap; gap: 8px; }
.flavor-group__value-input { width: 155px; }
</style>
