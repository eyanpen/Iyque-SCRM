<script setup>
import { useRoute, useRouter } from 'vue-router'
import * as api from './api'
import * as apiCategory from './apiCategory'

import SelectHotWord from './SelectHotWord.vue'
import synch from './synch.vue'

let loading = ref(false)
let id = useRoute().query.id

let query = ref({})
let cardData = ref([])
// 获取指标数据
;(function getStatistic() {
  loading.value = true
  api
    .getStatistic()
    .then(({ data = {} }) => {
      cardData.value = [
        {
          title: '热词讨论总数',
          tips: '预设的热词讨论累计总数',
          value: data.discussTotalNumber || 0,
        },
        // {
        //   title: '昨日热词讨论数',
        //   tips: '昨日热词讨论总数',
        //   value: data.ydDiscussTotalNumber || 0,
        // },
        // {
        //   title: '昨日客户焦点热词',
        //   tips: '昨日客户讨论热度最高的热词',
        //   value: data.ydHotWord || 0,
        // },
        // {
        //   title: '昨日客户焦点类别',
        //   tips: '昨日客户讨论热度最高的热词分组',
        //   value: data.ydHotWordCategory || 0,
        // },
      ]
    })
    .finally(() => {
      loading.value = false
    })
})()
let hotWordTop5 = ref([])
function dealDataConversionTop(data, series, xData) {
  series.length = 0
  hotWordTop5.value = []
  if (data?.length) {
    let _data = []
    data.forEach((element) => {
      hotWordTop5.value.push(element.hotWord)
      _data.push(element.hotWordDiscussNumber)
    })
    series.push(_data)
  }
}

const categoryList = ref([])
function getCategoryList() {
  apiCategory.getList().then(({ data }) => {
    categoryList.value = data
  })
}
getCategoryList()

let hotWordCategoryTop5 = ref([])
function dealDataWordCategoryTop5(data, series, xData) {
  series.length = 0
  hotWordCategoryTop5.value = []
  if (data?.length) {
    let _data = []
    data.forEach((element) => {
      hotWordCategoryTop5.value.push(element.hotWord)
      _data.push(element.hotWordDiscussNumber)
    })
    series.push(_data)
  }
}

// 数据趋势 数据处理
function dealDataTrend(data, series, xData) {
  xData.length = 0
  series.length = 0
  let _data = [[]]
  xData.push(...data.xdata)
  series.push(...data.series)
  // data.series?.forEach((element) => {
  //   xData.push(element.xdata)
  //   _data[0].push(element.addGroupTotalNumber)
  // })
  // series.push(..._data)
}

// 使用 reactive 创建响应式对象
const state = reactive({
  loading: false,
  id: useRoute().query.id,
  dialogAiVisible: false,
  form: {},
})

// 方法：打开 AI 热词分析对话框
const openAIDialog = () => {
  state.form = {} // 重置表单
  state.dialogAiVisible = true // 显示对话框
}

// 方法：提交 AI 对话（假设你需要这个方法）
const synchRef = ref()
const submitAiVisible = () => {
  state.loading = true
  synchRef.value
    .submit()
    .then(() => {
      state.dialogAiVisible = false
    })
    .catch((e) => console.error(e))
    .finally(() => (state.loading = false))
  // state.loading = true
  // 这里调用你的 API 提交逻辑，例如 api.submitAI(state.form)
  // 处理完成后重置 loading 和关闭对话框
  // api.submitAI(state.form).then(response => {
  //   state.dialogAiVisible = false
  //   state.loading = false
  // }).catch(error => {
  //   console.error(error)
  //   state.loading = false
  // })
}

