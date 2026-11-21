const { request } = require('./request')

function startSchoolView(page, schoolSource, schoolId) {
  const id = Number(schoolId)
  if (!page || !schoolSource || !Number.isInteger(id) || id <= 0) {
    console.warn('[activity] 无效的学校浏览参数', { schoolSource, schoolId })
    return
  }
  page._schoolView = { schoolSource, schoolId: id, startedAt: Date.now(), sending: false, sent: false }
  console.info('[activity] 开始学校详情计时', { schoolSource, schoolId: id })
}

function finishSchoolView(page) {
  const view = page && page._schoolView
  if (!view || view.sent || view.sending || !view.schoolId) return Promise.resolve(false)
  const durationSeconds = Math.floor((Date.now() - view.startedAt) / 1000)
  if (durationSeconds < 3) return Promise.resolve(false)
  if (!wx.getStorageSync('token')) {
    console.warn('[activity] 本地没有 token，无法上报学校浏览行为')
    return Promise.resolve(false)
  }
  view.sending = true
  return request('/api/user/activity', 'POST', { schoolSource: view.schoolSource, schoolId: view.schoolId, durationSeconds }, true, true)
    .then(() => { view.sent = true; view.sending = false; console.info('[activity] 学校浏览行为上报成功', { schoolSource: view.schoolSource, schoolId: view.schoolId, durationSeconds }); return true })
    .catch(error => { view.sending = false; console.error('[activity] 学校浏览行为上报失败', { schoolSource: view.schoolSource, schoolId: view.schoolId, durationSeconds, error }); return false })
}

module.exports = { startSchoolView, finishSchoolView }
