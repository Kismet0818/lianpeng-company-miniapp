Page({
  data: {
    stages: [
      { grade: '小班', age: '3-4岁', goal: '兴趣启蒙', desc: '建立英语好感度，通过儿歌、绘本与亲子共读积累听力输入。', tags: ['听力输入', '亲子共读'] },
      { grade: '中班', age: '4-5岁', goal: '阅读起步', desc: '发展音素意识与基础词汇，逐步理解图文对应和简单故事。', tags: ['词汇积累', '图文理解'] },
      { grade: '大班', age: '5-6岁', goal: '自主衔接', desc: '结合自然拼读与分级阅读，尝试独立拼读并形成阅读习惯。', tags: ['自然拼读', '自主阅读'] }
    ],
    resources: [
      {
        name: 'RAZ 分级阅读',
        en: 'READING A-Z',
        desc: '体系覆盖面广、级别划分清晰，可根据孩子当前阅读能力循序进阶，兼顾词汇、理解与知识拓展。',
        points: ['科学分级', '主题丰富', '逐级进阶'],
        images: [
          'https://lianpeng.oss-cn-shanghai.aliyuncs.com/liuxuefudao/raz1.jpg',
          'https://lianpeng.oss-cn-shanghai.aliyuncs.com/liuxuefudao/raz2.jpg'
        ]
      },
      {
        name: '红火箭分级阅读',
        en: 'RED ROCKET READERS',
        desc: '内容短小、画面直观，适合启蒙阶段建立开口信心，也可配合自然拼读进行阅读练习。',
        points: ['启蒙友好', '图文对应', '开口练习'],
        images: ['https://lianpeng.oss-cn-shanghai.aliyuncs.com/liuxuefudao/redrocket.jpg']
      },
      {
        name: '牛津阅读树',
        en: 'OXFORD READING TREE',
        desc: '以连续人物和生活故事激发阅读兴趣，帮助孩子在熟悉语境中理解表达与积累常用词汇。',
        points: ['故事连贯', '生活语境', '兴趣培养'],
        images: ['https://lianpeng.oss-cn-shanghai.aliyuncs.com/liuxuefudao/niujinshu.jpg']
      },
      {
        name: '自然拼读',
        en: 'SYSTEMATIC PHONICS',
        desc: '建立字母与发音之间的联系，帮助孩子从“听懂、认识”逐步过渡到“会拼、能读”。',
        points: ['音素意识', '拼读规则', '阅读衔接'],
        images: ['https://lianpeng.oss-cn-shanghai.aliyuncs.com/liuxuefudao/ziranpindu.jpg']
      }
    ]
  },

  onConsult() {
    wx.navigateTo({ url: '/pages/lead/lead?source=preschool-language' })
  },

  onShareAppMessage() {
    return {
      title: '3-6岁学龄前语言成长规划',
      path: '/pages/preschool-language/preschool-language'
    }
  }
})
