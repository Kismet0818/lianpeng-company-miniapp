const { request } = require('../../utils/request')
Component({
  data: { pageReady:false, blessings:[], blessingScrollTop:0, admissionArticles:[], volunteerTips:[], allVolunteerTips:[], showAllVolunteerTips:false, articleYear:'2026', articleYears:['2026'], keyword:'', districts:[['黄浦区',17],['徐汇区',19],['长宁区',9],['静安区',21],['普陀区',13],['虹口区',12],['杨浦区',16],['闵行区',29],['宝山区',19],['嘉定区',12],['浦东新区',60],['金山区',10],['松江区',13],['青浦区',8],['奉贤区',9],['崇明区',8]].map(x=>({name:x[0],count:x[1]})), filteredDistricts:[] },
  lifetimes:{
    attached(){ this.setData({filteredDistricts:this.data.districts,pageReady:true}); this.loadBlessings(); this.loadAdmissionContent() },
    detached(){ this.stopBlessingAutoScroll() }
  },
  methods:{
    onSearchInput(e){const keyword=String(e.detail.value||'').trim();this.setData({keyword,filteredDistricts:keyword?this.data.districts.filter(x=>x.name.includes(keyword)):this.data.districts})},
    onDistrictTap(e){wx.navigateTo({url:'/pages/district-admission/district-admission?district='+encodeURIComponent(e.currentTarget.dataset.name)})},
    async loadBlessings(){try{const d=await request('/api/blessing/list?page=1&size=6');this.setData({blessings:(d.records||[]).map(x=>{const n=x.nickname||'匿名用户';return{nickname:n,initial:n.slice(0,1),target:String(x.content||'').split(/\s+/)[0]||'祝同学',schoolName:x.schoolName||''}}),blessingScrollTop:0},()=>this.startBlessingAutoScroll())}catch(e){}},
    startBlessingAutoScroll(){
      this.stopBlessingAutoScroll()
      if(this.data.blessings.length<=4)return
      const windowInfo=wx.getWindowInfo?wx.getWindowInfo():wx.getSystemInfoSync()
      const rowHeight=72*windowInfo.windowWidth/750
      const maxIndex=this.data.blessings.length-4
      this._blessingScrollIndex=0
      this._blessingScrollTimer=setInterval(()=>{
        this._blessingScrollIndex=this._blessingScrollIndex>=maxIndex?0:this._blessingScrollIndex+1
        this.setData({blessingScrollTop:this._blessingScrollIndex*rowHeight})
      },2600)
    },
    stopBlessingAutoScroll(){
      if(this._blessingScrollTimer){clearInterval(this._blessingScrollTimer);this._blessingScrollTimer=null}
      if(this._blessingResumeTimer){clearTimeout(this._blessingResumeTimer);this._blessingResumeTimer=null}
    },
    pauseBlessingAutoScroll(){this.stopBlessingAutoScroll()},
    resumeBlessingAutoScroll(){
      this.stopBlessingAutoScroll()
      this._blessingResumeTimer=setTimeout(()=>this.startBlessingAutoScroll(),1800)
    },
    async loadAdmissionContent(){try{const d=await request('/api/admission-content/articles','GET',{category:'详细文章',year:this.data.articleYear});this.setData({admissionArticles:(Array.isArray(d)?d:d.records||[]).slice(0,3)})}catch(e){} try{const d=await request('/api/admission-content/articles','GET',{category:'志愿填报小知识'});const a=Array.isArray(d)?d:d.records||[];this.setData({allVolunteerTips:a,volunteerTips:a.slice(0,3)})}catch(e){}},
    selectArticleYear(e){this.setData({articleYear:String(e.currentTarget.dataset.year)},()=>this.loadAdmissionContent())}, openAdmissionArticle(e){wx.navigateTo({url:'/pages/admission-content-detail/admission-content-detail?id='+encodeURIComponent(e.currentTarget.dataset.id)})}, openAdmissionList(){const a=this.data.allVolunteerTips;const v=!this.data.showAllVolunteerTips;this.setData({showAllVolunteerTips:v,volunteerTips:v?a:a.slice(0,3)})}, onConsult(){wx.navigateTo({url:'/pages/lead/lead?target='+encodeURIComponent('上海中考升学咨询')+'&source=score-tool'})}
  }
})
