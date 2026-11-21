Page({
  data: {
    subjects: [
      {
        name: '语文', en: 'CHINESE', theme: 'blue',
        desc: '高中语文从“记忆积累”转向阅读理解、逻辑表达与写作能力的综合考查。',
        points: ['现代文阅读：信息筛选与结构分析', '文言文：实词、句式与翻译', '作文：审题立意与素材体系'],
        scoreImage: 'https://lianpeng.oss-cn-shanghai.aliyuncs.com/liuxuefudao/yuwen.png?v=20260813',
        scoreImageAlt: '语文高考题型分数占比图'
      },
      {
        name: '数学', en: 'MATHEMATICS', theme: 'cyan',
        desc: '新高一数学节奏更快、抽象程度更高，函数是后续多个模块的重要基础。',
        points: ['集合与逻辑：规范数学表达', '函数：性质、图像与综合应用', '几何与概率：建立模型意识'],
        scoreImage: 'https://lianpeng.oss-cn-shanghai.aliyuncs.com/liuxuefudao/shuxue.png?v=20260813',
        scoreImageAlt: '数学高考题型分数占比图'
      },
      {
        name: '英语', en: 'ENGLISH', theme: 'teal',
        desc: '高中英语更重视长篇阅读、语篇理解和准确表达，需要持续积累而非短期突击。',
        points: ['词汇：从识记走向语境运用', '阅读：主旨、细节与推断', '写作：结构、句式与逻辑衔接'],
        scoreImage: 'https://lianpeng.oss-cn-shanghai.aliyuncs.com/liuxuefudao/yingyu.png?v=20260813',
        scoreImageAlt: '英语高考题型分数占比图'
      }
    ],
    timeline: [
      { term: '高一上', title: '完成初高衔接', desc: '适应课堂容量与考试节奏，建立各科学习方法和错题复盘习惯。' },
      { term: '高一下', title: '关注选科方向', desc: '结合成绩、兴趣与未来专业方向，观察学科优势，避免只看单次分数。' },
      { term: '高二', title: '形成优势学科', desc: '夯实主干知识，完成专题训练，让优势科目稳定、弱势科目可控。' },
      { term: '高三', title: '进入系统复习', desc: '按一轮基础、二轮专题、三轮模拟推进，管理目标与考试状态。' }
    ]
  },

  onConsult() {
    wx.navigateTo({ url: '/pages/lead/lead?target=' + encodeURIComponent('新高一课程规划') + '&source=grade-ten-planning' })
  },

  onShareAppMessage() {
    return { title: '新高一课程规划｜语数英考点与三年节奏', path: '/pages/grade-ten-planning/grade-ten-planning' }
  }
})
