import pymysql
J={'市十中学':170,'储能中学':167,'金陵中学':174,'市南中学':184,'音乐学院附属黄浦比乐中学':188,'格致初级中学':171,'大同初级中学':168,'向明初级中学':185,'大境初级中学':187,'光明初级中学':172,'敬业初级中学':175,'卢湾中学':177,'市八初级中学':183,'尚文中学':182,'上海交通大学附属黄浦实验中学':166,'清华中学':181,'黄教院附属中山学校':173}
P={'蓬莱路第二小学':100026,'徽宁路第三小学':100032,'上外-黄浦外国语小学':100028,'复兴东路第三小学':100031,'海华小学':100039,'黄浦区第一中心小学':100030,'卢湾一中心小学':100034,'巨鹿路第一小学':100037}
pairs=[('市十中学','蓬莱路第二小学'),('储能中学','徐锦秋实验学校、重庆北路小学'),('储能中学','民办永昌学校（小学部）'),('储能中学','康德双语实验学校（小学部）'),('金陵中学','报童小学'),('市南中学','徽宁路第三小学'),('音乐学院附属黄浦比乐中学','上海师范大学附属卢湾实验小学'),('格致初级中学','曹光彪小学、北京东路小学'),('大同初级中学','上外-黄浦外国语小学'),('大同初级中学','上海师范大学附属黄浦梅溪小学'),('向明初级中学','七色花小学、卢湾二中心小学'),('大境初级中学','新凌小学、上海师范专科学校附属小学'),('光明初级中学','复兴东路第三小学'),('敬业初级中学','实验小学、回民小学'),('卢湾中学','海华小学'),('市八初级中学','中华路第三小学'),('尚文中学','黄浦区第一中心小学、董家渡路第二小学'),('上海交通大学附属黄浦实验中学','卢湾一中心小学'),('上海交通大学附属黄浦实验中学','上海交通大学附属黄浦实验小学'),('清华中学','巨鹿路第一小学'),('黄教院附属中山学校','黄教院附属中山学校（小学部）、瞿溪路小学')]
conn=pymysql.connect(host='sh-cynosdbmysql-grp-7le4bnf4.sql.tencentcdb.com',port=29693,user='lianpeng',password='Datou870263',database='cloud1-d6g28bq6c04919547',charset='utf8mb4',autocommit=False)
try:
 with conn.cursor() as c:
  c.execute("SELECT id,name FROM primary_school WHERE district='黄浦区'"); dbp=dict(c.fetchall())
  def norm(s): return s.replace('上海市','').replace('黄浦区','').replace('（小学部）','').replace('(','').replace(')','').replace('（','').replace('）','').replace(' ','')
  pmap={norm(n):(i,n) for i,n in dbp.items()}; rows=[]; seen=set()
  for j,p in pairs:
   hit=pmap.get(norm(p)); pid=hit[0] if hit else P.get(p); pname=hit[1] if hit else p
   key=(J[j],pid,pname)
   if key not in seen: seen.add(key); rows.append((J[j],pid,pname))
  c.execute("DELETE FROM junior_high_primary_duikou WHERE district='黄浦区' AND admission_year=2026")
  note='来源文件：2026年黄浦区招生合集.xls；工作表：初中预备班对口入学方案'
  for order,(jid,pid,pname) in enumerate(rows,1): c.execute("INSERT INTO junior_high_primary_duikou (duikou_material_id,admission_year,district,junior_high_school_id,primary_school_id,primary_school_name,relation_type,admission_method,scope_text,note,sort_order,status) VALUES (6,2026,'黄浦区',%s,%s,%s,'direct','对口入学','初中预备班对口入学方案',%s,%s,1)",(jid,pid,pname,note,order))
 conn.commit(); print('rows',len(rows))
except: conn.rollback(); raise
finally: conn.close()
