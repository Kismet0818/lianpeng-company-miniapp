/**
 * 联朋升学 — OSS 文件上传工具（直传阿里云 OSS，HTTPS）
 * 
 * 流程：
 *   1. 通过云托管 callContainer 获取直传签名（/api/oss/policy，需登录）
 *   2. 用 wx.uploadFile 直接把文件上传到 OSS（HTTPS）
 * 
 * 使用示例：
 *   const { upload } = require('../../utils/upload')
 *   const url = await upload(filePath, 'logo')
 */

const app = getApp()
const { request } = require('./request')

/** 从临时路径提取扩展名（wx.chooseMedia 返回 wxfile://tmp_xxx.jpg） */
function getExt(filePath) {
  const m = String(filePath || '').match(/\.([a-zA-Z0-9]+)(\?|$)/)
  return m ? m[1].toLowerCase() : 'jpg'
}

/**
 * 上传单张图片到阿里云 OSS（直传，HTTPS）
 * 
 * @param {string}  filePath - wx.chooseImage / wx.chooseMedia 返回的临时路径
 * @param {string}  folder   - OSS 存储目录（logo / gallery / qr / guide / banner / avatar）
 * @returns {Promise<string>} 上传成功返回 OSS 公网 HTTPS URL
 */
async function upload(filePath, folder = '') {
  // 1. 通过云托管获取直传签名（需登录，自动带 Authorization）
  const policy = await request('/api/oss/policy', 'POST', { dir: folder }, true)

  // 2. 生成 OSS 对象 key（目录/时间戳_随机.ext）
  const ext = getExt(filePath)
  const name = Date.now() + '_' + Math.floor(Math.random() * 1e6) + '.' + ext
  const key = (folder ? folder + '/' : '') + name

  // 3. 直传 OSS（HTTPS，返回 204 表示成功）
  return new Promise((resolve, reject) => {
    wx.uploadFile({
      url: policy.host,
      filePath,
      name: 'file',
      formData: {
        key,
        policy: policy.policy,
        OSSAccessKeyId: policy.accessid,
        signature: policy.signature
      },
      success(res) {
        if (res.statusCode === 204) {
          resolve(policy.host + '/' + key)
        } else {
          console.error('OSS 上传失败：', res.statusCode, res.data)
          wx.showToast({ title: '上传失败', icon: 'none' })
          reject(new Error('upload failed: ' + res.statusCode))
        }
      },
      fail(err) {
        wx.showToast({ title: '网络错误', icon: 'none' })
        reject(err)
      }
    })
  })
}

module.exports = { upload }
