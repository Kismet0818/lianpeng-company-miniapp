import json, sys
from collections import Counter
from urllib.parse import urlsplit, parse_qsl

path = sys.argv[1]
with open(path, 'r', encoding='utf-8-sig') as f:
    har = json.load(f)
entries = har.get('log', {}).get('entries', [])
print('entries=', len(entries))
counts = Counter()
for i, e in enumerate(entries):
    req, resp = e.get('request', {}), e.get('response', {})
    u = urlsplit(req.get('url', ''))
    key = (req.get('method'), u.netloc, u.path, resp.get('status'))
    counts[key] += 1
for (method, host, route, status), n in counts.most_common():
    print(f'{n:3} {method:5} {status} {host}{route}')

print('\nJSON_API_DETAILS')
for i, e in enumerate(entries):
    req, resp = e.get('request', {}), e.get('response', {})
    content = resp.get('content', {})
    text = content.get('text') or ''
    mime = content.get('mimeType', '')
    if not text or ('json' not in mime.lower() and not text.lstrip().startswith(('{','['))):
        continue
    u = urlsplit(req.get('url', ''))
    try:
        data = json.loads(text)
    except Exception:
        continue
    if isinstance(data, dict):
        shape = list(data.keys())[:20]
        sizes = {k: len(v) for k, v in data.items() if isinstance(v, (list, dict))}
    else:
        shape = [type(data).__name__]
        sizes = {'root': len(data)} if isinstance(data, list) else {}
    query = [(k, v) for k, v in parse_qsl(u.query) if k.lower() not in {'token','access_token','authorization','key','secret','openid'}]
    post = req.get('postData', {})
    body = post.get('text', '')
    if len(body) > 500: body = body[:500] + '...'
    print(f'#{i} {req.get("method")} {resp.get("status")} {u.scheme}://{u.netloc}{u.path} query={query} body={body!r} keys={shape} sizes={sizes}')
