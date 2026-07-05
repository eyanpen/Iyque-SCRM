// 环境变量
const envs = {
  production: {
    // Demo/生产机 (scrm.genetop.top) —— Apache vhost 目前只监听 :80，故用 http://
    // 命名为 production 是为了让 vite build 默认 mode='production' 时能命中此条,
    // 从而 `env.BASE_URL='/tools/'` 被 vite base 正确使用。
    DOMAIN: 'http://scrm.genetop.top',
    BASE_URL: '/tools/',
    BASE_API: 'http://scrm.genetop.top/iyque',
  },
  development: {
    // 测试机 (10.210.156.69:8081) —— 也用作 npm run dev 的默认代理目标
    DOMAIN: 'http://10.210.156.69:8081',
    BASE_URL: '/tools/',
    BASE_API: 'http://10.210.156.69:8081/iyque',
  },
}

let mode =
  process.env.NODE_ENV == 'development' || !globalThis.document
    ? process.env.VUE_APP_ENV
    : Object.keys(envs).find((e) => envs[e].DOMAIN === window?.location.origin)

export const env = { ...envs[mode], ENV: mode }

// 系统常量配置
export const common = {
  SYSTEM_NAME: '护城河', // 系统简称
  SYSTEM_SLOGAN:
    '护城河 · 价值投资顾问 SCRM —— 为高净值客户构筑长期财富的护城河', // 系统标语
  COPYRIGHT: 'Copyright © 2022-2026 护城河 All Rights Reserved.', // 版权信息
  LOGO: env.BASE_URL + 'static/logo.png', // 深色logo
}
