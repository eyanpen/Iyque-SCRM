# Changelog

本项目基于 [源雀 SCRM](https://iyque.cn)（Apache License 2.0）二次开发，
以下记录了护城河 SCRM 相对上游的显著修改（满足 Apache 2.0 Section 4b "prominent notice" 要求）。

---

## 护城河 SCRM 0.1.0-demo — 2026-07-05

基于源雀 SCRM 上游 commit `f72d9ef` (fix llm slow issue) 分叉。

### 品牌与外观 (Rebrand)

- 将系统显示名从「源雀」替换为「护城河」，同步替换 SLOGAN、COPYRIGHT、水印文案。
- 前端 PC 端：
  - `frontEnd/pc/sys.config.ts` 环境域名统一切换至 `http://10.210.156.69:8085`。
  - `frontEnd/pc/index.html` `<title>` 与 favicon 引用更新。
  - `frontEnd/pc/src/views/system/login/index.vue` 登录页产品介绍改为价值投资顾问定位。
  - `frontEnd/pc/src/components/AiChat.vue` AI 会话背景水印、默认 role prompt。
  - `frontEnd/pc/src/components/WelcomeForm.vue` 默认欢迎语。
  - `frontEnd/pc/src/layout/components/Sidebar/index.vue` 移除对外引流链接。
  - `frontEnd/pc/README.md` 文档同步。
- 前端 Mobile 端：
  - `frontEnd/mobile/sys.config.js` 环境域名统一切换至 `http://10.210.156.69:8085`。
  - `frontEnd/mobile/src/utils/request.js` 域名判断改为 `10.210.156.69`。
- 部署配置：
  - `configFile/nginx.conf` 移除 HTTPS 与 iyque.cn 证书，仅保留 10.210.156.69 HTTP 演示配置。
  - `README.md` / `README.en.md` 重写为护城河产品介绍。
- 后端：
  - `src/main/resources/banner.txt` 启动 banner 替换为护城河 ASCII。

### 演示数据 (Demo data)

- 新增 `configFile/scrm_ky_moat_demo.sql`：为业务表填充 ≥50 条演示数据。
  员工虚构为价值投资大师团（格雷厄姆 / 巴菲特 / 芒格 / 彼得·林奇 / 段永平 /
  林园 / 但斌 / 章盟主 等），客户为其著名咨询对象，会话内容围绕中国资本市场
  经典价值投资案例。

### 保留 (Preserved)

- `LICENSE`（Apache License 2.0 原文，未修改）。
- 全部 `@author ruoyi` 源码注释（`HttpStatus`、`PageDomain`、`ServletUtils`、
  `TableSupport`、`IpUtils`、`StringUtils` 6 个工具类）。
- Java 包名 `cn.iyque.*`、Maven 坐标 `cn.iyque:iyque-code`、数据库表前缀
  `iyque_`：均保留不动，仅前端与客户可见处替换品牌。
