const { request } = require('../../utils/request')
Component({
  properties: { required: Boolean },
  data: { opened: false, binding: false },
  observers: { required(value) { if (!value) this.setData({ opened: false }) } },
  methods: {
    onMaskTap() { this.setData({ opened: true }) },
    noop() {},
    onCancel() { this.setData({ opened: false }) },
    async onGetPhoneNumber(e) {
      const code = e.detail && e.detail.code
      if (!code) return wx.showToast({ title: '请授权手机号后继续使用', icon: 'none' })
      this.setData({ binding: true })
      try { await request('/api/user/phone', 'POST', { code }, true); this.setData({ opened: false }); wx.showToast({ title: '授权成功', icon: 'success' }); this.setData({ required: false })
      } catch (error) { wx.showToast({ title: '授权失败，请重试', icon: 'none' }) }
      this.setData({ binding: false })
    }
  }
})
