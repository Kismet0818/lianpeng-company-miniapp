import pymysql

DB = dict(host="sh-cynosdbmysql-grp-7le4bnf4.sql.tencentcdb.com", port=29693,
          user="lianpeng", password="Datou870263", database="cloud1-d6g28bq6c04919547",
          charset="utf8mb4", autocommit=False)

def n(s):
    return ''.join(str(s or '').split()).replace('（','(').replace('）',')')

rows = [
 (1,'上海市位育初级中学','lottery',['上海市徐汇区向阳小学','上海市徐汇区建襄小学','上海市徐汇区第一中心小学']),
 (2,'上海市第二初级中学','lottery',['上海市徐汇区向阳小学','上海市徐汇区建襄小学','上海市徐汇区第一中心小学']),
 (3,'上海市南洋模范初级中学','lottery',['上海市徐汇区高安路第一小学（宛平校区）','上海市徐汇区世界小学','上海市徐汇区虹桥路小学','上海交通大学附属小学（部分）']),
 (4,'上海市徐汇中学（蒲汇塘校区）','lottery',['上海市徐汇区高安路第一小学（宛平校区）','上海市徐汇区世界小学','上海市徐汇区虹桥路小学','上海市徐汇区汇师小学（徐虹校区）','上海交通大学附属小学（部分）','上海市徐汇区光启小学']),
 (5,'上海市第五十四中学','lottery',['上海市徐汇区高安路第一小学（宛平校区）','上海市徐汇区世界小学']),
 (6,'上海市田林中学','lottery',['上海市徐汇区田林第三小学','上海市徐汇区田林第四小学','上海市漕河泾新兴技术开发区实验小学','上海交通大学附属小学（部分）']),
 (7,'上海市田林第二中学','lottery',['上海市徐汇区田林第三小学','上海市徐汇区田林第四小学','上海市漕河泾新兴技术开发区实验小学','上海交通大学附属小学（部分）']),
 (8,'上海市紫阳中学','lottery',['上海市徐汇区高安路第一小学（华展校区）','上海市徐汇区徐浦小学（部分）','上海市徐汇区位育小学（部分）']),
 (9,'上海市位育附属徐汇科技实验中学','lottery',['上海市徐汇区高安路第一小学（华展校区）','上海市徐汇区徐浦小学（部分）','上海市徐汇区位育小学（部分）']),
 (10,'上海市田林第三中学','mixed',['上海市徐汇区田林第三小学','上海市徐汇区田林第四小学','上海市漕河泾新兴技术开发区实验小学','上海交通大学附属小学（部分）','上海市徐汇区求知小学（部分）']),
 (11,'上海市第四中学','mixed',['上海市徐汇区高安路第一小学（宛平校区）','上海市徐汇区世界小学','上海市徐汇区汇师小学（徐虹校区）','上海市徐汇区光启小学','上海市徐汇区龙华小学','上海市徐汇区东安三村小学（部分）','上海市徐汇区东二小学（部分）']),
 (12,'上海市零陵中学','mixed',['上海市徐汇区龙华小学','上海市徐汇区东安三村小学（部分）','上海市徐汇区东二小学（部分）','上海市教育科学研究院实验小学（部分）','上海市徐汇区日晖新村小学（部分）']),
 (13,'上海市南洋初级中学','direct',['上海市教育科学研究院实验小学（部分）','上海宋庆龄学校附属徐汇实验小学','上海市徐汇区日晖新村小学（部分）','上海市徐汇区东二小学（部分）']),
 (14,'上海市中国中学','direct',['上海市徐汇区康宁科技实验小学','上海市徐汇区求知小学（部分）','上海师范大学第一附属小学（部分）']),
 (15,'上海市康健外国语实验中学','direct',['上海市徐汇区樱花园小学','上海市徐汇区康健外国语实验小学','上海师范大学第一附属小学（部分）']),
 (16,'上海市龙苑中学','direct',['上海市徐汇区龙南小学']),
 (17,'上海市徐汇区教育学院附属实验中学','direct',['上海市徐汇区上海小学（本部校区）','上海市徐汇区向阳育才小学（部分）']),
 (18,'上海市园南中学（总校）','direct',['上海市徐汇区园南小学','上海市徐汇区徐汇实验小学（部分）']),
 (19,'上海市园南中学（罗城校区）','direct',['上海市徐汇区汇师小学（中城校区）']),
 (20,'上海市汾阳中学','direct',['上海市徐汇区向阳育才小学（部分）']),
 (21,'上海市徐汇区教育学院附属实验中学南部分校','direct',['上海市徐汇区上海小学（龙川校区）','上海市徐汇区教育学院附属实验小学','上海市徐汇区徐汇实验小学（部分）']),
 (22,'华东理工大学附属中学','direct',['上海市实验学校附属小学','上海市徐汇区启新小学（部分）','华东理工大学附属小学（部分）']),
 (23,'上海市梅园中学','direct',['上海市徐汇区启新小学（部分）','华东理工大学附属小学（部分）']),
 (24,'上海市徐汇中学（南校区）','direct',['上海市徐汇区徐汇实验小学（部分）','上海市徐汇区位育小学（部分）','上海市徐汇区徐浦小学（部分）']),
 (25,'上海师范大学第三附属实验学校（初中）','direct',['上海师范大学第三附属实验学校（小学）']),
 (26,'复旦大学附属徐汇实验学校（初中）','direct',['复旦大学附属徐汇实验学校（小学）']),
 (27,'上海市西南位育附属实验学校（初中）','direct',['上海市西南位育附属实验学校（小学）']),
 (28,'上海市徐汇区上汇实验学校（初中）','direct',['上海市徐汇区上汇实验学校（小学）']),
]

