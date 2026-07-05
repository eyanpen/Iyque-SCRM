<template>
  <div class="loginPage">
    <div class="w-[1200px] h-[730px] centerCenter flex --Radius">
      <div class="w-[50%] bg-(--ColorLight8) p-[40px]">
        <img
          class="h-[35px]"
          style="vertical-align: middle"
          v-if="sysConfig.LOGO"
          :src="$store.app.isDark ? sysConfig.LOGO : sysConfig.LOGO"
          alt="" />
        <span class="text-[29px] blod ml5" style="vertical-align: middle">{{ sysConfig.SYSTEM_NAME }}</span>
        <el-tag class="bold" type="primary" size="large" effect="dark">价值投资顾问版</el-tag>

        <div class="bold mt20 font16 mb-[90px]">
          为高净值客户构筑长期财富的护城河
        </div>
        <div class="leading-[40px]">
          护城河 SCRM 是面向价值投资顾问机构的一体化客户关系与咨询管理平台，将大师级投研方法论、AI 会话洞察与合规存档融为一体，帮助家族办公室、私人银行与私募机构实现四大目标：
          <ul>
            <li class="!list-disc !list-inside !pl-[30px]">投顾专家画像 + 客户传承档案，一人一策</li>
            <li class="!list-disc !list-inside !pl-[30px]">AI 语义分析会话，识别客户长期意图</li>
            <li class="!list-disc !list-inside !pl-[30px]">投研知识库 RAG 检索，秒级引用大师原话</li>
            <li class="!list-disc !list-inside !pl-[30px]">合规存档 + 敏感词拦截，满足监管要求</li>
          </ul>
          护城河致力于将本杰明·格雷厄姆、沃伦·巴菲特、查理·芒格、彼得·林奇、段永平、林园、但斌、章盟主等价值投资大师的智慧沉淀为可复用的投顾流程，让每一次客户对话都传递专业。
        </div>
        <div class="--Color bold font16 mt20">
          <span>护城河 · 让专业成为客户的护城河</span>
        </div>
      </div>
      <div class="w-[50%] bg-(--BgWhite) relative">
        <div class="centerCenter">
          <div class="text-[29px] blod mb-[30px]">登录</div>
          <el-form
            ref="loginForm"
            size="large"
            :model="loginForm"
            :rules="loginRules"
            class="login-form"
            style="--formItemWidth: 390px">
            <template v-if="loginType === 'username'">
              <el-form-item prop="username">
                <el-input v-model.trim="loginForm.username" type="text" auto-complete="off" placeholder="账号">
                  <template #prefix>
                    <svg-icon icon="user" class="input-icon" />
                  </template>
                </el-input>
              </el-form-item>
              <el-form-item prop="password">
                <el-input
                  v-model.trim="loginForm.password"
                  :type="ispassword ? 'password' : 'text'"
                  auto-complete="off"
                  placeholder="密码"
                  @keyup.enter="login">
                  <template #prefix>
                    <svg-icon icon="password" class="input-icon" />
                  </template>
                  <template #suffix>
                    <svg-icon
                      :icon="ispassword ? 'eye' : 'eye-open'"
                      class="input-icon cp"
                      @click="ispassword = !ispassword" />
                  </template>
                </el-input>
              </el-form-item>

              <div class="fxbw g-margin-b">
                <el-checkbox class="fr" v-model="loginForm.rememberMe">记住密码</el-checkbox>
              </div>
              <el-form-item>
                <el-button :loading="loading" type="default" class="w-[100%]" @click.prevent="login">
                  <span v-if="!loading">立即登录</span>
                  <span v-else>登 录 中...</span>
                </el-button>
              </el-form-item>
              <!-- <a :href="threeLoginInfo.threeLoginUrl" v-if="threeLoginInfo.startThreeLogin">
                <el-button class="w-[100%] blod" type="primary" @click="">
                  <svg-icon icon="gitee" class="text-[25px] mr5" />
                  使用 Gitee 账号 Star 一下，直接免密登录
                </el-button>
              </a> -->
            </template>
          </el-form>
        </div>
      </div>
    </div>
    <div class="fixed bottom-[30px] left-[50%] translate-x-[-50%] --FontBlack5 font12">
      <span v-html="sysConfig.COPYRIGHT"></span>
    </div>
  </div>
</template>

<script>
import { login, findThreeLoginInfo, giteeLogin } from './api'
import Cookies from 'js-cookie'
import { setToken } from '@/utils/auth'

export default {
  name: 'Login',
  data() {
    return {
      loginType: 'username',
      ispassword: true,
      loginForm: {
        username: '',
        password: '',
        rememberMe: false,
        code: '',
        uuid: '',
      },
      loginRules: {
        username: [{ required: true, trigger: 'blur', message: '用户名不能为空' }],
        password: [{ required: true, trigger: 'blur', message: '密码不能为空' }],
        code: [{ required: true, trigger: 'change', message: '验证码不能为空' }],
      },
      authLink: '',
      loading: false,
      redirect: undefined,
      dialogVisible: true,
      isDemonstrationLogin: false,
      threeLoginInfo: {},
    }
  },
  watch: {
    $route: {
      handler: function (route) {
        this.redirect = route.query && route.query.redirect
      },
      immediate: true,
    },
  },
  created() {
    this.getCookie()
    this.findThreeLoginInfo()
  },
  methods: {
    findThreeLoginInfo() {
      findThreeLoginInfo().then(({ data }) => {
        this.threeLoginInfo.startThreeLogin = data.startThreeLogin
        this.threeLoginInfo.threeLoginUrl = data.threeLoginUrl
      })
    },
    getCookie() {
      const username = Cookies.get('username')
      const password = Cookies.get('password')
      const rememberMe = Cookies.get('rememberMe')
      this.loginForm = {
        username: username === undefined ? this.loginForm.username : username,
        password: password === undefined ? this.loginForm.password : password,
        rememberMe: rememberMe === undefined ? false : Boolean(rememberMe),
      }
    },
    login() {
      this.$refs.loginForm.validate((valid) => {
        if (valid) {
          this.loading = true
          if (this.loginForm.rememberMe) {
            Cookies.set('username', this.loginForm.username, { expires: 30 })
            Cookies.set('password', this.loginForm.password, {
              expires: 30,
            })
            Cookies.set('rememberMe', this.loginForm.rememberMe, {
              expires: 30,
            })
          } else {
            Cookies.remove('username')
            Cookies.remove('password')
            Cookies.remove('rememberMe')
          }
          let loginForm = JSON.parse(JSON.stringify(this.loginForm))
          loginForm.password = this.loginForm.password
          login(loginForm)
            .then(({ data }) => {
              setToken(data.token)
              this.$router.push(this.redirect || '/')
            })
            .catch(() => {
              this.loading = false
            })
        }
      })
    },
  },
}
</script>

<style lang="scss" scoped>
.loginPage {
  width: 100%;
  min-height: 100%;
  background: #f0f2f5 url(./bg.svg) no-repeat 50%;
  background-size: 100%;
}

.login-form {
  // .el-input {
  //   height: 38px;
  //   line-height: 38px;
  //   input {
  //     height: 38px;
  //   }
  // }
  .desc {
    text-align: center;
    color: var(--font-black-7);
    font-size: 12px;
    margin: -22px 0 50px;
  }
  .input-icon {
    font-size: 16px;
  }
}
</style>
