# PlotAssistant（剧情创作助手）

[![Docker](https://img.shields.io/badge/Docker-%E2%9C%93-blue)](https://www.docker.com/)
[![Vue 3](https://img.shields.io/badge/Vue-3-green)](https://vuejs.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-2.x-brightgreen)](https://spring.io/projects/spring-boot)
[![License](https://img.shields.io/badge/License-MIT-yellow.svg)](./LICENSE)

PlotAssistant 是一款面向剧情创作者的 **AI 辅助剧情树构建工具**。项目采用 Vue 3 + Spring Boot 全栈架构，支持从世界观设定到完整剧情脚本的九层 AI 生成管线，并提供基于 `@vue-flow/core` 的可视化画布，让创作者能够直观地拖拽、编辑剧情节点与分支连线。

PlotAssistant剧情创作工具,在线预览地址http://163.245.210.83/。（默认演示账户:usrname：1，password：1)

---

## 核心功能

- **L1 ~ L9 生成管线**：世界观、角色、大纲、节点、场景、NPC、对话、分支、完整脚本
- **可视化剧情画布**：基于 Vue Flow 的节点拖拽、连线编辑、自动布局
- **AI 生成集成**：支持多层级 AI 内容生成与人工干预
- **历史版本管理**：正史/备选版本切换与回溯
- **项目化管理**：多项目隔离，支持角色库、世界观库独立维护

---

## 技术栈

### 前端

- **框架**：Vue 3（Composition API + `<script setup>`）
- **语言**：TypeScript
- **构建**：Vite 4
- **UI 库**：Element Plus 2
- **状态管理**：Pinia 2
- **路由**：Vue Router 4（History 模式）
- **可视化**：@vue-flow/core + @vue-flow/background + @vue-flow/controls
- **HTTP**：Axios

### 后端

- **框架**：Spring Boot
- **语言**：Java 8
- **ORM**：JPA / Hibernate
- **数据库**：MySQL 8.0
- **API 风格**：RESTful，统一响应格式 `{ code, msg, data }`

---

## 快速开始（Docker 一键部署）

本项目提供完整的 Docker Compose 编排，**无需本地安装 Node、Maven、MySQL**，一条命令即可启动全套服务。

### 前提条件

- [Docker](https://www.docker.com/products/docker-desktop)（建议 20.10+）
- [Docker Compose](https://docs.docker.com/compose/)（建议 2.0+）
- Git（用于克隆配置）

### 部署步骤

```bash
# 1. 克隆仓库
git clone https://github.com/zlzpic/plotassistant.git
cd plotassistant/deploy

# 2. 准备数据库初始化脚本（首次运行必须）
# 编辑 db/init.sql，填入你的建表语句与测试数据。
# 若使用 JPA 自动建表，可保留文件为空或仅插入测试数据。

# 3. 启动所有服务（后台运行）
docker-compose up -d

# 4. 等待约 20~30 秒，让 MySQL 完成首次初始化
# 查看启动日志：
docker-compose logs -f backend
```

### 访问服务

| 服务     | 地址                  | 说明           |
| -------- | --------------------- | -------------- |
| 前端页面 | http://localhost      | 剧情创作主界面 |
| 后端 API | http://localhost:8080 | RESTful 接口   |

### 停止与清理

```bash
# 停止服务（保留数据）
docker-compose down

# 彻底清理（包括数据库 volume，用于重新执行 init.sql）
docker-compose down -v
```

---

## 项目结构

```
plotassistant/
├── deploy/
│   ├── docker-compose.yml      # 全栈编排：MySQL + 后端 + 前端
│   └── db/
│       └── init.sql            # 数据库初始化脚本
├── backend/                    # Spring Boot 后端源码
│   ├── Dockerfile
│   ├── pom.xml
│   └── src/
├── frontend/                   # Vue 3 前端源码
│   ├── Dockerfile
│   ├── nginx.conf
│   ├── package.json
│   └── src/
└── README.md
```

---

## 开发说明

### 后端开发

```bash
cd backend
mvn clean package -DskipTests
# 生成的 jar 位于 target/ 目录，供 Docker 构建使用
```

### 前端开发

```bash
cd frontend
npm install
npm run dev
# 开发服务器运行于 http://localhost:4000
```

---

## 常见问题

### Q1: 首次启动后数据库表没有创建？

`init.sql` 仅在 MySQL **首次创建 volume** 时执行一次。如果你之前启动过，需要执行：

```bash
docker-compose down -v
docker-compose up -d
```

### Q2: 后端提示数据库连接失败？

请确认 `db/init.sql` 中已创建 `plotassistant` 数据库，且后端 `application.properties` 或 JPA 配置正确。

### Q3: 如何修改数据库密码？

编辑 `deploy/docker-compose.yml` 中 `mysql` 和 `backend` 的 `environment` 环境变量，确保两者一致，然后重启。

---

## Docker 镜像

本项目镜像托管于 **GitHub Container Registry**：

| 镜像 | 地址                                           |
| ---- | ---------------------------------------------- |
| 后端 | `ghcr.io/zlzpic/plotassistant-backend:latest`  |
| 前端 | `ghcr.io/zlzpic/plotassistant-frontend:latest` |

镜像由 GitHub Actions（或本地构建）自动推送，与源码版本保持一致。

---

## License

本项目基于 [MIT License](./LICENSE) 开源。
