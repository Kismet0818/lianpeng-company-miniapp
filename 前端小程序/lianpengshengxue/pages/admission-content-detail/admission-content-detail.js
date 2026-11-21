const { request } = require('../../utils/request')

function decodeEntities(value) {
  return String(value || '').replace(/&nbsp;/gi, ' ').replace(/&amp;/gi, '&').replace(/&lt;/gi, '<').replace(/&gt;/gi, '>').replace(/&quot;/gi, '"').replace(/&#39;/gi, "'")
}

function normalizeRichText(value) {
  let html = decodeEntities(value).trim()
  if (!html) return '<p>正文内容正在整理中</p>'
  if (!/<[a-z][\s\S]*>/i.test(html)) return '<p>' + html.replace(/\r?\n/g, '<br/>') + '</p>'
  html = html.replace(/<script[\s\S]*?<\/script>/gi, '').replace(/<style[\s\S]*?<\/style>/gi, '')
  // 公众号编辑器常把正文包在多层 section 中，而小程序 rich-text 对 section
  // 的兼容性不稳定。转换为 rich-text 支持更好的 div，保留原有行内样式。
  html = html.replace(/<\/?section\b/gi, match => match.toLowerCase().startsWith('</') ? '</div' : '<div')
  html = html.replace(/\son\w+\s*=\s*(["']).*?\1/gi, '')
  html = html.replace(/\sstyle\s*=\s*(["'])([\s\S]*?)\1/gi, (match, quote, style) => {
    const safeStyle = style
      .replace(/display\s*:\s*none\s*;?/gi, '')
      // 公众号卡片常用 flex 布局，rich-text 对 flex:1 及其父子尺寸计算兼容性差，
      // 可能把正文压缩成装饰元素的 25px 宽度，造成文字逐字竖排。
      .replace(/display\s*:\s*flex\s*;?/gi, 'display:block;')
      .replace(/(?:flex|flex-grow|flex-shrink|flex-basis)\s*:[^;]+;?/gi, '')
      .replace(/(?:justify-content|align-items|align-self)\s*:[^;]+;?/gi, '')
      .replace(/visibility\s*:\s*hidden\s*;?/gi, '')
      .replace(/(?:width|max-width|min-width)\s*:\s*\d+(?:\.\d+)?px\s*;?/gi, 'max-width:100%;')
      .replace(/(?:height|max-height|min-height)\s*:\s*\d+(?:\.\d+)?px\s*;?/gi, 'height:auto;')
      .replace(/overflow(?:-x|-y)?\s*:\s*(?:hidden|clip)\s*;?/gi, 'overflow:visible;')
      .replace(/url\s*\([^)]*javascript:[^)]*\)/gi, '')
      .replace(/expression\s*\([^)]*\)/gi, '')
    return ' style="max-width:100%;box-sizing:border-box;' + safeStyle + '"'
  })
  html = html.replace(/<(iframe|object|embed|form|input|button|textarea|select)[^>]*>[\s\S]*?<\/\1>/gi, '')
  html = html.replace(/<(iframe|object|embed|form|input|button|textarea|select)[^>]*\/?\s*>/gi, '')
  html = html.replace(/<img\b([^>]*)>/gi, (match, attrs) => {
    const cleanAttrs = attrs
      .replace(/\s(?:width|height)\s*=\s*(["']).*?\1/gi, '')
      .replace(/\sstyle\s*=\s*(["']).*?\1/gi, '')
    return '<img' + cleanAttrs + ' style="display:block;width:100%;max-width:100%;height:auto;max-height:720px;object-fit:contain;margin:24px auto;" />'
  })
  html = html.replace(/<p[^>]*>\s*(?:<br\s*\/?>)?\s*<\/p>/gi, '')
  return html
}

Page({
  data: { article: null, loading: true, error: '', contentNodes: '', navBarHeight: 88, menuTop: 26, menuHeight: 32 },

  onLoad(options) {
    this.id = options.id || ''
    this.source = options.source || ''
    try {
      const menu = wx.getMenuButtonBoundingClientRect()
      this.setData({ navBarHeight: menu.bottom + 8, menuTop: menu.top, menuHeight: menu.height })
    } catch (error) {}
    this.load()
  },

  async load() {
    if (!this.id) { this.setData({ loading: false, error: '文章参数不完整' }); return }
    try {
      const endpoint = this.source === 'parent-qa' ? '/api/parent-qa/articles/' : (this.source === 'lottery-guide' ? '/api/lottery-guide/articles/' : '/api/admission-content/articles/')
      const article = await request(endpoint + encodeURIComponent(this.id))
      const imageUrls = Array.isArray(article.imageUrls) ? article.imageUrls : []
      // 不同来源的文章正文可能使用 contentText/content/contentHtml；正文为空时至少展示摘要，避免页面看起来像加载失败。
      const content = article.contentText || article.content || article.contentHtml || article.summary || ''
      const publishedAt = String(article.publishedAt || article.createdTime || '').replace('T', ' ').slice(0, 10)
      this.setData({ article: { ...article, contentText: content, imageUrls, publishedAt }, contentNodes: normalizeRichText(content), loading: false, error: '' })
    } catch (error) { this.setData({ loading: false, error: '文章加载失败，请稍后重试' }) }
  },

  goBack() {
    const pages = getCurrentPages()
    if (pages.length > 1) wx.navigateBack({ delta: 1 })
    else wx.switchTab({ url: '/pages/home/home' })
  },

  preview(e) {
    const url = e.currentTarget.dataset.url
    const urls = (this.data.article.imageUrls || []).filter((item, index) => !this.data.failedImages || !this.data.failedImages[index])
    if (urls.length) wx.previewImage({ current: url, urls })
  },

  imageError(e) {
    const index = Number(e.currentTarget.dataset.index)
    this.setData({ ['failedImages.' + index]: true })
  }
})
