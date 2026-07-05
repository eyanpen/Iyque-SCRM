# 护城河 SCRM 前端 (PC)

护城河价值投资顾问 SCRM 的 PC 端前端源码库，技术栈：[Vue3](https://cn.vuejs.org/) + [Vite](https://cn.vitejs.dev/) + [Pinia](https://pinia.vuejs.org/zh/) + [Element Plus](https://element-plus.gitee.io/zh-CN/)。

## 前端结构

```
├── you-project-name              // 后台管理端项目
    ├── public                    # 静态资源
    │   │── static                # 用于绝对路径的非打包资源，公用基础 css 等
    │   │── favicon.ico           # favicon 图标
    ├── src                       # 源代码
    │   ├── api                   # 后端公共接口请求
    │   ├── assets                # 主题 字体 svg icons 等静态资源
    │   ├── components            # 全局公用组件
    │   ├── layout                # 全局基础布局结构组件
    │   ├── router                # 路由 权限管理等
    │   ├── stores                # 全局 pinia store 管理
    │   ├── styles                # 全局样式
    │   ├── utils                 # 全局公用方法
    │   ├── views                 # 业务功能所有页面
    │   ├── App.vue               # 入口页面
    │   ├── config.js             # 全局配置文件
    │   ├── main.ts               # 入口文件 加载组件 初始化等
    ├── babel.config.js           # babel-loader 配置
    ├── sys.config.ts             # 系统环境配置
    ├── index.html                # html 模板
    ├── jsconfig.json             # jsconfig 配置 快捷路径等
    ├── package.json              # package.json
    ├── vite.config.ts            # vite 配置
```

## 系统配置

在 [sys.config.ts](./sys.config.ts) 中配置开发、生产等各个环境的接口域名、路由基础路径与页面基础路径等：

```ts
// 环境变量
const envs = {
  development: {
    DOMAIN: 'http://127.0.0.1:8085',
    BASE_URL: '/tools/',
    BASE_API: 'http://127.0.0.1:8085',
  },
  production: {
    DOMAIN: 'http://10.210.156.69:8085',
    BASE_URL: '/tools/',
    BASE_API: 'http://10.210.156.69:8085/iyque',
  },
}

// 系统常量配置
export const common = {
  SYSTEM_NAME: '护城河',
  SYSTEM_SLOGAN: '护城河 · 价值投资顾问 SCRM —— 为高净值客户构筑长期财富的护城河',
  COPYRIGHT: 'Copyright © 2022-2026 护城河 All Rights Reserved.',
  LOGO: env.BASE_URL + 'static/logo.png',
}
```

## 运行与部署

**Node 推荐 16.x 及以上版本**

```sh
# 进入项目目录
cd frontEnd/pc

# 安装依赖
npm i
# 或者使用 cnpm
npm install -g cnpm --registry=https://registry.npmmirror.com
cnpm i

# 启动开发服务
npm run dev

# 构建生产环境
npm run build
```
