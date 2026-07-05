# Moat SCRM (护城河 SCRM)

**An Integrated CRM Platform for Value-Investing Advisory Firms**

WeCom-based · AI Conversation Insights · Master Methodology Codified · Private Deployment

[简体中文](./README.md) | English

---

## 💡 Positioning

Moat SCRM serves family offices, private banks, boutique fund managers,
and independent value-investing advisors. The platform codifies the
methodologies of Benjamin Graham, Warren Buffett, Charlie Munger, Peter
Lynch, Duan Yongping, Lin Yuan, Dan Bin, and Zhang Mengzhu into reusable
advisory workflows, so every client conversation delivers professional
depth.

---

## 🏗️ Capabilities

| Pipeline | Capability |
|----------|-----------|
| **Acquisition** | Staff QR codes / short links / group codes / H5 landing pages |
| **Client Profile** | Legacy client records + advisor personas + tag system |
| **Advisory** | Conversation archive + AI semantic analysis + master-methodology RAG |
| **Compliance** | Sensitive-word interception + audit review + full operation log |
| **Knowledge** | Investment knowledge base (master writings + classic cases) |
| **AI** | AI advisor assistant + AI summary + intent detection + hot-word insights |

---

## 🛠️ Tech Stack

| Layer | Technology |
|-------|-----------|
| **Backend** | Spring Boot + JPA + MyBatis-Plus + MySQL |
| **Frontend** | Vue 3 + Vite + Pinia + Element Plus |
| **AI** | OpenAI-compatible LLM (Aliyun Bailian qwen3.7-plus) + Ollama Embedding + Qdrant vector DB |

---

## 🚀 Demo Deployment

The demo environment runs on `10.210.156.69`:

| Service | Port |
|---------|------|
| Backend Spring Boot | 8085 |
| MySQL | 3306 |
| FalkorDB (Redis-compatible) | 30869 |
| Qdrant Cloud | 443 (HTTPS) |

- PC: `http://10.210.156.69:8085/tools/`
- Mobile: `http://10.210.156.69:8085/openmobile/`

---

## 📄 License

Moat SCRM is derived from [源雀 SCRM (Iyque SCRM)](https://iyque.cn) under the
Apache License 2.0. Custom modifications by the Moat team retain their own
copyright; the upstream code continues to be governed by the original Apache
License 2.0. See [`LICENSE`](./LICENSE), [`NOTICE.md`](./NOTICE.md), and
[`CHANGELOG.md`](./CHANGELOG.md).
