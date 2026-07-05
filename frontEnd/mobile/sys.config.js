/**
 * 环境变量
 */
const envs = {
  production: {
    // Demo/生产机 (scrm.genetop.top) —— Apache vhost 目前只监听 :80，故用 http://
    // 命名为 production 是为了让 vite build 默认 mode='production' 时能命中此条。
    DOMAIN: 'http://scrm.genetop.top',
    BASE_URL: '/openmobile/',
    BASE_API: 'http://scrm.genetop.top/iyque',
  },
  development: {
    // 测试机 (10.210.156.69:8081) —— 也用作 npm run dev 的默认代理目标
    DOMAIN: 'http://10.210.156.69:8081',
    BASE_URL: '/openmobile/',
    BASE_API: 'http://10.210.156.69:8081/iyque',
  },
}

const packMode = globalThis.MODE || import.meta.env.MODE
const mode =
  process.env.NODE_ENV == 'development' || !globalThis.document
    ? packMode // 本地开发和vite中使用
    : Object.keys(envs).find((e) => envs[e].DOMAIN === window?.location.origin) || 'diy' // 打包后，根据访问域名动态判断环境

const BASE_URL = envs[packMode].BASE_URL
const env = envs[mode] || {}

// 配置项
export const config = {
  ...env,
  SYSTEM_NAME: '护城河', // 系统简称
}
Object.assign(config, { BASE_URL, RUN_ENV: mode })
