const app = getApp()
Component({
  data: {
    selected: -1,
    tabs: [
      {
        pagePath: '/pages/home/home',
        text: '首页',
        iconPath: '/assets/icons/tab-bar/home.svg',
        selectedIconPath: '/assets/icons/tab-bar/home-active.svg',
      },
      {
        pagePath: '/pages/school/school',
        text: '学校库',
        iconPath: '/assets/icons/tab-bar/school.svg',
        selectedIconPath: '/assets/icons/tab-bar/school-active.svg',
      },
      {
        pagePath: '/pages/study-abroad/study-abroad',
        text: '升学工具',
        iconPath: '/assets/icons/tab-bar/study.svg',
        selectedIconPath: '/assets/icons/tab-bar/study-active.svg',
      },
      {
        pagePath: '/pages/user/user',
        text: '个人中心',
        iconPath: '/assets/icons/tab-bar/user.svg',
        selectedIconPath: '/assets/icons/tab-bar/user-active.svg',
      }
    ]
  },

  lifetimes: {
    ready() {
      this.setData({ selected: app.globalData.selectedTab })
    }
  },

  methods: {
    switchTab(e) {
      const selected = Number(e.currentTarget.dataset.index)
      const selectedTab = this.data.tabs[selected]
      if (!Number.isInteger(selected) || !selectedTab) return
      this.navigateToTab(selectedTab, selected)
    },

    navigateToTab(selectedTab, selected) {
      app.globalData.selectedTab = selected
      this.setData({ selected })
      wx.switchTab({
        url: selectedTab.pagePath,
        fail: (error) => {
          console.error('底部导航跳转失败：', error)
        }
      })
    }
  }
})
