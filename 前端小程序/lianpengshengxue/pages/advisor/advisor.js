const { request } = require('../../utils/request')
Page({
  data:{messages:[],choices:[],context:{},schools:[],input:'',loading:false,error:'',canContact:false,navTop:20,navHeight:64},
  onLoad(){const info=wx.getWindowInfo?wx.getWindowInfo():wx.getSystemInfoSync();const menu=wx.getMenuButtonBoundingClientRect?wx.getMenuButtonBoundingClientRect():null;const navTop=info.statusBarHeight||20;const navHeight=menu?menu.bottom+5:navTop+44;this.setData({navTop,navHeight});this.start()},
  async start(){this.setData({loading:true,error:''});try{this.apply(await request('/api/advisor/welcome'),true)}catch(e){this.setData({error:'暂时无法连接顾问，请稍后重试'})}finally{this.setData({loading:false})}},
  apply(data,reset){const reply=String(data&&data.reply||'');this.setData({messages:reset?(reply?[{role:'assistant',text:reply}]:[]):[...this.data.messages,...(reply?[{role:'assistant',text:reply}]:[])],choices:Array.isArray(data&&data.choices)?data.choices:[],context:data&&data.context||{},schools:Array.isArray(data&&data.schools)?data.schools:[],canContact:Boolean(data&&(data.canContact||data.step==='lead'))})},
  onChoice(e){this.send(String(e.currentTarget.dataset.value||e.currentTarget.dataset.label||''))},
  onInput(e){this.setData({input:e.detail.value})},
  onConfirm(){const v=String(this.data.input||'').trim();if(v){this.setData({input:''});this.send(v)}},
  async send(message){if(this.data.loading||!message)return;this.setData({loading:true,error:'',choices:[],schools:[],messages:[...this.data.messages,{role:'user',text:message}]});try{this.apply(await request('/api/advisor/reply','POST',{message:message.slice(0,200),context:this.data.context}),false)}catch(e){this.setData({error:'发送失败，请重试'})}finally{this.setData({loading:false})}},
  retry(){this.start()},
  goBack(){wx.navigateBack({delta:1})},
  contactTeacher(){wx.navigateTo({url:'/pages/lead/lead?target='+encodeURIComponent('智能升学顾问')})},
  onSchoolTap(e){const s=e.currentTarget.dataset.item;if(!s||!s.schoolId||!s.schoolSource)return;const p=s.schoolSource==='international'?'/pages/school-detail/school-detail':'/pages/score-school-detail/score-school-detail';wx.navigateTo({url:p+'?id='+Number(s.schoolId)+'&schoolSource='+encodeURIComponent(s.schoolSource)})}
})
