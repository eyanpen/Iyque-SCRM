import request from '@/utils/request'

// SSE 端点必须自己 fetch (不能走 axios 封装), 但 base URL 逻辑要与 request.js 保持一致:
//   dev  → /api   (Vite proxy 转发到本地后端)
//   prod → window.sysConfig.BASE_API  (Apache /iyque/ 反代)
// 参见 pc/src/utils/request.js:10
const SSE_BASE = process.env.NODE_ENV === 'development'
  ? '/api'
  : window.sysConfig.BASE_API

export function chatWithMemory(data) {
  return request({
    url: '/iYqueAi/chatWithMemory',
    method: 'post',
    data
  })
}

export function getAvailableModels() {
  return request({
    url: '/iYqueAi/models',
    method: 'get'
  })
}

export function getFunctionRoutes() {
  return request({
    url: '/iYqueAi/functionRoutes',
    method: 'get'
  })
}

/**
 * 拉取所有知识库列表，供 AI Chat 顶部知识库选择器使用。
 * 后端源: /knowledge/findAll (cn.iyque.controller.IYqueKnowledgeController)
 * 返回结构: { code, msg, data: [{ id, kname, description, createTime, ... }] }
 */
export function getKnowledgeList() {
  return request({
    url: '/knowledge/findAll',
    method: 'get'
  })
}

/**
 * 拉取 RAG 引用的完整片段内容。前端点击 [资料 N] 时调用。
 * 后端源: /iYqueAi/rag/fragment/{fid} (IYqueAiController.getRagFragment)
 * 返回 data: { fid, kid, docId, idx, content, docName, docType, downloadUrl }
 */
export function getRagFragment(fid) {
  return request({
    url: `/iYqueAi/rag/fragment/${fid}`,
    method: 'get'
  })
}

/**
 * 拼装原始附件下载 URL。用户点击 [资料 N] 弹窗里的"下载原始文件"时用这个 URL。
 * 后端源: /knowledge/attach/download/{docId} (IYqueKnowledgeController.downloadAttach)
 * 前端调用侧走 fetch + blob (自带 Bearer token), 不能用 window.open (无法带 header).
 */
export function buildAttachDownloadUrl(docId) {
  // request.js 里 baseURL 的计算逻辑一致: dev 走 /api 代理, prod 走 window.sysConfig.BASE_API
  const base = process.env.NODE_ENV === 'development' ? '/api' : window.sysConfig.BASE_API
  return `${base}/knowledge/attach/download/${docId}`
}

