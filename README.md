# 在线考试系统

一个基于 Spring Boot 3、Vue 3、MySQL 的在线考试系统，包含管理员端、教师端和学生端。项目目前处于课程设计/毕业设计原型阶段，核心考试流程、题库、课程班、阅卷、成绩和切屏检测已经完成，适合继续扩展为更完整的教学考试平台。
<img width="2879" height="1465" alt="屏幕截图 2026-05-30 215124" src="https://github.com/user-attachments/assets/049a5090-3d63-481c-bd44-711999c57881" />
<img width="2879" height="1465" alt="屏幕截图 2026-05-30 215143" src="https://github.com/user-attachments/assets/81a193b3-6401-4f56-a17e-63551ece4be1" />
<img width="2879" height="1473" alt="屏幕截图 2026-05-30 215217" src="https://github.com/user-attachments/assets/c73a1d7f-34ff-4320-8767-cf791922ca80" />

## 技术栈

- 后端：Spring Boot 3.3.5、Spring Web、Spring Data JPA、Validation
- 数据库：MySQL 8.x
- 前端：Vue 3、Vite
- 认证：登录后签发 HMAC-SHA256 JWT，前端通过 `Authorization: Bearer <token>` 访问接口
- 构建环境：JDK 17、Maven、Node.js、npm

## 当前开发情况

已完成：

- 管理员端：教师管理、科目管理、教师授课科目分配、行政班级管理、学生管理、成绩修改。
- 教师端：题库管理、单选题/填空题/简答题维护、考试发布、截止时间设置、课程班级管理、按行政班批量导入学生、单个学生加入课程班、成绩查看与修改、主观题当前页阅卷。
- 学生端：学号密码登录、查看可参加考试、随机抽题开始考试、单选/填空自动判分、简答题待教师阅卷、提交试卷、考试中切屏检测。
- 随机抽题：后端按教师和科目从题库中使用 MySQL `ORDER BY RAND() LIMIT n` 随机生成试卷。
- 切屏检测：前端监听 `document.visibilityState`、`visibilitychange` 和 `window.blur`，后端记录切屏次数；前端已做节流，避免高频事件影响考试。
- UI：管理员、教师、学生端均已改为标签页/工作台式布局；提醒统一为页面内 toast 或系统内弹层，不使用浏览器 `alert/prompt/confirm`。
- 演示数据：启动时会自动生成示例行政班、教师、学生、Java 课程班和若干试题，方便本地测试。

仍建议继续完善：

- 密码目前为明文存储，仅适合学习和演示；正式部署前应改为 BCrypt 等安全哈希。
- 登录账号密码传输应配合 HTTPS；JWT 是登录后的访问凭证，不等同于加密账号密码。
- 主观题阅卷、成绩修改、考试重考等流程可继续增加审计日志。
- 可增加分页、搜索、导入 Excel、考试倒计时、交卷防重复提交等增强功能。

## 目录结构

```text
.
├── frontend/                 # Vue 3 + Vite 前端
├── sql/                      # 数据库辅助 SQL
├── src/main/java/com/exam/   # Spring Boot 后端源码
├── src/main/resources/       # Spring Boot 配置和初始化 SQL
├── pom.xml
└── README.md
```

## 本地启动

### 1. 准备数据库

创建 MySQL 数据库用户后，设置环境变量或直接修改 `src/main/resources/application.yml` 中的占位符。

PowerShell 示例：

```powershell
$env:DB_URL="jdbc:mysql://localhost:3306/online_exam?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&createDatabaseIfNotExist=true"
$env:DB_USERNAME="<MYSQL_USERNAME>"
$env:DB_PASSWORD="<MYSQL_PASSWORD>"
$env:JWT_SECRET="<LONG_RANDOM_JWT_SECRET>"
$env:APP_CORS_ALLOWED_ORIGINS="http://localhost:5173,http://127.0.0.1:5173"
```

### 2. 启动后端

```powershell
mvn clean compile spring-boot:run
```

如果没有配置 Maven 命令，也可以用 IntelliJ IDEA 打开项目，运行：

```text
com.exam.OnlineExamApplication
```

### 3. 启动前端

```powershell
cd frontend
npm install
npm run dev
```

前端默认访问：

```text
http://localhost:5173
```

如果后端地址不是 `http://localhost:8080/api`，复制 `frontend/.env.example` 为 `frontend/.env.local`，修改：

```env
VITE_API_BASE_URL=<BACKEND_API_BASE_URL>
```

## 默认测试账号

应用初始化后会生成以下演示账号：

```text
管理员：admin / admin123
教师：teacher01 / 123456
学生：20260001 / 123456
```

正式部署时请删除或修改这些默认账号。管理员账号来自 `src/main/resources/data.sql`，演示教师、学生和题库来自 `src/main/java/com/exam/config/DataSeeder.java`。

## 部署时必须替换的配置

为了避免泄露本机隐私信息，仓库中的敏感配置都使用占位符或环境变量。部署者需要替换：

| 配置项 | 说明 |
| --- | --- |
| `DB_URL` | MySQL JDBC 地址，例如生产库地址和库名 |
| `DB_USERNAME` | MySQL 用户名 |
| `DB_PASSWORD` | MySQL 密码 |
| `JWT_SECRET` | JWT 签名密钥，必须替换为足够长的随机字符串 |
| `APP_CORS_ALLOWED_ORIGINS` | 允许访问后端的前端域名，多个地址用英文逗号分隔 |
| `VITE_API_BASE_URL` | 前端请求后端 API 的基础地址 |
| 默认管理员密码 | 修改 `data.sql` 或上线后立即在数据库/后台中修改 |
| 演示数据 | 不需要演示数据时删除或关闭 `DataSeeder` |

当前 `application.yml` 已不包含个人数据库密码。

## 数据库说明

后端使用 JPA 自动建表：

```yaml
spring:
  jpa:
    hibernate:
      ddl-auto: update
```

辅助 SQL：

- `sql/init.sql`：创建 `online_exam` 数据库。
- `sql/cleanup_duplicate_students.sql`：用于清理重复学生数据的维护脚本。
- `src/main/resources/data.sql`：初始化管理员账号和行政班数据。

## 常见问题

### 页面空白

先确认前端依赖已安装并重新启动：

```powershell
cd frontend
npm install
npm run dev
```

再检查浏览器控制台和 `VITE_API_BASE_URL` 是否指向正确后端。

### 后端启动失败，提示数据库连接错误

检查 `DB_URL`、`DB_USERNAME`、`DB_PASSWORD` 是否正确，并确认 MySQL 服务已启动。

### 前端提示未登录或请求失败

确认后端已启动，且 `APP_CORS_ALLOWED_ORIGINS` 包含当前前端地址。

## 安全提醒

本项目当前定位为学习和演示项目。上传 GitHub 前请确认：

- 不要提交真实数据库密码、服务器地址、私钥、生产 JWT 密钥。
- 不要提交本地 `.env`、`.env.local`、`frontend/.env.local`。
- 生产环境请启用 HTTPS。
- 生产环境请将用户密码改为哈希存储。
- 生产环境请修改或删除默认演示账号。
