<template>
  <div class="page page--sub edit-page">
    <PageHeader :title="isEdit ? '编辑地址' : '新增地址'" />
    <van-form class="address-form page-card" @submit="save">
      <van-field
        v-model="form.consignee"
        name="consignee"
        label="联系人"
        maxlength="30"
        placeholder="请输入联系人姓名"
        :rules="consigneeRules"
        :error-message="fieldErrors.consignee"
        @update:model-value="clearError('consignee')"
      />
      <van-field
        v-model="form.phone"
        name="phone"
        label="手机号"
        type="tel"
        maxlength="11"
        placeholder="请输入手机号"
        :rules="phoneRules"
        :error-message="fieldErrors.phone"
        @update:model-value="clearError('phone')"
      />
      <van-field name="sex" label="称呼" :error-message="fieldErrors.sex">
        <template #input>
          <van-radio-group v-model="form.sex" direction="horizontal" @change="clearError('sex')">
            <van-radio name="1">先生</van-radio>
            <van-radio name="0">女士</van-radio>
          </van-radio-group>
        </template>
      </van-field>
      <van-field
        :model-value="regionText"
        name="region"
        label="省市区"
        readonly
        is-link
        placeholder="请选择省、市、区"
        :rules="regionRules"
        :error-message="fieldErrors.region"
        @click="showAreaPicker = true"
      />
      <van-field
        v-model="form.detail"
        name="detail"
        label="详细地址"
        type="textarea"
        rows="2"
        autosize
        maxlength="200"
        show-word-limit
        placeholder="街道、楼栋、门牌号（至少5个字符）"
        :rules="detailRules"
        :error-message="fieldErrors.detail"
        @update:model-value="clearError('detail')"
      />
      <van-field
        v-model="form.label"
        name="label"
        label="标签"
        maxlength="20"
        placeholder="公司 / 学校 / 家"
        :rules="labelRules"
        :error-message="fieldErrors.label"
        @update:model-value="clearError('label')"
      />
      <van-cell title="设为默认地址">
        <template #right-icon><van-switch v-model="isDefault" size="22" /></template>
      </van-cell>
      <div class="form-submit">
        <van-button class="brand-button" type="primary" block native-type="submit" :loading="saving">
          保存地址
        </van-button>
      </div>
    </van-form>

    <van-popup v-model:show="showAreaPicker" position="bottom" round>
      <van-cascader
        v-model="cascaderValue"
        title="选择省市区"
        :options="areaOptions"
        @close="showAreaPicker = false"
        @finish="finishArea"
      />
    </van-popup>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  Button as VanButton,
  Cascader as VanCascader,
  Cell as VanCell,
  Field as VanField,
  Form as VanForm,
  Popup as VanPopup,
  Radio as VanRadio,
  RadioGroup as VanRadioGroup,
  Switch as VanSwitch,
  showToast,
} from 'vant'
import { useCascaderAreaData } from '@vant/area-data'
import PageHeader from '@/components/PageHeader.vue'
import { addressApi } from '@/api/modules'
import { AppRequestError, errorMessage } from '@/api/http'
import type { AddressPayload, AddressUpdatePayload } from '@/types'
import {
  applyAreaSelection,
  controlCharacters,
  normalizeAddressFieldErrors,
  type AddressField,
  type SelectedAreaOption,
} from '@/utils/address'

const route = useRoute()
const router = useRouter()
const saving = ref(false)
const isDefault = ref(false)
const showAreaPicker = ref(false)
const cascaderValue = ref('')
const areaOptions = useCascaderAreaData()
const fieldErrors = reactive<Partial<Record<AddressField, string>>>({})
const isEdit = computed(() => Boolean(route.params.id))
const form = reactive<AddressPayload>({
  consignee: '',
  phone: '',
  sex: '1',
  provinceCode: '',
  provinceName: '',
  cityCode: '',
  cityName: '',
  districtCode: '',
  districtName: '',
  detail: '',
  label: '',
  isDefault: 0,
})

const regionText = computed(() => [form.provinceName, form.cityName, form.districtName].filter(Boolean).join(' / '))
const consigneeRules = [
  { required: true, message: '请输入联系人' },
  { validator: (value: string) => value.trim().length >= 2 && value.trim().length <= 30, message: '联系人长度应为2～30个字符' },
  { validator: (value: string) => !controlCharacters.test(value), message: '联系人不能包含控制字符' },
]
const phoneRules = [
  { required: true, message: '请输入手机号' },
  { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号' },
]
const regionRules = [{ validator: () => Boolean(form.provinceCode && form.cityCode && form.districtCode), message: '请选择省市区' }]
const detailRules = [
  { required: true, message: '请输入详细地址' },
  { validator: (value: string) => value.trim().length >= 5 && value.trim().length <= 200, message: '详细地址长度应为5～200个字符' },
  { validator: (value: string) => !controlCharacters.test(value), message: '详细地址不能包含控制字符' },
]
const labelRules = [
  { validator: (value: string) => !value || value.trim().length <= 20, message: '标签最多20个字符' },
  { validator: (value: string) => !controlCharacters.test(value), message: '标签不能包含控制字符' },
]

onMounted(async () => {
  if (!isEdit.value) return
  try {
    const address = await addressApi.detail(String(route.params.id))
    Object.assign(form, address)
    isDefault.value = address.isDefault === 1
    cascaderValue.value = address.districtCode
  } catch (error) {
    showToast(errorMessage(error))
  }
})

function finishArea({ selectedOptions }: { selectedOptions: SelectedAreaOption[] }) {
  if (!applyAreaSelection(form, selectedOptions)) return
  clearError('region')
  showAreaPicker.value = false
}

function clearError(field: AddressField) {
  delete fieldErrors[field]
}

function applyServerErrors(error: AppRequestError) {
  Object.assign(fieldErrors, normalizeAddressFieldErrors(error.fieldErrors))
}

async function save() {
  if (saving.value) return
  saving.value = true
  Object.keys(fieldErrors).forEach((key) => delete fieldErrors[key as AddressField])
  try {
    form.consignee = form.consignee.trim()
    form.phone = form.phone.trim()
    form.detail = form.detail.trim()
    form.label = form.label?.trim() || ''
    form.isDefault = isDefault.value ? 1 : 0
    if (isEdit.value) {
      await addressApi.update({ ...form, id: String(route.params.id) } as AddressUpdatePayload)
    } else {
      await addressApi.create({ ...form })
    }
    showToast('地址已保存')
    router.back()
  } catch (error) {
    if (error instanceof AppRequestError) applyServerErrors(error)
    showToast(errorMessage(error))
  } finally {
    saving.value = false
  }
}
</script>

<style scoped>
.address-form { overflow: hidden; }
.address-form :deep(.van-cell) { padding: 16px; }
.form-submit { padding: 24px 16px; }
</style>