export function chatWithMemoryStream(data, onMessage, onError, onComplete, onCitations) {
  // onCitations: 可选回调, 收到 RAG 引用元数据首帧时触发, 参数为 citations 数组
  //   [{ idx, fid, docId, kid, docName, score }, ...]
  const url = `${SSE_BASE}/iYqueAi/chatWithMemoryStream`
  
  return new Promise((resolve, reject) => {
    let fullResponse = ''
    const TIMEOUT_DURATION = 120000
    let timeoutId = null
    
    const token = localStorage.getItem('Admin-Token') || ''
    
    const headers = {
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${token}`,
      ...window.sysConfig.headers
    }
    
    console.log('[SSE] 开始请求:', { url, data })
    
    timeoutId = setTimeout(() => {
      console.error('[SSE] 请求超时')
      const error = new Error('响应超时，请稍后重试')
      if (onError) onError(error)
      reject(error)
    }, TIMEOUT_DURATION)
    
    fetch(url, {
      method: 'POST',
      headers: headers,
      body: JSON.stringify(data)
    })
    .then(response => {
      console.log('[SSE] 响应状态:', response.status, response.ok)
      if (!response.ok) {
        throw new Error(`HTTP error! status: ${response.status}`)
      }
      
      const reader = response.body.getReader()
      const decoder = new TextDecoder()
      let buffer = ''
      
      function processText(text) {
        buffer += text
        console.log('[SSE] 当前buffer:', JSON.stringify(buffer))
        
        const lines = buffer.split('\n\n')
        buffer = lines.pop() || ''
        
        console.log('[SSE] 分割后的lines:', lines)
        console.log('[SSE] 剩余buffer:', JSON.stringify(buffer))
        
        for (const line of lines) {
          console.log('[SSE] 处理line:', JSON.stringify(line))
          
          let content = line
          if (line.startsWith('data:')) {
            content = line.replace(/^data:\s*/, '')
          }
          
          console.log('[SSE] 处理后的content:', JSON.stringify(content))
          
          if (content.trim() === '[DONE]') {
            console.log('[SSE] 收到结束标记')
            clearTimeout(timeoutId)
            if (onComplete) onComplete(fullResponse)
            resolve(fullResponse)
            return true
          }
          
          if (content.includes('"error"')) {
            try {
              const errorObj = JSON.parse(content)
              console.error('[SSE] 收到错误:', errorObj)
              clearTimeout(timeoutId)
              const error = new Error(errorObj.error || '未知错误')
              if (onError) onError(error)
              reject(error)
              return true
            } catch (e) {
              console.log('[SSE] 解析错误失败，作为普通数据处理')
            }
          }
          
          if (content.trim()) {
            // RAG 引用元数据首帧: 后端会以 __RAG_CITATIONS__<JSON> 为首个 chunk 发送，
            // 前端识别后走 onCitations 回调, 不追加到 AI 回答正文里。
            if (content.startsWith('__RAG_CITATIONS__')) {
              try {
                const citations = JSON.parse(content.substring('__RAG_CITATIONS__'.length))
                console.log('[SSE][RAG] 收到 citations:', citations.length, '条')
                if (onCitations) onCitations(citations)
              } catch (e) {
                console.warn('[SSE][RAG] 解析 citations JSON 失败:', e, content.slice(0, 100))
              }
              continue    // 关键: 不 append 到 fullResponse, 不触发 onMessage
            }

            clearTimeout(timeoutId)
            timeoutId = setTimeout(() => {
              console.error('[SSE] 请求超时')
              const error = new Error('响应超时，请稍后重试')
              if (onError) onError(error)
              reject(error)
            }, TIMEOUT_DURATION)
            
            fullResponse += content
            console.log('[SSE] 调用onMessage, 内容:', JSON.stringify(content))
            console.log('[SSE] 当前完整响应:', JSON.stringify(fullResponse))
            if (onMessage) onMessage(content)
          }
        }
        return false
      }
      
      function read() {
        reader.read().then(({ done, value }) => {
          console.log('[SSE] read状态:', { done, valueLength: value?.length })
          
          if (done) {
            console.log('[SSE] 流结束')
            if (buffer.trim()) {
              processText('\n\n')
            }
            clearTimeout(timeoutId)
            console.log('[SSE] 最终响应:', JSON.stringify(fullResponse))
            if (onComplete) onComplete(fullResponse)
            resolve(fullResponse)
            return
          }
          
          const chunk = decoder.decode(value, { stream: true })
          console.log('[SSE] 收到chunk:', JSON.stringify(chunk))
          
          const shouldStop = processText(chunk)
          if (!shouldStop) {
            read()
          }
        })
        .catch(error => {
          console.error('[SSE] 读取错误:', error)
          clearTimeout(timeoutId)
          if (onError) onError(error)
          reject(error)
        })
      }
      
      read()
    })
    .catch(error => {
      console.error('[SSE] 请求错误:', error)
      clearTimeout(timeoutId)
      if (onError) onError(error)
      reject(error)
    })
  })
}

export function navigationChatStream(data, onMessage, onError, onComplete, onCitations) {
  // onCitations: 可选回调, 收到 __RAG_CITATIONS__ 首帧时触发。
  //   与 chatWithMemoryStream 一致的协议; 导航模式也可以带 kid 做 RAG。
  const url = `${SSE_BASE}/iYqueAi/navigationChatStream`
  
  return new Promise((resolve, reject) => {
    let fullResponse = ''
    const TIMEOUT_DURATION = 120000
    let timeoutId = null
    
    const token = localStorage.getItem('Admin-Token') || ''
    
    const headers = {
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${token}`,
      ...window.sysConfig.headers
    }
    
    console.log('[Navigation SSE] 开始请求:', { url, data })
    
    timeoutId = setTimeout(() => {
      console.error('[Navigation SSE] 请求超时')
      const error = new Error('响应超时，请稍后重试')
      if (onError) onError(error)
      reject(error)
    }, TIMEOUT_DURATION)
    
    fetch(url, {
      method: 'POST',
      headers: headers,
      body: JSON.stringify(data)
    })
    .then(response => {
      console.log('[Navigation SSE] 响应状态:', response.status, response.ok)
      if (!response.ok) {
        throw new Error(`HTTP error! status: ${response.status}`)
      }
      
      const reader = response.body.getReader()
      const decoder = new TextDecoder()
      let buffer = ''
      
      function processText(text) {
        buffer += text
        
        const lines = buffer.split('\n\n')
        buffer = lines.pop() || ''
        
        for (const line of lines) {
          let content = line
          if (line.startsWith('data:')) {
            content = line.replace(/^data:\s*/, '')
          }
          
          if (content.trim() === '[DONE]') {
            clearTimeout(timeoutId)
            if (onComplete) onComplete(fullResponse)
            resolve(fullResponse)
            return true
          }
          
          if (content.includes('"error"')) {
            try {
              const errorObj = JSON.parse(content)
              clearTimeout(timeoutId)
              const error = new Error(errorObj.error || '未知错误')
              if (onError) onError(error)
              reject(error)
              return true
            } catch (e) {
            }
          }
          
          if (content.trim()) {
            // 与 chatWithMemoryStream 同样的 __RAG_CITATIONS__ 首帧处理
            if (content.startsWith('__RAG_CITATIONS__')) {
              try {
                const citations = JSON.parse(content.substring('__RAG_CITATIONS__'.length))
                console.log('[Navigation SSE][RAG] 收到 citations:', citations.length, '条')
                if (onCitations) onCitations(citations)
              } catch (e) {
                console.warn('[Navigation SSE][RAG] 解析 citations JSON 失败:', e)
              }
              continue
            }

            clearTimeout(timeoutId)
            timeoutId = setTimeout(() => {
              const error = new Error('响应超时，请稍后重试')
              if (onError) onError(error)
              reject(error)
            }, TIMEOUT_DURATION)
            
            fullResponse += content
            if (onMessage) onMessage(content)
          }
        }
        return false
      }
      
      function read() {
        reader.read().then(({ done, value }) => {
          if (done) {
            if (buffer.trim()) {
              processText('\n\n')
            }
            clearTimeout(timeoutId)
            if (onComplete) onComplete(fullResponse)
            resolve(fullResponse)
            return
          }
          
          const chunk = decoder.decode(value, { stream: true })
          
          const shouldStop = processText(chunk)
          if (!shouldStop) {
            read()
          }
        })
        .catch(error => {
          console.error('[Navigation SSE] 读取错误:', error)
          clearTimeout(timeoutId)
          if (onError) onError(error)
          reject(error)
        })
      }
      
      read()
    })
    .catch(error => {
      console.error('[Navigation SSE] 请求错误:', error)
      clearTimeout(timeoutId)
      if (onError) onError(error)
      reject(error)
    })
  })
}

