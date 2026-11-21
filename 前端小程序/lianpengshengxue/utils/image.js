// 联朋升学 — 图片地址统一解析（强制 HTTPS + OSS 图片压缩）
// 使用：const { resolveImageUrl } = require('../../utils/image')
// 规则：
//   https://  → 直接使用
//   http://   → 拦截（正式版小程序不允许 HTTP 资源）
//   相对路径  → 拼接到 OSS 域名
//   OSS 图片(jpg/png/bmp) → 自动追加 x-oss-process 转 webp 压缩，省下行流量

const OSS_HOST = 'https://lianpeng.oss-cn-shanghai.aliyuncs.com'
// 图片处理参数：转 webp + 质量 80（视觉无损）
const OSS_PROCESS = '?x-oss-process=image/format,webp/quality,q_80'

/** 是否为需要压缩的 OSS 图片（排除 svg 矢量图、gif 动图、webp 已压缩、pdf 非图片） */
function isCompressibleOssImage(value) {
  return value.startsWith(OSS_HOST + '/') &&
    /\.(jpe?g|png|bmp)$/i.test(value.split('?')[0])
}

function resolveImageUrl(url) {
  const value = String(url || '').trim()
  if (!value) return ''

  let full = value
  if (value.startsWith('https://')) {
    full = value
  } else if (value.startsWith('http://')) {
    console.warn('已拦截非 HTTPS 图片地址：', value)
    return ''
  } else {
    full = OSS_HOST + '/' + value.replace(/^\/+/, '')
  }

  // OSS 图片且尚未带处理参数 → 追加 webp 压缩
  if (isCompressibleOssImage(full) && !full.includes('x-oss-process=')) {
    full += OSS_PROCESS
  }
  return full
}

module.exports = { resolveImageUrl }
