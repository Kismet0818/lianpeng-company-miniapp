const { request } = require('../../utils/request')
const app = getApp()

Page({
  data: {
    id: '',
    article: null,
    isHtml: false,
    loading: true,
    error: '',
    safeContent: '',
    articleImages: [],
    thumbnailImage: null,
    savingImage: false,
    previewVisible: false,
    previewUrl: '',
    showQr: false,
    qrImageUrl: 'https://lianpeng.oss-cn-shanghai.aliyuncs.com/lead-materials/lingziliao.png'
  },

  onLoad(options) {
    this.setData({ id: options.id || '' })
    this.loadArticle()
  },

  noop() {},

  async loadArticle() {
    if (!this.data.id) {
      this.setData({ loading: false, error: '文章参数不完整' })
      return
    }
    this.setData({ loading: true, error: '' })
    try {
      const article = await request('/api/guide/article/' + encodeURIComponent(this.data.id))
      if (!article) throw new Error('empty article')
      const content = article.content || ''
      const normalizedAttachments = Array.isArray(article.attachments) ? article.attachments : []
      const articleImages = this.extractImages(content)
      normalizedAttachments.forEach(item => {
        const extension = String(item.fileExtension || item.extension || '').toLowerCase()
        if (item.fileUrl && ['jpg', 'jpeg', 'png', 'gif', 'webp'].indexOf(extension) >= 0) {
          articleImages.push({
            url: item.fileUrl,
            fileName: item.fileName || '升学指南附件.' + extension,
            attachmentId: item.attachmentId != null ? item.attachmentId : item.id
          })
        }
      })
      this.setData({
        article: {
          ...article,
          attachments: normalizedAttachments
        },
        articleImages,
        thumbnailImage: article.thumbnailUrl ? { url: article.thumbnailUrl, fileName: '升学指南封面.jpg' } : null,
        safeContent: String(content).replace(/<img\b[^>]*>/gi, ''),
        isHtml: /<[^>]+>/.test(content),
        loading: false
      })
      wx.setNavigationBarTitle({ title: article.pathName || '文章详情' })
    } catch (e) {
      this.setData({ loading: false, error: '文章加载失败，请稍后重试' })
    }
  },

  extractImages(content) {
    const urls = []
    const add = url => {
      const value = String(url || '').trim()
      if (value && urls.indexOf(value) < 0) urls.push(value)
    }
    const html = String(content || '')
    const pattern = /<img[^>]+src=["']([^"']+)["'][^>]*>/gi
    let match
    while ((match = pattern.exec(html))) add(match[1])
    return urls.map((url, index) => ({ url, fileName: '升学指南图片' + (index + 1) + '.jpg' }))
  },

  async saveGuideImage(e) {
    if (this.data.savingImage) return
    const item = e.currentTarget.dataset.item
    if (!item || !item.url || !this.data.article) return
    let token = wx.getStorageSync('token')
    if (!token) {
      try { token = await app.login() } catch (error) {
        wx.showToast({ title: '请先登录后保存', icon: 'none' })
        return
      }
    }
    if (!token) return wx.showToast({ title: '请先登录后保存', icon: 'none' })

    this.setData({ savingImage: true })
    wx.showLoading({ title: '正在保存', mask: true })
    wx.downloadFile({
      url: encodeURI(item.url),
      header: { Authorization: 'Bearer ' + token },
      success: res => {
        if (Number(res.statusCode) !== 200) {
          if (Number(res.statusCode) === 401 || Number(res.statusCode) === 403) {
            wx.removeStorageSync('token')
            app.globalData.token = ''
            wx.showToast({ title: '登录已失效，请重新登录', icon: 'none' })
          } else if (Number(res.statusCode) === 404) wx.showToast({ title: '图片不存在或已下架', icon: 'none' })
          else wx.showToast({ title: '图片加载失败，请稍后再试', icon: 'none' })
          return
        }
        wx.saveImageToPhotosAlbum({
          filePath: res.tempFilePath,
          success: () => this.reportGuideDownload(item),
          fail: error => {
            console.error('保存指南图片失败：', error)
            wx.showToast({ title: '保存失败，请授权相册权限', icon: 'none' })
          }
        })
      },
      fail: error => {
        console.error('指南图片下载失败：', error)
        wx.showToast({ title: '网络异常，保存失败', icon: 'none' })
      },
      complete: () => {
        this.setData({ savingImage: false })
        wx.hideLoading()
      }
    })
  },

  async reportGuideDownload(item) {
    try {
      const payload = {
        sourceType: 'GUIDE',
        sourceId: Number(this.data.id)
      }
      if (item.attachmentId != null) payload.attachmentId = item.attachmentId
      else {
        payload.fileUrl = item.url
        payload.fileName = item.fileName
      }
      await request('/api/material-downloads/report', 'POST', payload, true)
      wx.showToast({ title: '已保存到相册', icon: 'success' })
    } catch (error) {
      console.error('指南资料保存记录上报失败：', error)
      wx.showToast({ title: '已保存到相册', icon: 'success' })
    }
  },

  openPolicyAttachment(e) {
    const { url, name, extension } = e.currentTarget.dataset
    if (!url) {
      wx.showToast({ title: '附件地址无效', icon: 'none' })
      return
    }
    const type = String(extension || '').toLowerCase()
    if (['jpg', 'jpeg', 'png', 'gif', 'webp'].indexOf(type) >= 0) {
      this.setData({ previewUrl: url, previewVisible: true })
      return
    }
    this.openDownloadedDocument(url, extension, name, false)
  },

  downloadHighQuality(e) {
    this.setData({ showQr: true })
  },

  openDownloadedDocument(url, extension, fileName, allowSave) {
    const fileType = String(extension || 'pdf').toLowerCase().replace(/^\./, '')
    const supportedTypes = ['pdf', 'doc', 'docx', 'xls', 'xlsx', 'ppt', 'pptx']
    const safeType = supportedTypes.indexOf(fileType) >= 0 ? fileType : 'pdf'
    wx.showLoading({ title: '正在下载', mask: true })
    wx.downloadFile({
      url: encodeURI(url),
      header: { Authorization: 'Bearer ' + wx.getStorageSync('token') },
      success: res => {
        const statusCode = Number(res.statusCode)
        if (statusCode !== 200) {
          if (statusCode === 404) wx.showToast({ title: '附件不存在或已下架', icon: 'none' })
          else if (statusCode >= 500) wx.showToast({ title: '服务异常，请稍后再试', icon: 'none' })
          else wx.showToast({ title: '下载失败（' + statusCode + '）', icon: 'none' })
          return
        }

        // 临时下载路径通常没有扩展名，开发者工具和部分真机会因此无法识别 PDF。
        const rawName = String(fileName || ('升学指南附件.' + safeType))
        const baseName = rawName.replace(/[\\/:*?"<>|]/g, '_').replace(/\.[^.]+$/, '').trim().slice(0, 80) || '升学指南附件'
        const targetPath = `${wx.env.USER_DATA_PATH}/${baseName}.${safeType}`
        const fileSystem = wx.getFileSystemManager()
        const open = filePath => wx.openDocument({
          filePath,
          fileType: safeType,
          showMenu: Boolean(allowSave),
          fail: error => {
            console.error('指南附件预览失败：', { error, filePath, safeType })
            wx.showToast({ title: '文件打开失败，请在真机重试', icon: 'none' })
          }
        })
        const copyAndOpen = () => fileSystem.copyFile({
          srcPath: res.tempFilePath,
          destPath: targetPath,
          success: () => open(targetPath),
          fail: error => {
            console.warn('复制指南预览文件失败，尝试直接打开临时文件：', error)
            open(res.tempFilePath)
          }
        })
        fileSystem.unlink({ filePath: targetPath, complete: copyAndOpen })
      },
      fail: error => {
        console.error('高清文件下载失败：', error)
        wx.showToast({ title: '网络异常，下载失败', icon: 'none' })
      },
      complete: () => wx.hideLoading()
    })
  },

  closePreview() {
    this.setData({ previewVisible: false, previewUrl: '' })
  },

  closeQr() {
    this.setData({ showQr: false })
  }
})
