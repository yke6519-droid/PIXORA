import fs from 'node:fs'
import path from 'node:path'
import { fileURLToPath } from 'node:url'

const scriptDirectory = path.dirname(fileURLToPath(import.meta.url))
const apiDirectory = path.resolve(scriptDirectory, '../src/api')

function readApiFile(fileName) {
  return fs.readFileSync(path.join(apiDirectory, fileName), 'utf8')
}

function writeApiFile(fileName, content) {
  fs.writeFileSync(path.join(apiDirectory, fileName), content, 'utf8')
}

/**
 * 替换生成文件中的一个完整导出函数，避免依赖具体的行号。
 * OpenAPI 生成器升级后，只要函数名仍然存在，这个补丁就能继续工作。
 */
function replaceExportFunction(source, functionName, replacement) {
  const functionIndex = source.indexOf(`export async function ${functionName}`)
  if (functionIndex < 0) throw new Error(`未找到生成函数: ${functionName}`)

  const commentStart = source.lastIndexOf('/**', functionIndex)
  const nextComment = source.indexOf('/**', functionIndex)
  if (commentStart < 0 || nextComment < 0) {
    throw new Error(`无法定位生成函数边界: ${functionName}`)
  }

  return `${source.slice(0, commentStart)}${replacement.trim()}\n\n${source.slice(nextComment)}`
}

let fileController = readApiFile('fileController.ts')
fileController = replaceExportFunction(
  fileController,
  'avatarUpload',
  `/** 头像上传：后端使用 @RequestParam("avatar") 接收 multipart 文件。 */
export async function avatarUpload(file: File, options?: { [key: string]: any }) {
  const formData = new FormData()
  formData.append('avatar', file)

  return request<API.BaseResponseUploadAvatarVO>('/file/avatarUpload', {
    method: 'POST',
    data: formData,
    ...(options || {}),
  })
}`,
)
writeApiFile('fileController.ts', fileController)

let pictureController = readApiFile('pictureController.ts')
pictureController = replaceExportFunction(
  pictureController,
  'reloadPicture',
  `/** 重新上传：后端通过 multipart 的 file/url 和 RequestParam 元信息接收请求。 */
export async function reloadPicture(
  params: API.reloadPictureParams,
  body: {},
  file?: File,
  options?: { [key: string]: any },
) {
  const formData = new FormData()
  if (file) formData.append('file', file)

  Object.entries({ ...params, ...body }).forEach(([key, value]) => {
    if (value === undefined || value === null) return
    if (Array.isArray(value)) {
      value.forEach((item) => formData.append(key, String(item)))
      return
    }
    formData.append(key, String(value))
  })

  return request<API.BaseResponsePictureVO>('/picture/reloadPicture', {
    method: 'POST',
    data: formData,
    ...(options || {}),
  })
}`,
)
pictureController = pictureController.replace(/^\s*requestType:\s*["']form["'],\r?\n/m, '')
writeApiFile('pictureController.ts', pictureController)

let typings = readApiFile('typings.d.ts')
// Java Long 在后端 JSON 中按字符串传输；兼容 OpenAPI 生成的必填和可选字段。
typings = typings.replace(
  /(id|spaceId|userId|picId|checkAdminId|picsize|maxSize|usedSize|maxCount|usedCount|total|totalSize|size)(\?)?: number;/g,
  '$1$2: number | string;',
)
typings = typings.replace(/(picIds|ids)\?: number\[\];/g, '$1?: Array<number | string>;')
writeApiFile('typings.d.ts', typings)

console.log('[openAPI]: ✅ 已应用前端原型兼容补丁')
