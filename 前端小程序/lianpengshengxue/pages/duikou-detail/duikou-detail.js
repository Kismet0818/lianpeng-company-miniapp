const { request } = require('../../utils/request')

const DISTRICT_PATTERN = /(黄浦|徐汇|长宁|静安|普陀|虹口|杨浦|闵行|宝山|嘉定|浦东新区|金山|松江|青浦|奉贤|崇明)区?/

Page({
  data: {
    id: '',
    safeTop: 20,
    article: null,
    blocks: [],
    loading: true,
    error: ''
  },

  onLoad(options) {
    let safeTop = 20
    try {
      const menu = wx.getMenuButtonBoundingClientRect()
      safeTop = menu && menu.top ? menu.top : 20
    } catch (e) {}
    this.setData({ id: options.id || '', safeTop })
    this.loadDetail()
  },

  goBack() { wx.navigateBack() },

  async loadDetail() {
    if (!this.data.id) {
      this.setData({ loading: false, error: '资料参数不完整' })
      return
    }
    this.setData({ loading: true, error: '' })
    try {
      const article = await request('/api/duikou/' + encodeURIComponent(this.data.id))
      if (!article) throw new Error('empty')
      const m = DISTRICT_PATTERN.exec(article.title || '')
      this.setData({
        article: {
          ...article,
          district: m ? (m[1].endsWith('区') ? m[1] : m[1] + '区') : '',
          timeText: this.formatTime(article.updatedTime || article.createdTime)
        },
        blocks: this.parseBlocks(article.content || ''),
        loading: false
      })
    } catch (e) {
      this.setData({ loading: false, error: '资料加载失败，请稍后重试' })
    }
  },

  /** 将 HTML 正文拆分为：表格块（原生组件渲染）、图片块、富文本块；否则 rich-text 无法渲染 <table> */
  parseBlocks(html) {
    const blocks = []
    const tokenRe = /<table[\s\S]*?<\/table>|<img[^>]+src=["']([^"']+)["'][^>]*>|<a[^>]+href=["']([^"']+)["'][^>]*>[\s\S]*?<\/a>/gi
    let lastIndex = 0
    let m
    while ((m = tokenRe.exec(html))) {
      const between = html.slice(lastIndex, m.index).trim()
      if (between) blocks.push({ type: 'html', html: between })
      const raw = m[0]
      if (/^<table/i.test(raw)) {
        const t = this.parseTable(raw)
        if (t.rows.length) {
          blocks.push({ type: 'table', rows: t.rows, captions: t.captions, headerIndex: t.captions.findIndex(c => !c) })
        }
      } else if (/^<img/i.test(raw)) {
        const src = (m[1] || '').replace(/\\\//g, '/').trim()
        if (/^https?:\/\//i.test(src)) blocks.push({ type: 'img', src })
      } else {
        const url = (m[2] || '').replace(/&amp;/gi, '&').replace(/\\\//g, '/').trim()
        const label = this.htmlToText(raw) || '打开附件'
        if (/^https?:\/\//i.test(url)) blocks.push({ type: 'link', url, label })
      }
      lastIndex = m.index + raw.length
    }
    const tail = html.slice(lastIndex).trim()
    if (tail) blocks.push({ type: 'html', html: tail })
    return blocks
  },

  /** 解析 <table> 为二维字符串数组，处理 rowspan/colspan 占据 */
  parseTable(tableHtml) {
    const trs = tableHtml.match(/<tr[^>]*>[\s\S]*?<\/tr>/gi) || []
    const grid = []
    for (const tr of trs) {
      const cells = tr.match(/<t[dh][^>]*>[\s\S]*?<\/t[dh]>/gi) || []
      const row = []
      for (const cellHtml of cells) {
        const openTag = (cellHtml.match(/^<t[dh]([^>]*)>/i) || [])[1] || ''
        const colspan = parseInt(((openTag.match(/colspan\s*=\s*["']?(\d+)/i) || [])[1]) || 1, 10)
        const rowspan = parseInt(((openTag.match(/rowspan\s*=\s*["']?(\d+)/i) || [])[1]) || 1, 10)
        row.push({ text: this.htmlToText(cellHtml), colspan, rowspan })
      }
      if (row.length) grid.push(row)
    }
    return this.expandTable(grid)
  },

  expandTable(grid) {
    const matrix = []
    for (let r = 0; r < grid.length; r++) {
      if (!matrix[r]) matrix[r] = []
      let c = 0
      for (const cell of grid[r]) {
        while (matrix[r][c] !== undefined) c++
        const rs = Math.max(1, cell.rowspan)
        const cs = Math.max(1, cell.colspan)
        for (let dr = 0; dr < rs; dr++) {
          if (!matrix[r + dr]) matrix[r + dr] = []
          for (let dc = 0; dc < cs; dc++) {
            matrix[r + dr][c + dc] = (dr === 0 && dc === 0) ? cell.text : ''
          }
        }
        c += cs
      }
    }
    const colCount = Math.max(1, ...matrix.map(row => row.length))
    const rows = matrix.map(row => {
      const out = []
      for (let i = 0; i < colCount; i++) out.push(row[i] === undefined ? '' : row[i])
      return out
    })
    // 整行只有一个非空单元格时视为合并的标题行，并折叠为单元素便于渲染
    const captions = rows.map(row => row.filter(cell => cell !== '').length === 1)
    const finalRows = rows.map((row, i) => captions[i] ? [row.find(cell => cell !== '') || ''] : row)
    return { rows: finalRows, captions }
  },

  htmlToText(html) {
    return html
      .replace(/<br\s*\/?>/gi, '\n')
      .replace(/<\/p>/gi, '\n')
      .replace(/<[^>]*>/g, '')
      .replace(/&nbsp;/gi, ' ')
      .replace(/&amp;/gi, '&')
      .replace(/&lt;/gi, '<')
      .replace(/&gt;/gi, '>')
      .replace(/&quot;/gi, '"')
      .replace(/&#39;|&apos;/gi, "'")
      .replace(/[ \t]+/g, ' ')
      .replace(/\n{2,}/g, '\n')
      .trim()
  },

  formatTime(value) {
    if (!value) return ''
    return String(value).replace('T', ' ').slice(0, 16)
  },

  previewImage(e) {
    const src = e.currentTarget.dataset.src
    const urls = this.data.blocks.filter(b => b.type === 'img').map(b => b.src)
    wx.previewImage({ current: src, urls })
  },

  openFileLink(e) {
    const url = e.currentTarget.dataset.url
    if (!url || (this._linkLongPressedAt && Date.now() - this._linkLongPressedAt < 800)) return
    const cleanUrl = String(url).split('#')[0].split('?')[0]
    const extension = (cleanUrl.match(/\.([a-zA-Z0-9]+)$/) || [])[1]
    const supportedTypes = ['doc', 'docx', 'xls', 'xlsx', 'ppt', 'pptx', 'pdf']
    const fileType = supportedTypes.includes((extension || '').toLowerCase()) ? extension.toLowerCase() : undefined

    wx.showLoading({ title: '正在打开', mask: true })
    wx.downloadFile({
      url,
      success: (res) => {
        if (res.statusCode !== 200 || !res.tempFilePath) {
          wx.showToast({ title: '文件下载失败', icon: 'none' })
          return
        }
        const options = {
          filePath: res.tempFilePath,
          showMenu: true,
          fail: () => wx.showToast({ title: '微信无法打开该文件', icon: 'none' })
        }
        if (fileType) options.fileType = fileType
        wx.openDocument(options)
      },
      fail: () => wx.showToast({ title: '打开失败，请长按复制链接', icon: 'none' }),
      complete: () => wx.hideLoading()
    })
  },

  copyFileLink(e) {
    const url = e.currentTarget.dataset.url
    if (!url) return
    this._linkLongPressedAt = Date.now()
    wx.setClipboardData({
      data: url,
      success: () => wx.showToast({ title: '链接已复制', icon: 'success' }),
      fail: () => wx.showToast({ title: '复制失败', icon: 'none' })
    })
  }
})
