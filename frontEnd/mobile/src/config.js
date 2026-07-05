import { config } from '../sys.config'
const BASE_URL = import.meta.env.BASE_URL

// 系统常量
window.sysConfig = {
  ...config,
  ...(globalThis.sysConfig || {}),

  BASE_URL,
  RUN_ENV: config.RUN_ENV,
  // 开发免登录 token 已删除。本地调试请通过接口获取真实 token 后手动填入
  // sessionStorage.token，或在 .env.local 里配 VITE_DEV_TOKEN 后自行接入。

  services: {
    wecom: '/open',
    weChat: '/wx-api',
    ai: '/ai',
  },

  // 以下仅用于系统信息展示，不作为项目变量使用，请勿在代码中使用
  _packDateTime: __PACK_DATETIME__, // 打包时间
  _mode: import.meta.env.MODE, // 前端打包模式
}
// window.sysConfig.TOKEN 已移除；下面这行保留是历史向后兼容，实际不会触发
process.env.NODE_ENV === 'development' && window.sysConfig.TOKEN && (sessionStorage.token = window.sysConfig.TOKEN)
// 统一为img的src不是绝对地址的拼接接口地址
document.addEventListener(
  'error',
  function (e) {
    let target = e.target
    let src = target.attributes.getNamedItem('src').value
    if (target.tagName.toUpperCase() === 'IMG' && src && !src.includes('http')) {
      target.src = window.sysConfig.BASE_API + '/file/fileView/' + src
      e.stopPropagation()
    }
  },
  true,
)
