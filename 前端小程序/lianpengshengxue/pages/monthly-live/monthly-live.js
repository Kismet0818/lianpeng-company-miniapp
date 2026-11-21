const { request } = require('../../utils/request')

Page({
  data: {
    livePreviews: [],
    displayLives: [],
    loading: true,
    loadFailed: false,
    reservingId: null
  },

  onLoad() {
    this.loadLives()
  },

  async loadLives() {
    this.setData({ loading: true, loadFailed: false })
    try {
      const result = await request('/api/live/list', 'GET')
      this.applyLives(Array.isArray(result) ? result : [])
    } catch (error) {
      this.setData({
        livePreviews: [],
        displayLives: [],
        loading: false,
        loadFailed: true
      })
    }
  },

  applyLives(source) {
    const list = source
      .filter(item => item && Number(item.isActive !== undefined ? item.isActive : item.is_active) === 1)
      .map(item => this.normalizeLive(item))
      .filter(item => item.liveTimestamp !== null)
      .sort((a, b) => this.compareLiveTime(a, b))

    const now = new Date()
    const weekStart = new Date(now.getFullYear(), now.getMonth(), now.getDate())
    const day = weekStart.getDay() || 7
    weekStart.setDate(weekStart.getDate() - day + 1)
    const weekEnd = new Date(weekStart)
    weekEnd.setDate(weekEnd.getDate() + 7)
    const monthStart = new Date(now.getFullYear(), now.getMonth(), 1)
    const monthEnd = new Date(now.getFullYear(), now.getMonth() + 1, 1)
    const inRange = (item, start, end) => item.liveTimestamp >= start.getTime() && item.liveTimestamp < end.getTime()
    const livePreviews = list.filter(item => inRange(item, weekStart, weekEnd))
    const displayLives = list.filter(item => inRange(item, monthStart, monthEnd))

    this.setData({ livePreviews, displayLives, loading: false, loadFailed: false })
  },

  compareLiveTime(a, b) {
    const now = Date.now()
    const aFuture = a.liveTimestamp >= now
    const bFuture = b.liveTimestamp >= now
    if (aFuture !== bFuture) return aFuture ? -1 : 1
    if (aFuture) return a.liveTimestamp - b.liveTimestamp
    return b.liveTimestamp - a.liveTimestamp
  },

  normalizeLive(item) {
    const rawTime = item.liveTime || item.live_time || ''
    const liveDate = this.parseDate(rawTime)
    const month = liveDate ? liveDate.getMonth() + 1 : 0
    const day = liveDate ? liveDate.getDate() : 0
    const hour = liveDate ? String(liveDate.getHours()).padStart(2, '0') : '--'
    const minute = liveDate ? String(liveDate.getMinutes()).padStart(2, '0') : '--'

      return {
      id: Number(item.id || 0),
      title: String(item.title || '直播公开课').trim(),
      cover: String(item.cover || '').trim(),
      liveTimeRaw: String(rawTime).trim(),
      liveTimestamp: liveDate ? liveDate.getTime() : null,
      date: liveDate ? `${month}月${day}日` : '待定',
      time: liveDate ? `${hour}:${minute}` : '待定',
      timeText: liveDate ? `${month}月${day}日 ${hour}:${minute} 直播` : '直播时间待定',
      finderUserName: String(item.finderUserName || item.finder_user_name || '').trim(),
      channelsName: String(item.channelsName || item.channels_name || '联朋升学').trim(),
      channelsAvatar: String(item.channelsAvatar || item.channels_avatar || '').trim(),
      isEnded: liveDate ? liveDate.getTime() <= Date.now() : false,
      sort: Number(item.sort || 0)
    }
  },

  parseDate(value) {
    if (!value) return null
    const trimmed = String(value).trim()
    // 兼容时间戳（10~13 位纯数字：秒或毫秒）
    if (/^\d{10,13}$/.test(trimmed)) {
      const num = Number(trimmed)
      const d = new Date(num > 1e12 ? num : num * 1000)
      return Number.isNaN(d.getTime()) ? null : d
    }
    // 带时区的 ISO 时间必须按真实时区解析，不能当作本地时间处理。
    if (/T.*(?:Z|[+-]\d{2}:?\d{2})$/.test(trimmed)) {
      const d = new Date(trimmed)
      return Number.isNaN(d.getTime()) ? null : d
    }
    const match = trimmed.match(/^(\d{4})-(\d{1,2})-(\d{1,2})(?:[ T](\d{1,2}):(\d{1,2})(?::(\d{1,2}))?)?/)
    if (!match) return null
    const date = new Date(
      Number(match[1]), Number(match[2]) - 1, Number(match[3]),
      Number(match[4] || 0), Number(match[5] || 0), Number(match[6] || 0)
    )
    return Number.isNaN(date.getTime()) ? null : date
  },

  onRetry() {
    this.loadLives()
  },

  onReserve(e) {
    const id = Number(e.currentTarget.dataset.id || 0)
    if (this.data.reservingId !== null) return
    const finderUserName = e.currentTarget.dataset.finder
    const cardTime = e.currentTarget.dataset.time || ''
    const live = [...this.data.livePreviews, ...this.data.displayLives].find(item => item.id === id)
    if (live && live.isEnded) {
      wx.showToast({ title: '该场直播已结束', icon: 'none' })
      return
    }
    this.setData({ reservingId: id })
    const finish = () => this.setData({ reservingId: null })
    if (!finderUserName) {
      wx.showToast({ title: '该场直播暂未开放预约', icon: 'none' })
      finish()
      return
    }
    if (typeof wx.getChannelsLiveNoticeInfo !== 'function' || typeof wx.reserveChannelsLive !== 'function') {
      wx.showToast({ title: '当前微信版本不支持直播预约，请升级微信后重试', icon: 'none' })
      finish()
      return
    }
    wx.showLoading({ title: '正在打开预约' })
    wx.getChannelsLiveNoticeInfo({
      finderUserName,
      success: notice => {

        // 收集该视频号所有直播预告：最近一场在主返回，其余在 otherInfos（最多 15 个）
        const notices = []
        if (notice && notice.noticeId) notices.push(notice)
        if (notice && Array.isArray(notice.otherInfos)) notices.push(...notice.otherInfos)

        // 优先按开始时间匹配当前点击的场次
        let target = cardTime
          ? notices.find(n => this.matchLiveTime(n.startTime, cardTime))
          : null
        // 卡片无时间时，退而取最近那场（时间最近的可预约预告）
        if (!target && !cardTime && notices.length) {
          target = notices[0]
        }

        if (!target) {
          wx.hideLoading()
          console.warn('未找到对应的微信直播预告', {
            finderUserName,
            cardTime,
            noticeTimes: notices.map(item => item && item.startTime)
          })
          wx.showToast({ title: '暂未获取到该场直播预告', icon: 'none' })
          finish()
          return
        }
        if (target.reservable === false || Number(target.status) !== 0) {
          finish()
          wx.hideLoading()
          console.warn('微信直播预告当前不可预约', target)
          wx.showToast({ title: '该场直播暂未开放预约', icon: 'none' })
          return
        }
        if (!target.noticeId) {
          wx.hideLoading()
          wx.showToast({ title: '暂未获取到该场直播预告', icon: 'none' })
          finish()
          return
        }
        wx.reserveChannelsLive({
          noticeId: target.noticeId,
          success: result => {
            wx.hideLoading()
            finish()
            const state = Number(result && result.state)
            console.log('视频号直播预约结果', { state, noticeId: target.noticeId, finderUserName, cardTime })
            if (state === 6) return wx.showToast({ title: '预约成功', icon: 'success' })
            // 5/8/9 都是用户关闭弹窗或取消操作，不应误报系统失败。
            if (state === 5 || state === 8 || state === 9) return
            if (state === 7) return wx.showToast({ title: '已取消预约', icon: 'none' })
            if (state === 3) return wx.showToast({ title: '该直播预告已取消', icon: 'none' })
            if (state === 4 || state === 10) return wx.showToast({ title: '该直播已结束或已过期', icon: 'none' })
            wx.showToast({ title: '直播预约未完成，请稍后重试', icon: 'none' })
          },
          fail: err => {
            wx.hideLoading()
            finish()
            console.error('视频号直播预约接口失败', { err, noticeId: target.noticeId, finderUserName, cardTime })
            if (!err || !String(err.errMsg || '').includes('cancel')) {
              const message = String(err && (err.errMsg || err.errCode) || '')
              wx.showToast({
                title: message.indexOf('100008') >= 0
                  ? '小程序与该视频号未关联'
                  : '直播预约未完成，请稍后重试',
                icon: 'none'
              })
            }
          }
        })
      },
      fail: err => {
        wx.hideLoading()
        finish()
        if (!err || !String(err.errMsg || '').includes('cancel')) {
          wx.showToast({ title: '直播预约未完成，请稍后重试', icon: 'none' })
        }
      }
    })
  },

  /** 比对两个时间是否同一场（精确到分钟，忽略秒） */
  matchLiveTime(startTime, rawTime) {
    const a = this.parseDate(startTime)
    const b = this.parseDate(rawTime)
    if (!a || !b) return false
    return a.getFullYear() === b.getFullYear() &&
           a.getMonth() === b.getMonth() &&
           a.getDate() === b.getDate() &&
           a.getHours() === b.getHours() &&
           a.getMinutes() === b.getMinutes()
  },

  onConsult() {
    wx.navigateTo({ url: '/pages/lead/lead?target=' + encodeURIComponent('直播课程咨询') + '&source=monthly-live' })
  },

  onShareAppMessage() {
    return { title: '联朋升学｜本月直播公开课', path: '/pages/monthly-live/monthly-live' }
  }
})
