const { request } = require('../../utils/request')
const ACADEMIC_YEAR = '2025-2026'
const DISTRICTS = [
  {label:'委属',value:'委属'},{label:'黄浦',value:'黄浦区'},{label:'徐汇',value:'徐汇区'},{label:'长宁',value:'长宁区'},
  {label:'静安',value:'静安区'},{label:'普陀',value:'普陀区'},{label:'虹口',value:'虹口区'},{label:'杨浦',value:'杨浦区'},
  {label:'闵行',value:'闵行区'},{label:'宝山',value:'宝山区'},{label:'嘉定',value:'嘉定区'},{label:'浦东',value:'浦东新区'},
  {label:'金山',value:'金山区'},{label:'松江',value:'松江区'},{label:'青浦',value:'青浦区'},{label:'奉贤',value:'奉贤区'},{label:'崇明',value:'崇明区'}
]
Page({
  data:{districts:DISTRICTS,selectedDistrict:'黄浦区',schools:[],loading:false,error:''},
  onLoad(options){
    let district='黄浦区'
    try{district=decodeURIComponent(options.district||district)}catch(e){}
    if(!DISTRICTS.some(item=>item.value===district))district='黄浦区'
    this.setData({selectedDistrict:district})
    this.loadData()
  },
  onDistrictTap(e){
    const district=e.currentTarget.dataset.district
    if(!district||district===this.data.selectedDistrict)return
    this.setData({selectedDistrict:district,schools:[]})
    this.loadData()
  },
  async loadData(){
    this.setData({loading:true,error:''})
    try{
      const data=await request('/api/score-query/volunteer?district='+encodeURIComponent(this.data.selectedDistrict)+'&year='+encodeURIComponent(ACADEMIC_YEAR)+'&moduleType=2')
      const schools=(Array.isArray(data)?data:[]).filter(item=>Number(item.moduleType)===2&&(!item.targetDistrict||item.targetDistrict===this.data.selectedDistrict)).map(item=>({
        id:item.id,schoolName:item.schoolName||item.highSchoolName||'学校名称待补充',
        schoolTypeLabel:item.schoolType==='市特色普通高中'?'市特色':'市重点',nature:item.nature||'公办',boarding:item.boarding||item.boardingType||'',
        subjectExcellent:this.formatCount(item.subjectExcellent),sportsExcellent:this.formatCount(item.sportsExcellent),artBackbone:this.formatCount(item.artBackbone)
      }))
      this.setData({schools})
    }catch(e){console.error('自主招生信息加载失败：',e);this.setData({error:'自主招生信息加载失败'})}
    finally{this.setData({loading:false})}
  },
  formatCount(value){
    if(value===null||value===undefined||value==='')return '—'
    const number=Number(value)
    return Number.isFinite(number)?String(Number(number.toFixed(1))):'—'
  }
})
