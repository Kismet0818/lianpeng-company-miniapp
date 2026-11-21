import json, sys
from urllib.parse import urlsplit

with open(sys.argv[1], encoding='utf-8-sig') as f:
    entries = json.load(f)['log']['entries']

def describe(value, depth=0):
    if depth > 3: return type(value).__name__
    if isinstance(value, dict): return {k: describe(v, depth+1) for k,v in value.items()}
    if isinstance(value, list): return {'count':len(value), 'first':describe(value[0], depth+1) if value else None}
    if isinstance(value, str): return f'str:{value[:100]}'
    return value

seen=set()
for e in entries:
    path=urlsplit(e['request']['url']).path
    if path not in ('/ApiIntlV1/schoolList','/ApiIntlV1/schoolDetail') or path in seen: continue
    seen.add(path)
    data=json.loads(e['response']['content']['text'])
    print('\nPATH',path)
    print(json.dumps(describe(data),ensure_ascii=False,indent=2))
    if path.endswith('schoolList'):
        print('\nLIST_DATA_SAMPLE')
        print(json.dumps(data.get('data'),ensure_ascii=False,indent=2)[:12000])
    else:
        print('\nDETAIL_DATA')
        print(json.dumps(data.get('data'),ensure_ascii=False,indent=2)[:20000])
