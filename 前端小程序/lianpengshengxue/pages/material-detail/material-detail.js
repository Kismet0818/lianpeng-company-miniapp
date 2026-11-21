const { request } = require('../../utils/request')
const app = getApp()

Page({
  data: { id: '', material: null, loading: true, error: '', navTop: 20, navHeight: 32, navBarHeight: 64, contentTop: 72, previewVisible: false, previewUrl: '', showQr: false, qrImageUrl: 'https://lianpeng.oss-cn-shanghai.aliyuncs.com/lead-materials/lingziliao.png' },
  onLoad(options) { this.initNavigation(); this.setData({ id: options.id || '' }); wx.setNavigationBarTitle({ title: '择校资料' }); this.load() },
  initNavigation() {
    let navTop = 20
    let navHeight = 32
    let navBarHeight = 64
    try {
      const menu = wx.getMenuButtonBoundingClientRect()
      if (menu && menu.top && menu.height) {
        navTop = menu.top
        navHeight = menu.height
        navBarHeight = menu.bottom + 8
      }
    } catch (error) {
      console.warn('获取导航栏尺寸失败，使用默认尺寸', error)
    }
    this.setData({ navTop, navHeight, navBarHeight, contentTop: navBarHeight + 8 })
  },
  async load() { try { const material = await request('/api/school-materials/' + encodeURIComponent(this.data.id)); this.setData({ material: { ...material, attachments: Array.isArray(material.attachments) ? material.attachments : [] }, loading: false }) } catch (e) { this.setData({ loading: false, error: '资料不存在或已下架' }) } },
  async openAttachment(e) {
    const item = e.currentTarget.dataset.item
    if (!item) return
    const extension = String(item.extension || item.fileExtension || 'pdf').toLowerCase()
    const previewUrl = item.previewUrl || item.preview_url || item.firstPageUrl || item.first_page_url
    if (previewUrl && ['jpg', 'jpeg', 'png', 'gif', 'webp'].indexOf(extension) >= 0) {
      this.setData({ previewVisible: true, previewUrl })
      return
    }
    if (['pdf', 'doc', 'docx', 'xls', 'xlsx', 'ppt', 'pptx'].indexOf(extension) >= 0) {
      return this.downloadAttachment(e, true)
    }
    wx.showToast({ title: '该文件格式暂不支持预览', icon: 'none' })
  },
  openQr() { this.setData({ showQr: true }) },
  closeQr() { this.setData({ showQr: false }) },
  closePreview() { this.setData({ previewVisible: false, previewUrl: '' }) },
  noop() {},
  openDownloadedDocument(tempFilePath, item, previewOnly) {
    const extension = String(item.extension || item.fileExtension || 'pdf').toLowerCase().replace(/^\./, '')
    const supportedExtensions = ['pdf', 'doc', 'docx', 'xls', 'xlsx', 'ppt', 'pptx']
    const fileType = supportedExtensions.includes(extension) ? extension : 'pdf'
    const targetPath = `${wx.env.USER_DATA_PATH}/material-preview-${item.id || this.data.id}.${fileType}`
    const fileSystem = wx.getFileSystemManager()

    const openDocument = filePath => {
      wx.openDocument({
        filePath,
        fileType,
        showMenu: !previewOnly,
        success: () => console.info('政策文件打开成功', { filePath, fileType }),
        fail: error => {
          console.error('政策文件打开失败', { error, filePath, fileType, item })
          wx.showToast({ title: '文件打开失败，请在真机重试', icon: 'none' })
        }
      })
    }

    const copyAndOpen = () => {
      fileSystem.copyFile({
        srcPath: tempFilePath,
        destPath: targetPath,
        success: () => openDocument(targetPath),
        fail: error => {
          console.warn('复制预览文件失败，尝试直接打开临时文件', error)
          openDocument(tempFilePath)
        }
      })
    }

    // 同一材料再次预览时先删除旧文件，避免 copyFile 因目标已存在而失败。
    fileSystem.unlink({ filePath: targetPath, complete: copyAndOpen })
  },
  async downloadAttachment(e, previewOnly = false) {
    const item = e.currentTarget.dataset.item
    if (!item || !item.url) return wx.showToast({ title: '附件地址无效', icon: 'none' })

    // 下载接口必须携带当前登录态，不能直接打开附件 URL。
    let token = wx.getStorageSync('token')
    if (!token) {
      try {
        token = await app.login()
      } catch (error) {
        wx.showToast({ title: '请先登录后下载', icon: 'none' })
        return
      }
    }
    if (!token) return wx.showToast({ title: '请先登录后下载', icon: 'none' })

    wx.showLoading({ title: previewOnly ? '正在加载文件' : '正在下载', mask: true })
    wx.downloadFile({
      url: encodeURI(item.url),
      header: { Authorization: 'Bearer ' + token },
      success: res => {
        const statusCode = Number(res.statusCode)
        if (statusCode === 200) {
          this.openDownloadedDocument(res.tempFilePath, item, previewOnly)
          return
        }
        if (statusCode === 401) {
          wx.removeStorageSync('token')
          app.globalData.token = ''
          wx.showToast({ title: '登录已失效，请重新登录', icon: 'none' })
          return
        }
        if (statusCode === 403) return wx.showToast({ title: '账号不可用，请联系客服', icon: 'none' })
        if (statusCode === 404) {
          wx.showToast({ title: '资料不存在或已下架', icon: 'none' })
          this.load()
          return
        }
        if (statusCode === 429) return wx.showToast({ title: '操作频繁，请稍后再试', icon: 'none' })
        if (statusCode >= 500) {
          console.error('资料下载服务异常：', statusCode, res.data)
          return wx.showToast({ title: '服务异常，请稍后再试', icon: 'none' })
        }
        wx.showToast({ title: '下载失败（' + statusCode + '）', icon: 'none' })
      },
      fail: error => {
        console.error('资料下载网络异常：', error)
        wx.showToast({ title: '网络异常，下载失败', icon: 'none' })
      },
      complete: () => wx.hideLoading()
    })
  },
  goBack() { wx.navigateBack({ delta: 1 }) }
})
