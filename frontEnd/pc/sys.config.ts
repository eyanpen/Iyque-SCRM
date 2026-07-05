// 环境变量
const envs = {
  development: {
    DOMAIN: 'http://127.0.0.1:8085',
    BASE_URL: '/tools/',
    BASE_API: 'http://127.0.0.1:8085',
  },
  local: {
    DOMAIN: 'http://10.210.156.69:8081',
    BASE_URL: '/tools/',
    BASE_API: 'http://10.210.156.69:8081/iyque',
  },
  test: {
    DOMAIN: 'http://10.210.156.69:8081',
    BASE_URL: '/tools/',
    BASE_API: 'http://10.210.156.69:8081/iyque',
  },
  production: {
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
