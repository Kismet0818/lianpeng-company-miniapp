// 联朋升学 — 通用网络请求
// useCloud=true  → 云托管 callContainer（上线）
// useCloud=false → 本地后端 wx.request（调试）
// 不能在模块加载阶段调用 getApp()：auth-gate 会在 App() 初始化前加载本模块。
// 延迟到每次请求/响应处理时再获取 App 实例。
function getAppInstance() {
  return getApp()
}

/** 统一的响应处理：code 200 → data；401 → 清 token；其他 → 提示 msg */
function handleResponse(d, resolve, reject, silent) {
  const app = getAppInstance()
  let data = d
  let parseCount = 0
  while (typeof data === 'string' && parseCount < 2) {
    try {
      data = JSON.parse(data.replace(/^\uFEFF/, '').trim())
      parseCount += 1
    } catch (e) {
      console.error('服务响应不是有效 JSON：', String(data).slice(0, 300))
      if (!silent) wx.showToast({ title: '服务响应格式错误', icon: 'none' })
      reject(e)
      return
    }
  }
  if (!data || typeof data !== 'object') {
    const error = new Error('INVALID_RESPONSE')
    if (!silent) wx.showToast({ title: '服务响应格式错误', icon: 'none' })
    reject(error)
    return
  }
  if (data.code === 200) resolve(data.data)
  else if (data.code === 401) {
    wx.removeStorageSync('token')
    app.globalData.token = ''
    reject(data)
  } else {
    if (!silent) wx.showToast({ title: data.msg, icon: 'none' })
    reject(data)
  }
}

function handleHttpError(statusCode, data, url, reject, silent) {
  const app = getAppInstance()
  const error = new Error('HTTP_' + statusCode)
  error.code = statusCode
  error.statusCode = statusCode
  error.data = data
  console.error('接口请求失败：', url, statusCode, data)
  if (statusCode === 401) {
    wx.removeStorageSync('token')
    app.globalData.token = ''
  } else if (!silent) {
    const message = data && data.msg ? data.msg : '接口请求失败(' + statusCode + ')'
    wx.showToast({ title: message, icon: 'none' })
  }
  reject(error)
}

function request(url, method = 'GET', data = {}, needAuth = false, silent = false) {
  const app = getAppInstance()
  const header = { 'Content-Type': 'application/json' }
  if (needAuth) {
    header['Authorization'] = 'Bearer ' + wx.getStorageSync('token')
  }

  // ===== 本地调试：wx.request =====
  if (app.globalData.useCloud === false) {
    return new Promise((resolve, reject) => {
      wx.request({
        url: app.globalData.localBaseUrl + url,
        method,
        data,
        header,
        success(res) {
          if (res.statusCode < 200 || res.statusCode >= 300) {
            handleHttpError(res.statusCode, res.data, url, reject, silent)
            return
          }
          handleResponse(res.data, resolve, reject, silent)
        },
        fail() {
          if (!silent) wx.showToast({ title: '网络错误', icon: 'none' })
          reject()
        }
      })
    })
  }

  // ===== 云端：wx.cloud.callContainer =====
  header['X-WX-SERVICE'] = app.globalData.cloudService
  return new Promise((resolve, reject) => {
    if (!wx.cloud || typeof wx.cloud.callContainer !== 'function') {
      const error = new Error('当前微信基础库不支持云托管')
      if (!silent) wx.showToast({ title: '当前微信版本过低，请升级后重试', icon: 'none' })
      reject(error)
      return
    }
    wx.cloud.callContainer({
      config: { env: app.globalData.cloudEnv },
      path: String(url || '').startsWith('/') ? url : '/' + url,
      method,
      data,
      header,
      success(res) {
        if (res.statusCode < 200 || res.statusCode >= 300) {
          handleHttpError(res.statusCode, res.data, url, reject, silent)
          return
        }
        handleResponse(res.data, resolve, reject, silent)
      },
      fail(error) {
        const detail = error && (error.errMsg || error.message) || 'unknown'
        console.error('云托管请求失败：', {
          url,
          env: app.globalData.cloudEnv,
          service: app.globalData.cloudService,
          detail,
          error
        })
        if (!silent) wx.showToast({ title: detail.indexOf('Failed to fetch') >= 0 ? '云服务连接失败，请检查云托管配置' : '网络错误', icon: 'none' })
        reject(error)
      }
    })
  })
}

module.exports = { request }