def main():
    c=pymysql.connect(**DB)
    try:
      with c.cursor() as x:
        x.execute("SELECT id FROM duikou_material WHERE status=1 AND title LIKE %s",('%徐汇%',)); material=x.fetchone()[0]
        x.execute("SELECT id,name,short_name FROM junior_high_school WHERE district=%s",('徐汇区',)); js=x.fetchall()
        x.execute("SELECT id,name,short_name FROM primary_school WHERE district=%s",('徐汇区',)); ps=x.fetchall()
        def find_school(items, wanted):
            w=n(wanted).replace('(部分)','').replace('(宛平校区)','').replace('(华展校区)','').replace('(徐虹校区)','').replace('(中城校区)','').replace('(本部校区)','').replace('(龙川校区)','').replace('(小学)','').replace('(初中)','')
            # 先全称，再简称和去掉校区/部分后的名称。
            for sid,name,short in items:
                candidates=[n(name),n(short)]
                if any(w and (w==z or w in z or z in w) for z in candidates): return sid,name
            return None,None
        jids={}
        for no,jname,typ,primary_names in rows:
            sid,canonical=find_school(js,jname)
            if sid: jids[no]=(sid,canonical)
        x.execute("DELETE FROM junior_high_primary_duikou WHERE district=%s AND admission_year=2026",('徐汇区',))
        sql="""INSERT IGNORE INTO junior_high_primary_duikou
          (duikou_material_id,admission_year,district,junior_high_school_id,primary_school_id,primary_school_name,relation_type,admission_method,scope_text,note,sort_order,status)
          VALUES (%s,2026,'徐汇区',%s,%s,%s,%s,%s,%s,%s,%s,1)"""
        inserted=0; missing_j=[]; missing_p=[]
        for no,jname,typ,pnames in rows:
            if no not in jids: missing_j.append((no,jname)); continue
            jid,_=jids[no]
            for order,pname in enumerate(pnames):
                pid,canonical=find_school(ps,pname)
                if not pid: missing_p.append((no,pname))
                method='电脑派位' if typ=='lottery' else ('电脑派位+对口入学' if typ=='mixed' else ('九年一贯制' if no>=25 else '对口入学'))
                relation='lottery' if typ=='lottery' else 'direct'
                note=f'来源：2026年徐汇区招生合集.xlsx；徐汇区公办初中入学方式；第{no+9}行；整理自用户提供的28项清单'
                x.execute(sql,(material,jid,pid,pname,relation,method,pname,note,no*100+order)); inserted+=x.rowcount
        c.commit()
        x.execute("SELECT COUNT(*),COUNT(DISTINCT junior_high_school_id),COUNT(DISTINCT primary_school_id),SUM(primary_school_id IS NULL) FROM junior_high_primary_duikou WHERE district='徐汇区' AND admission_year=2026")
        print('inserted',inserted,'totals',x.fetchone())
        print('missing_junior',missing_j)
        print('missing_primary',missing_p)
    except Exception: c.rollback(); raise
    finally: c.close()
if __name__=='__main__': main()
