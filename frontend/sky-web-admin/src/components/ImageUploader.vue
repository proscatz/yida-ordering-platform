<template>
  <div class="image-uploader">
    <div class="image-uploader__preview" :class="{ 'image-uploader__preview--empty': !modelValue }">
      <img v-if="modelValue" :src="modelValue" alt="商品图片预览" />
      <div v-else><Picture /><span>等待上传图片</span></div>
      <div v-if="uploading" class="image-uploader__progress"><el-progress type="circle" :percentage="progress" :width="72" /></div>
    </div>
    <div class="image-uploader__actions">
      <el-upload
        accept=".jpg,.jpeg,.png,.webp,.gif,image/jpeg,image/png,image/webp,image/gif"
        :show-file-list="false"
        :http-request="handleUpload"
        :disabled="uploading"
      >
        <el-button type="primary" plain :loading="uploading">{{ modelValue ? '重新上传' : '选择图片' }}</el-button>
      </el-upload>
      <el-button v-if="modelValue" :disabled="uploading" @click="$emit('update:modelValue', '')">移除</el-button>
    </div>
    <p class="image-uploader__hint">JPG、PNG、WEBP 或 GIF，文件不超过 5MB</p>
    <el-alert v-if="uploadError" :title="uploadError" type="error" :closable="false" show-icon />
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { Picture } from '@element-plus/icons-vue'
import type { UploadRequestOptions } from 'element-plus'
import { uploadImage } from '@/api/upload'
import { userFacingError } from '@/api/errors'
import { validateImageFileSignature } from '@/utils/catalog'

defineProps<{ modelValue: string }>()
const emit = defineEmits<{ 'update:modelValue': [value: string] }>()
const uploading = ref(false)
const progress = ref(0)
const uploadError = ref('')

async function handleUpload(options: UploadRequestOptions) {
  const validationError = await validateImageFileSignature(options.file)
  if (validationError) {
    uploadError.value = validationError
    options.onError(new Error(validationError) as never)
    return
  }

  uploading.value = true
  progress.value = 0
  uploadError.value = ''
  try {
    const url = await uploadImage(options.file, (value) => { progress.value = value })
    progress.value = 100
    emit('update:modelValue', url)
    options.onSuccess(url)
  } catch (error) {
    uploadError.value = userFacingError(error, '图片上传失败')
    options.onError(error instanceof Error ? error as never : new Error(uploadError.value) as never)
  } finally {
    uploading.value = false
  }
}
</script>

<style scoped>
.image-uploader { width: 100%; }
.image-uploader__preview { position: relative; width: 200px; height: 150px; overflow: hidden; border: 1px solid var(--line); border-radius: 16px; background: #f5f8f6; }
.image-uploader__preview img { width: 100%; height: 100%; object-fit: cover; }
.image-uploader__preview--empty > div { height: 100%; display: grid; place-items: center; align-content: center; gap: 8px; color: #9aa6a3; font-size: 11px; }
.image-uploader__preview--empty svg { width: 34px; }
.image-uploader__progress { position: absolute; inset: 0; display: grid; place-items: center; background: rgba(255,255,255,.88); backdrop-filter: blur(4px); }
.image-uploader__actions { margin-top: 12px; display: flex; align-items: center; gap: 9px; }
.image-uploader__hint { margin: 8px 0 10px; color: var(--muted); font-size: 10px; }
</style>
