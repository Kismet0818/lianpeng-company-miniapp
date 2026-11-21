import csv, json, re
from difflib import SequenceMatcher
from pathlib import Path

BASE=Path(r"C:\Users\Admin\Desktop\lpsx\.codex_work")
schools=json.loads((BASE/"schools_candidates.json").read_text(encoding="utf-8"))
with open(r"C:\Users\Admin\Desktop\lpsx\research_feature_classes.tsv",encoding="utf-8",newline="") as f:
    rows=list(csv.DictReader(f,delimiter="\t"))

def norm(s):
    s=(s or "").lower().replace("华东师范大学第二附属中学","华二").replace("上海师范大学","上师大").replace("上海外国语大学","上外")
    for x in ["上海市","上海","民办","公办","附属","初级中学","初中部","完全中学","中学","学校","实验","教育集团","区","新区"]:
        s=s.replace(x,"")
    return re.sub(r"[^0-9a-z一-鿿]","",s)

def norm2(s):
    s=(s or "").lower().replace("华东师范大学第二附属中学","华二").replace("上海师范大学","上师大").replace("上海外国语大学","上外")
    for x in ["上海市","上海","民办","公办"]:
        s=s.replace(x,"")
    return re.sub(r"[^0-9a-z一-鿿]","",s)

cands=[]
for s in schools:
    names=[s.get("学校全称","")]
    names += re.split(r"[;；,/]+",s.get("学校简称","") or "")
    cands.append((s,names,[norm(x) for x in names],[norm2(x) for x in names]))

manual={
    "闵华二":"华东师范大学第二附属中学闵行紫竹分校",
    "复旦复兴":"复旦大学附属复兴中学初中部",
    "上外松外":"上海外国语大学松江外国语学校",
    "交中初级":"上海民办浦东交中初级中学",
}

matched=[]
for r in rows:
    a=r["alias"]
    best=None
    if a in manual:
        for s,n,nn,nn2 in cands:
            if s.get("学校全称")==manual[a]: best=(1,s,n); break
    if not best:
        na=norm(a); na2=norm2(a)
        for s,names,nn,nn2 in cands:
            score=max([SequenceMatcher(None,na,x).ratio() for x in nn if x]+[SequenceMatcher(None,na2,x).ratio() for x in nn2 if x] or [0])
            if any(na2 and (na2 in x or x in na2) and min(len(na2),len(x))>=3 for x in nn2): score=max(score,.97)
            if any(na and (na in x or x in na) and min(len(na),len(x))>=2 for x in nn): score=max(score,.93)
            if best is None or score>best[0]: best=(score,s,names)
    score,s,names=best
    matched.append({**r,"match_score":round(score,3),"school_id":s.get("school_id"),"school_name":s.get("学校全称"),"school_short":s.get("学校简称")})

(BASE/"matched_research.json").write_text(json.dumps(matched,ensure_ascii=False,indent=2),encoding="utf-8")
aliases={}
for m in matched:
    aliases.setdefault(m['alias'],m)
for a,m in aliases.items():
    print(f"{m['match_score']:.3f}\t{a}\t=>\t{m['school_id']}\t{m['school_name']}")
print(json.dumps({"research_rows":len(rows),"aliases":len(aliases),"matched_ge_0.8":sum(m['match_score']>=.8 for m in aliases.values())},ensure_ascii=False))

report=[]
for a in aliases:
    na=norm(a); na2=norm2(a); scored=[]
    for s,names,nn,nn2 in cands:
        score=max([SequenceMatcher(None,na,x).ratio() for x in nn if x]+[SequenceMatcher(None,na2,x).ratio() for x in nn2 if x] or [0])
        if any(na2 and (na2 in x or x in na2) and min(len(na2),len(x))>=3 for x in nn2): score=max(score,.97)
        scored.append((score,s['school_id'],s['学校全称'],s.get('学校简称') or ''))
    scored.sort(reverse=True)
    report.append(a+'\n'+'\n'.join(f"  {sc:.3f}\t{sid}\t{name}\t{short}" for sc,sid,name,short in scored[:5]))
(BASE/'match_report.txt').write_text('\n'.join(report),encoding='utf-8')