export function getConversationList() {
  return request({
    url: '/ai/conversation/list',
    method: 'get',
    params: { deviceType: 1 }
  })
}

export function createConversation(data) {
  return request({
    url: '/ai/conversation/create',
    method: 'post',
    data: { ...data, deviceType: 1 }
  })
}

export function updateConversation(data) {
  return request({
    url: '/ai/conversation/update',
    method: 'put',
    data: { ...data, deviceType: 1 }
  })
}

export function deleteConversation(conversationId) {
  return request({
    url: `/ai/conversation/delete/${conversationId}`,
    method: 'delete',
    params: { deviceType: 1 }
  })
}

export function getConversationMessages(conversationId) {
  return request({
    url: `/ai/conversation/messages/${conversationId}`,
    method: 'get',
    params: { deviceType: 1 }
  })
}

export function saveConversationMessage(data) {
  return request({
    url: '/ai/conversation/message/save',
    method: 'post',
    data: { ...data, deviceType: 1 }
  })
}

export function saveConversationMessages(conversationId, messages) {
  return request({
    url: `/ai/conversation/messages/save/${conversationId}`,
    method: 'post',
    data: { messages, deviceType: 1 }
  })
}

export function getConversationDetail(conversationId) {
  return request({
    url: `/ai/conversation/detail/${conversationId}`,
    method: 'get',
    params: { deviceType: 1 }
  })
}
