/* 联朋升学 管理端静态服务器
 * 用法: node server.js
 * 局域网设备通过 http://<本机IP>:3000 访问管理页
 */
const http = require('http')
const fs = require('fs')
const path = require('path')

const PORT = 3000
const ROOT = __dirname

const MIME = {
  '.html': 'text/html; charset=utf-8',
  '.js': 'text/javascript; charset=utf-8',
  '.css': 'text/css; charset=utf-8',
  '.json': 'application/json; charset=utf-8',
  '.png': 'image/png',
  '.jpg': 'image/jpeg',
  '.svg': 'image/svg+xml',
  '.ico': 'image/x-icon'
}

http.createServer((req, res) => {
  const urlPath = decodeURIComponent(req.url.split('?')[0])
  let filePath = path.join(ROOT, urlPath === '/' ? 'admin.html' : urlPath)

  // 防止目录穿越
  if (!filePath.startsWith(ROOT)) {
    res.writeHead(403)
    res.end('Forbidden')
    return
  }

  fs.readFile(filePath, (err, data) => {
    if (err) {
      res.writeHead(404)
      res.end('Not Found')
      return
    }
    res.writeHead(200, {
      'Content-Type': MIME[path.extname(filePath)] || 'application/octet-stream'
    })
    res.end(data)
  })
}).listen(PORT, '0.0.0.0', () => {
  console.log('==================================')
  console.log(' 联朋升学 · 留资管理端 已启动')
  console.log(' 本机访问:   http://localhost:' + PORT)
  console.log(' 局域网访问: http://<你的IP>:' + PORT)
  console.log('==================================')
})