// 会话原文 · 点击查看全文
const contentDialogVisible = ref(false)
const currentDetail = ref({})
function openContentDialog(row) {
  currentDetail.value = row || {}
  contentDialogVisible.value = true
}
// HTML 转义
function escapeHtml(s) {
  return String(s ?? '')
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
}
// 高亮命中的热词
function highlightHotWord(content, hotWord) {
  const safe = escapeHtml(content)
  if (!hotWord) return safe
  const escaped = String(hotWord).replace(/[.*+?^${}()|[\]\\]/g, '\\$&')
  return safe.replace(
    new RegExp(escaped, 'g'),
    (m) => `<span style="color:#c47c11;font-weight:600;background:#fff7dc;padding:0 2px;border-radius:2px;">${m}</span>`
  )
}
// 解析多轮对话：每行 "[HH:MM] 发言人：正文"
// 返回 [{ time, speaker, text, side }]，side='left' 员工 / 'right' 客户
function parseDialog(content, customerName) {
  const raw = String(content ?? '')
  if (!raw) return []
  const lines = raw.split(/\r?\n/).filter((l) => l.trim())
  const re = /^\[(\d{1,2}:\d{2})\]\s*([^：:]+)[:：]\s*(.*)$/
  return lines.map((line) => {
    const m = line.match(re)
    if (m) {
      const [, time, speaker, text] = m
      return {
        time,
        speaker: speaker.trim(),
        text,
        side: speaker.trim() === customerName ? 'right' : 'left',
      }
    }
    // fallback：不带时间前缀的整段文本
    return { time: '', speaker: '', text: line, side: 'left' }
  })
}
</script>

<template>
  <div>
