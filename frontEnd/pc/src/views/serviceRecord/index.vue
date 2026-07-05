<script setup lang="ts">
import { getList } from './api'
import { ref } from 'vue'

// 会话原文点击查看 · 多轮对话弹窗
const contentDialogVisible = ref(false)
const currentDetail = ref<any>({})
function openContentDialog(row: any) {
  currentDetail.value = row || {}
  contentDialogVisible.value = true
}
function escapeHtml(s: any) {
  return String(s ?? '')
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
}
// 解析 "[HH:MM] 发言人：正文" 格式的多轮对话
// side='right' 表示客户，'left' 表示投顾/客服
function parseDialog(content: any, customerName: any) {
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
    return { time: '', speaker: '', text: line, side: 'left' }
  })
}
</script>

<template>
  <div :_="$store.setBusininessDesc(`<div>查看所有客服场景中客户咨询的详细记录（每条记录一次完整咨询会话，点击「会话内容」查看多轮对话）</div>`)">
    <RequestChartTable
      ref="rctRef"
      :request="getList"
      searchBtnType="icon"
      @selectionChange="(val) => $emit('selectionChange', val)">
      <template #query="{ query }">
        <el-form-item label="所属客服" prop="kfName">
          <el-input v-model="query.kfName" placeholder="请输入" />
        </el-form-item>
      </template>

      <template #table="{ data }">
        <el-table-column label="咨询客户" fixed="left" prop="" min-width="160">
          <template #default="{ row }">
            <div class="flex items-center">
              <el-image class="flex-none" :src="row.avatar" style="width: 40px; height: 40px; border-radius: 50%;"></el-image>
              <div class="ml10">{{ row.nickname }}</div>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="所属客服" min-width="180" prop="list">
          <template #default="{ row }">
            <div class="flex items-center">
              <el-image class="flex-none" :src="row.kfPicUrl" style="width: 40px; height: 40px; border-radius: 50%;"></el-image>
              <div class="ml10">{{ row.kfName }}</div>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="接待员工" min-width="100" prop="switchUserName">
          <template #default="{ row }">
            {{ row.switchUserName || '-' }}
          </template>
        </el-table-column>
        <el-table-column label="咨询开始时间" prop="sendTime" width="170"></el-table-column>

        <el-table-column label="会话内容" prop="content" min-width="260" show-overflow-tooltip>
          <template #default="{ row }">
            <el-button link type="primary" @click="openContentDialog(row)"
              style="max-width:100%;display:inline-block;overflow:hidden;text-overflow:ellipsis;white-space:nowrap;vertical-align:middle;">
              {{ (row.content || '').replace(/\[\d{1,2}:\d{2}\]\s*/g, '').replace(/\s+/g, ' ').trim().slice(0, 70) }}
              <span style="color:#909399;">…点击查看多轮对话</span>
            </el-button>
          </template>
        </el-table-column>
      </template>
    </RequestChartTable>

    <!-- 多轮会话详情弹窗 -->
    <el-dialog title="咨询记录详情" v-model="contentDialogVisible" width="720px">
      <div style="line-height:1.8;">
        <div style="display:grid;grid-template-columns:110px 1fr;row-gap:10px;">
          <div style="color:#909399;">咨询客户</div>
          <div style="display:flex;align-items:center;">
            <el-image :src="currentDetail.avatar" style="width:32px;height:32px;border-radius:50%;margin-right:8px;"></el-image>
            <span>{{ currentDetail.nickname }}</span>
          </div>

          <div style="color:#909399;">所属客服</div>
          <div style="display:flex;align-items:center;">
            <el-image :src="currentDetail.kfPicUrl" style="width:32px;height:32px;border-radius:50%;margin-right:8px;"></el-image>
            <span>{{ currentDetail.kfName }}</span>
          </div>

          <div style="color:#909399;">接待员工</div>
          <div>{{ currentDetail.switchUserName || '-' }}</div>

          <div style="color:#909399;">咨询开始时间</div>
          <div>{{ currentDetail.sendTime }}</div>
        </div>

        <el-divider />

        <div style="color:#909399;margin-bottom:10px;">会话记录</div>
        <div style="max-height:52vh;overflow-y:auto;padding:6px 10px;background:#f6f8fb;border-radius:8px;">
          <div
            v-for="(turn, idx) in parseDialog(currentDetail.content, currentDetail.nickname)"
            :key="idx"
            :style="{
              display: 'flex',
              flexDirection: turn.side === 'right' ? 'row-reverse' : 'row',
              alignItems: 'flex-start',
              margin: '10px 0',
            }">
            <!-- 头像 -->
            <el-image
              :src="turn.side === 'right' ? currentDetail.avatar : currentDetail.kfPicUrl"
              :style="{
                flex: '0 0 36px',
                width: '36px', height: '36px', borderRadius: '50%',
                margin: turn.side === 'right' ? '0 0 0 8px' : '0 8px 0 0',
              }" />
            <!-- 气泡 -->
            <div style="max-width:78%;">
              <div style="font-size:12px;color:#909399;margin-bottom:2px;"
                :style="{ textAlign: turn.side === 'right' ? 'right' : 'left' }">
                {{ turn.speaker }}<span v-if="turn.time" style="margin-left:6px;">{{ turn.time }}</span>
              </div>
              <div
                :style="{
                  padding: '10px 12px',
                  borderRadius: turn.side === 'right'
                    ? '12px 4px 12px 12px'
                    : '4px 12px 12px 12px',
                  background: turn.side === 'right' ? '#dbe8fb' : '#ffffff',
                  border: turn.side === 'right' ? 'none' : '1px solid #e6ebf5',
                  color: '#303133',
                  whiteSpace: 'pre-wrap',
                  wordBreak: 'break-word',
                  lineHeight: 1.65,
                }">{{ turn.text }}</div>
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

<style scoped lang="scss">
.warning {
  padding: 8px 16px;
  background-color: #fff6f7;
  border-radius: 4px;
  border-left: 5px solid #fe6c6f;
  margin: 20px 0;
  line-height: 40px;
}
</style>
