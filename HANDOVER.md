# 联朋升学（lpsx）后端交接文档

> 本文档用于项目交接，包含后端技术栈、运行方式及关键凭据。
> ⚠️ **敏感信息提醒**：本文件含明文密码/密钥，请妥善保管，勿提交到公开仓库；如仓库需共享，请将凭据改为环境变量引用并单独保管。

---

## 1. 项目简介

「联朋升学」微信小程序的后端服务。小程序端基于微信原生小程序，后端提供学校库、招生动态、升学评估、留资、查分工具、图片上传（OSS 直传）等 API。

- 后端工程根目录：`c:\Users\Admin\Desktop\lpsx`
- 小程序端根目录：`C:\Users\Admin\WeChatProjects\lianpengshengxue`（工作区外）

---

## 2. 后端技术栈

| 分类 | 技术 | 版本 |
|---|---|---|
| 语言 / 运行时 | Java（JDK 25） | 25.0.2 |
| 框架 | Spring Boot | 3.4.7 |
| 构建工具 | Maven | - |
| ORM | MyBatis-Plus（spring-boot3 专用 starter） | 3.5.5 |
| 数据库 | MySQL（mysql-connector-j） | 8.x 驱动 |
| 对象存储 | 阿里云 OSS SDK | 3.17.4 |
| 鉴权 | JWT（jjwt） | 0.11.5 |
| 工具库 | Hutool | 5.8.26 |
| 编译优化 | Lombok | - |
| 热部署 | spring-boot-devtools | 随 Boot |
| 测试 | spring-boot-starter-test | - |

**运行环境注意**：
- 本机 JDK 路径：`C:\Users\Admin\AppData\Local\jdks\jdk-25.0.2`
- 编译/运行时需临时设置 `JAVA_HOME` 指向该 JDK 25，否则可能报版本不匹配。
- 本地启动：`mvn spring-boot:run`（监听 `0.0.0.0:8080`）。
- Maven 输出 JVM 警告到 stderr 会让 PowerShell 报 `NativeCommandError`，只要显示 `BUILD SUCCESS` 即视为编译成功。

---

## 3. 本地数据库连接

| 项 | 值 |
|---|---|
| 主机 | `192.168.3.5` |
| 端口 | `3306` |
| 数据库名 | `lpsx` |
| 用户名 | `root` |
| 密码 | `Datou123456!` |

连接串（application.yml 中）：
```
jdbc:mysql://192.168.3.5:3306/lpsx?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true
```

密码支持环境变量覆盖：`DB_PASSWORD`（未设置时默认使用上表密码）。

PowerShell 直连 MySQL 参考：
```powershell
$env:MYSQL_PWD='Datou123456!'; & 'C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe' '--default-character-set=utf8mb4' '-h192.168.3.5' '-P3306' '-uroot' 'lpsx' '-e' "SELECT 1"
```

---

## 4. 阿里云 OSS 服务凭据

| 项 | 值 |
|---|---|
| Endpoint | `oss-cn-shanghai.aliyuncs.com` |
| Bucket | `lianpeng` |
| AccessKey ID | 通过环境变量 `OSS_ACCESS_KEY_ID` 配置（未写入仓库） |
| AccessKey Secret | 通过环境变量 `OSS_SECRET` 配置（未写入仓库） |

- 支持环境变量覆盖：`OSS_ACCESS_KEY_ID` / `OSS_SECRET`。
- 上传方式：小程序端先调用 `/api/oss/policy` 获取签名直传 OSS，图片统一走 `utils/upload.js`；后端 `OssServiceImpl` 也提供本地文件上传能力（批量工具复用）。
- 图片访问统一经 `resolveImageUrl`（`utils/image.js`）强制 HTTPS + webp 压缩（仅 jpg/png/bmp 追加 `?x-oss-process=image/format,webp/quality,q_80`）。

---

## 5. 其他关键配置与凭据

| 用途 | 值 |
|---|---|
| 微信小程序 AppID | `wxc9192b93d888816d` |
| 微信小程序 Secret | `f26c07cf0132686ebbe5bcba05596280` |
| 后台管理密钥（Header `X-Admin-Key`，保护 `/api/admin/**` 与 `/api/oss/**`） | `LP@870263#lpsx` |
| JWT 签名密钥 | `lpsx2026secretkey32bytesLong!!!!`（环境变量 `JWT_SECRET` 可覆盖） |
| 管理端页面 | `admin/admin.html` + `admin/server.js`（Node 静态服务，监听 `0.0.0.0:3000`，局域网可访问） |

**线上环境（腾讯云，仅作了解，线上库请勿随意改动）**：
- 云托管服务：`lpsx1`，公网地址 `https://lpsx1-295231-8-1463346517.sh.run.tcloudbase.com`
- 云环境 ID：`cloud1-d6g28bq6c04919547`
- CynosDB：`sh-cynosdbmysql-grp-7le4bnf4.sql.tencentcdb.com:29693`
  - 用户名：`lianpeng` / 密码：`Datou870263`
  - **业务库名：`cloud1-d6g28bq6c04919547`**（注意不是 `lpsx`）
  - ⚠️ 线上库与本地库结构可能不一致（如查分工具学校图片表仅存在于本地库），改动前务必先与项目负责人确认。

---

## 6. 接口与鉴权说明

- 拦截器链（`WebMvcConfig`）：IP 限流 → AdminKey → JWT。
- JWT 保护 `/api/**`，白名单已排除：`/api/auth`、`/api/admin`、`/api/score-query`、`/api/schools`、`/api/home` 等。
- 限流：`/api/lead/**` 每分钟 5 次，`/api/auth/**` 每分钟 30 次（内存滑动窗口）。
- 查分工具接口：见 `SCORE_QUERY_API_GUIDE.md`。

---

## 7. 主要模块清单

- 学校 / 招生动态 / 升学评估 / 收藏 / 留资（Lead）/ 用户（微信登录）
- 查分工具（到区/到校/1-15 志愿）：`ScoreQueryController`、`ScoreVolunteerController`
- 学校图片批量上传工具：`ScoreImageAdminController`（`GET /api/admin/score-image/upload?mode=all|logo|gallery`，需 `X-Admin-Key`）
- 管理端留资列表：`LeadAdminController`（`/api/admin/leads`）
- 中考祝福墙：`BlessingController`、`BlessingAdminController`（接口详见 `BLESSING_API_GUIDE.md`，⚠️ `blessing` 表目前仅本地库）

---

## 8. 快速上手

1. 安装 JDK 25 并设置 `JAVA_HOME`（本机：`C:\Users\Admin\AppData\Local\jdks\jdk-25.0.2`）。
2. 确认本地 MySQL `192.168.3.5:3306` 可连，库 `lpsx` 存在。
3. 在项目根执行 `mvn spring-boot:run`。
4. 管理端：`node admin/server.js` 后浏览器访问 `http://<局域网IP>:3000`，操作需携带 `X-Admin-Key`。