<div class="fxbw">
      <!-- 绑定点击事件到 openAIDialog 方法 -->
      <el-button type="primary" @click="openAIDialog">AI热词分析</el-button>
    </div>
    <br />

    <el-dialog title="AI会话预审" v-model="state.dialogAiVisible" width="80%" :close-on-click-modal="false">
      <!-- 使用 synch 组件，并绑定 data -->
      <synch v-if="state.dialogAiVisible" :data="state.form" ref="synchRef"></synch>

      <!-- 对话框底部按钮 -->
      <template #footer>
        <el-button @click="state.dialogAiVisible = false">取消</el-button>
        <el-button type="primary" @click="submitAiVisible" :loading="state.loading">确定</el-button>
      </template>
    </el-dialog>

    <CardGroup :data="cardData" />

    <div class="grid grid-cols-2 --Gap --MarginT">
      <RequestChartTable
        title="热词Top5"
        type="barChart"
        isTimeQuery
        :request="api.hotWordTop5"
        :option="{
          xAxis: [{ type: 'value' }],
          yAxis: [{ type: 'category', data: hotWordTop5 }],
        }"
        :dealDataFun="dealDataConversionTop"></RequestChartTable>

      <RequestChartTable
        class="mt0"
        ref="rctRef"
        title="分类热词 Top5"
        type="barChart"
        isTimeQuery
        :searchBtnType="false"
        :request="api.hotWordCategoryTop5"
        :option="{
          xAxis: [{ type: 'value' }],
          yAxis: [{ type: 'category', data: hotWordCategoryTop5 }],
        }"
        :dealDataFun="dealDataWordCategoryTop5">
        <template #queryMiddle="{ query }">
          <el-form-item label="热词分类" prop="categoryId">
            <el-select v-model="query.categoryId" :popper-append-to-body="false" @change="$refs.rctRef.getList()">
              <el-option v-for="(item, key) in categoryList" :key="key" :label="item.name" :value="item.id" />
            </el-select>
          </el-form-item>
        </template>
      </RequestChartTable>
    </div>

    <RequestChartTable
      ref="rctRef2"
      type="table"
      title="热词讨论明细"
      isTimeQuery
      :request="api.findAll"
      :params="query"
      @reset=";(query._name = ''), (query.hotWordId = '')">
      <template #queryMiddle="{}">
        <el-form-item label="选择热词" prop="categoryId">
          <el-input
            v-model="query._name"
            placeholder="请选择热词"
            @click="$refs.selectHotWordRef.dialogRef.visible = true" />

          <SelectHotWord
            ref="selectHotWordRef"
            @confirm="
              ({ visible, loading, selected }) => (
                (query._name = selected?.map((e) => e.hotWord)?.join(',')),
                (query.hotWordIds = selected?.map((e) => e.id)?.join(',')),
                (visible.value = false),
                (loading.value = false),
                $nextTick(() => {
                  $refs.rctRef2.getList(1)
                })
              )
            "></SelectHotWord>
        </el-form-item>
      </template>

      <template #table="{ data }">
        <el-table-column label="热词讨论客户" prop="fromName"></el-table-column>
        <el-table-column label="会话员工" prop="acceptName" show-overflow-tooltip />

        <el-table-column label="热词" prop="hotWordName"></el-table-column>
        <el-table-column label="热词分类" prop="categoryName"></el-table-column>

        <el-table-column label="会话原文" prop="content" min-width="200px" show-overflow-tooltip>
          <template #default="{ row }">
            <el-button link type="primary" @click="openContentDialog(row)"
              style="max-width:100%;display:inline-block;overflow:hidden;text-overflow:ellipsis;white-space:nowrap;vertical-align:middle;">
              {{ (row.content || '').replace(/\[\d{1,2}:\d{2}\]\s*/g, '').replace(/\s+/g, ' ').trim().slice(0, 60) }}
              <span style="color:#909399;">…点击查看多轮对话</span>
            </el-button>
          </template>
        </el-table-column>
        <el-table-column label="讨论时间" prop="msgTime" show-overflow-tooltip />
      </template>
    </RequestChartTable>

    <!-- 会话原文 · 全文详情 -->
    <el-dialog title="会话原文详情" v-model="contentDialogVisible" width="720px">
      <div style="line-height:1.8;">
        <div style="display:grid;grid-template-columns:110px 1fr;row-gap:10px;">
          <div style="color:#909399;">客户</div>
          <div>{{ currentDetail.fromName }}</div>

          <div style="color:#909399;">会话员工</div>
          <div>{{ currentDetail.acceptName }}</div>

          <div style="color:#909399;">命中热词</div>
          <div>
            <el-tag effect="light" type="warning">{{ currentDetail.hotWordName }}</el-tag>
            <span style="margin-left:8px;color:#909399;font-size:13px;">
              分类：{{ currentDetail.categoryName }}
            </span>
          </div>

          <div style="color:#909399;">讨论时间</div>
          <div>{{ currentDetail.msgTime }}</div>
        </div>

        <el-divider />

        <div style="color:#909399;margin-bottom:10px;">会话记录</div>
        <div style="max-height:52vh;overflow-y:auto;padding:4px 8px;background:#f6f8fb;border-radius:8px;">
          <div
            v-for="(turn, idx) in parseDialog(currentDetail.content, currentDetail.fromName)"
            :key="idx"
            :style="{
              display: 'flex',
              flexDirection: turn.side === 'right' ? 'row-reverse' : 'row',
              alignItems: 'flex-start',
              margin: '10px 0',
            }">
            <!-- 头像圆点 -->
            <div
              :style="{
                flex: '0 0 32px',
                width: '32px', height: '32px', borderRadius: '50%',
                background: turn.side === 'right' ? '#3b6bb2' : '#c47c11',
                color: '#fff',
                display: 'flex', alignItems: 'center', justifyContent: 'center',
                fontSize: '13px', fontWeight: 600,
                margin: turn.side === 'right' ? '0 0 0 8px' : '0 8px 0 0',
              }">
              {{ (turn.speaker || '?').slice(0, 1) }}
            </div>
            <!-- 气泡 -->
            <div style="max-width:78%;">
              <div style="font-size:12px;color:#909399;margin-bottom:2px;"
                :style="{ textAlign: turn.side === 'right' ? 'right' : 'left' }">
                {{ turn.speaker }} <span v-if="turn.time" style="margin-left:6px;">{{ turn.time }}</span>
              </div>
              <div
                :style="{
                  padding: '9px 12px',
                  borderRadius: turn.side === 'right'
                    ? '12px 4px 12px 12px'
                    : '4px 12px 12px 12px',
                  background: turn.side === 'right' ? '#dbe8fb' : '#ffffff',
                  border: turn.side === 'right' ? 'none' : '1px solid #e6ebf5',
                  color: '#303133',
                  whiteSpace: 'pre-wrap',
                  wordBreak: 'break-word',
                  lineHeight: 1.6,
                }"
                v-html="highlightHotWord(turn.text, currentDetail.hotWordName)"></div>
            </div>
          </div>
        </div>
      </div>
      <template #footer>
        <el-button @click="contentDialogVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style lang="scss" scoped>
.warning {
  padding: 8px 16px;
  background-color: #fff6f7;
  border-radius: 4px;
  border-left: 5px solid #fe6c6f;
  margin: 20px 0;
  line-height: 40px;
}

.like {
  cursor: pointer;
  font-size: 25px;
  display: inline-block;
}
</style>
