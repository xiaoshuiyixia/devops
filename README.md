# Java DevOps 学习项目

独立运行的 Spring Boot 3.5.7 项目，使用 Java 21、Maven 和 JUnit 5。提供 `GET /hello`，返回 HTTP 200、`text/plain` 和 `Hello, DevOps!`。

另提供 `GET /greet?name=Alice`，返回 HTTP 200 和 JSON `{"message":"Hello, Alice!"}`。省略 `name` 时使用 `World`；名字两端空白会被去除，空字符串或纯空白名字返回 HTTP 400，支持中文名字。

## 本地环境

- JDK 21（不是只有 JRE），`JAVA_HOME` 指向 JDK 目录。
- Maven 3.9+；也可使用仓库中的 Maven Wrapper（固定为 3.9.11）。
- Docker Desktop 使用 Linux containers，且 Docker 引擎已启动。
- 首次构建需要联网下载 Maven 依赖和 Docker 基础镜像。

当前机器已安装 JDK 21，PowerShell 中可这样切换（只影响当前终端）：

```powershell
$env:JAVA_HOME = 'D:\develop\Java\jdk-21'
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
java -version
mvn -version
```

其他机器将 `JAVA_HOME` 替换为实际的 JDK 21 路径。Maven Enforcer 会在构建开始时检查 Java 和 Maven 版本，使用错误版本时给出提示。

## 构建和启动

在项目根目录执行：

```powershell
mvn clean verify
java -jar target/app.jar
```

也可以用 `mvn spring-boot:run` 启动。应用默认监听 `8080`，在另一个终端访问：

```powershell
curl.exe -i http://localhost:8080/hello
curl.exe -i "http://localhost:8080/greet?name=Alice"
```

没有全局 Maven 时，Windows 使用 `.\mvnw.cmd clean verify`，macOS/Linux 使用 `sh mvnw clean verify`。Wrapper 同样要求本机配置 JDK 21。

## 项目结构

```text
.
├── pom.xml                         # 依赖、Java 版本和构建插件
├── mvnw / mvnw.cmd                 # Maven Wrapper
├── .mvn/wrapper/                   # Maven 下载版本
├── src/main/java/com/example/devops/
│   ├── DevOpsApplication.java      # Spring Boot 启动入口
│   ├── HelloController.java        # GET /hello
│   └── GreetingController.java     # GET /greet?name=Alice
├── src/main/resources/
│   └── application.properties     # 应用名称
├── src/test/java/com/example/devops/
│   ├── HelloControllerTest.java    # 普通单元测试
│   ├── DevOpsApplicationTests.java # Spring 上下文启动测试
│   ├── HelloEndpointIT.java        # /hello 真实 HTTP 集成测试
│   └── GreetingEndpointIT.java     # /greet 参数和 JSON 响应测试
├── .github/workflows/ci.yml        # Maven CI 与 GHCR 镜像发布
├── Dockerfile                      # 多阶段镜像构建
└── .dockerignore                   # 仅发送构建所需的源码与 POM
```

`target/` 是生成目录，不提交到 Git，也不发送到 Docker 构建上下文。可执行 JAR 为 `target/app.jar`，包含应用代码、依赖和内嵌 Tomcat，无需另行安装应用服务器。

## 测试机制

所有测试使用 JUnit Jupiter（JUnit 5），由 `spring-boot-starter-test` 提供。

- Maven Surefire 在 `test` 阶段运行 `*Test` 和 `*Tests`：检查控制器返回值，以及 Spring 上下文能否启动。
- Maven Failsafe 在 `integration-test` 阶段运行 `*IT`，在 `verify` 阶段判断结果。`HelloEndpointIT` 启动完整应用和内嵌 Tomcat，使用随机空闲端口发送真实 HTTP 请求，检查 `/hello` 的状态码、响应体、内容类型，以及不存在路径的 404。
- `GreetingEndpointIT` 同样通过真实 HTTP 验证默认名字、自定义名字、去除两端空白、中文名字、JSON 内容类型和空白参数的 400 响应。加上监控集成测试后，项目共执行 14 项测试。
- `mvn clean verify` 依次清理旧产物、编译、执行单元测试、打包可执行 JAR、执行集成测试并检查结果；任何测试失败都会导致命令失败。
- `mvn test` 只执行前两类测试；完整验证请使用 `mvn clean verify`。报告位于 `target/surefire-reports/` 和 `target/failsafe-reports/`。

## Docker 镜像构建与运行

```powershell
docker build -t devops-learning:local .
docker run --rm -d --name devops-learning -p 8080:8080 devops-learning:local
curl.exe -i http://localhost:8080/hello
docker stop devops-learning
```

应用启动需要几秒钟，请在容器日志出现 `Started DevOpsApplication` 后访问接口；可使用 `docker logs devops-learning` 查看启动进度。

如果本地 Java 应用已占用 8080，可改用 `-p 8081:8080`，并访问 `http://localhost:8081/hello`。

构建过程：

1. Docker 根据 `.dockerignore` 仅发送 POM、源码等必要文件。
2. `build` 阶段使用带 JDK 21 的 Maven 3.9.9 Alpine 镜像，在容器内执行 `mvn clean verify`。测试通过后生成 `target/app.jar`，无需预先在宿主机打包。
3. `runtime` 阶段使用 Java 21 JRE 镜像，只从构建阶段复制 `app.jar`。Maven、测试源码和编译缓存不会进入最终镜像。
4. 容器以 UID/GID `10001:10001` 的非 root 用户执行 `java -jar /app/app.jar`。`EXPOSE 8080` 是端口声明；`docker run -p` 才会将端口映射到本机。

应用可独立运行，不依赖数据库或外部服务。

## 最小监控系统

本次检查：项目为 Java 21 / Spring Boot 3.5.7，Wrapper 固定 Maven 3.9.11，Docker 构建使用 Maven 3.9.9。原仓库没有 Compose、监控依赖或数据库配置。本次新增 Compose，复用原 Dockerfile 和 Compose 自动创建的默认网络，业务 Java 代码、Dockerfile、GitHub Actions 均不修改。

### 数据怎样流动

```text
浏览器 / curl → app:8080（业务请求）
                    ↓ Micrometer 在应用中记录 JVM、CPU、HTTP 指标
               app:8081/actuator/prometheus（Actuator 输出指标文本）
                    ↑ Prometheus 每 15 秒主动拉取
               prometheus:9090 → 数据写入 prometheus-data 卷
                    ↑ Grafana 使用 PromQL 查询
               grafana:3000 → 浏览器中的 Dashboard
```

Micrometer 是应用内的指标工具；Actuator 提供管理 HTTP 接口；Prometheus 负责定时采集和保存时间序列；Grafana 负责查询与展示。应用不会主动把指标推给 Grafana。`/actuator/health` 是应用健康检查，Prometheus 的 `up` 则表示最近一次指标抓取是否成功。

### 一键启动（PowerShell）

先启动 Docker Desktop 的 Linux 引擎，并停止其他占用本机 8080、9090、3000 的服务。在项目根目录执行：

```powershell
if (-not (Test-Path .env)) { Copy-Item .env.example .env }
# 使用编辑器打开 .env，给 GRAFANA_ADMIN_PASSWORD 设置自己的密码。
# .env 已被 Git 忽略；密码为空时 Compose 会拒绝启动。
docker compose config --quiet
docker compose up -d --build
docker compose ps
docker compose logs --tail=100 app prometheus grafana
```

无需安装本机 Maven 或 JDK即可通过 Dockerfile 构建；首次需要下载镜像与依赖。`depends_on` 只控制启动顺序，应用就绪前短暂显示 DOWN 属正常情况，Prometheus 会继续重试。修改 Prometheus 配置后执行 `docker compose restart prometheus`。

| 服务 | 本机地址 | 用途 |
| --- | --- | --- |
| Java 业务 | http://localhost:8080/hello | 产生 HTTP 请求 |
| Prometheus | http://localhost:9090/targets | 等待 job `devops-learning` 显示 UP |
| Grafana | http://localhost:3000 | 用户 `admin`，密码取自 `.env` |
| Dashboard | http://localhost:3000/d/java-overview | Java Monitoring 文件夹中的 Java Application Overview |

Grafana 自动加载 UID 为 `prometheus` 的默认数据源（URL 为 `http://prometheus:9090`）和六张图表：抓取状态、JVM 各内存池用量、进程/系统 CPU、HTTP 每秒请求数、平均响应时间、4xx/5xx 每秒请求数。容器里的 `localhost` 代表容器自身，所以数据源和 Target 使用 Compose 服务名。

启动后先发起业务请求，再等待至少两次抓取（约 30–60 秒）：

```powershell
1..100 | ForEach-Object { Invoke-RestMethod http://localhost:8080/hello | Out-Null }
Invoke-RestMethod 'http://localhost:9090/api/v1/query?query=up%7Bjob%3D%22devops-learning%22%7D'
```

也可以在 Prometheus 查询页面直接输入：

```promql
up{job="devops-learning"}
jvm_memory_used_bytes{job="devops-learning"}
process_cpu_usage{job="devops-learning"}
sum by (uri, status) (rate(http_server_requests_seconds_count{job="devops-learning",uri!~"/actuator.*"}[1m]))
```

Grafana 可在 Explore 中选择 Prometheus 运行同样的查询，再查看 Dashboard。HTTP 指标在相应请求完成后才出现；`rate` 至少需要两个样本。没有流量时平均耗时没有有效值，4xx/5xx 尚未发生时对应图表可能显示 No data，不能据此认定采集失败。CPU 为 JVM 报告的进程/系统比例，范围 0–1；系统范围取决于 JVM 对容器的识别，不等于容器 CPU 配额利用率。

### 验证 Actuator 与隔离

默认本机 `java -jar target/app.jar` 时，管理端口只绑定 `127.0.0.1:8081`：

```powershell
curl.exe -i http://127.0.0.1:8081/actuator/health
curl.exe -s http://127.0.0.1:8081/actuator/prometheus
```

Compose 中，8081 只在容器网络内监听，没有发布到宿主机。可用 Java 运行镜像自带的 BusyBox wget 从应用容器内部验证：

```powershell
docker compose exec app wget -qO- http://127.0.0.1:8081/actuator/health
docker compose exec app wget -qO- http://127.0.0.1:8081/actuator/prometheus
curl.exe -i http://localhost:8080/actuator/prometheus
```

前两个预期分别为 `{"status":"UP"}` 和包含 `jvm_memory_used_bytes`、`process_cpu_usage` 的文本；最后一个预期 404，证明业务端口不提供管理接口。不要为了在浏览器中查看指标而给 Compose 添加 `8081:8081` 映射。

本学习配置仅暴露 health/prometheus，不返回健康详情；8080、9090、3000 的宿主机映射均绑定 `127.0.0.1`。Prometheus 和 Actuator 没有额外认证，只适用于本机和可信 Docker 网络，勿通过公网反向代理转发这些管理端口。在远程开发机使用时通过 SSH 本地端口转发访问 Grafana / Prometheus。

### 持久化与停止

Prometheus 指标写入 `prometheus-data` 命名卷，保留 15 天或达到 1GB 留存阈值后清理旧数据（阈值不代表整个卷的硬磁盘上限）。Grafana 用户、设置及数据库写入 `grafana-data`；数据源和 Dashboard 由仓库文件自动配置。

```powershell
docker compose down
docker compose up -d
```

以上停止/重建会保留命名卷和历史数据。`docker compose down -v` 会删除监控数据，仅在主动重置学习环境时使用。`.env` 中的 Grafana 管理员密码仅在首次初始化数据库时生效；后续请在 Grafana 用户设置中修改。Dashboard 的配置文件为准，修改 JSON 后等待自动刷新；不要依赖 UI 保存覆盖仓库版本。

### 三个动手实验

1. **区分累计计数与速率**：运行下方循环约一分钟，观察 Dashboard 的请求速率与延迟。到 Prometheus 分别查询 `http_server_requests_seconds_count{uri="/hello"}` 和 `rate(http_server_requests_seconds_count{uri="/hello"}[1m])`。前者累计增长，停止请求后保持不变；后者随着流量停止逐渐归零。JVM 内存和 CPU 也可能变化，但轻量接口的 CPU 变化可能很小。

   ```powershell
   1..300 | ForEach-Object {
       Invoke-RestMethod http://localhost:8080/hello | Out-Null
       Start-Sleep -Milliseconds 200
   }
   ```

2. **观察 HTTP 标签**：执行 `1..30 | ForEach-Object { curl.exe -s -o NUL 'http://localhost:8080/greet?name=' }`，等待约 30 秒后查看 4xx 图表，再查询 `http_server_requests_seconds_count{uri="/greet",status="400"}`。对比正常的 `/greet?name=Alice`，理解 `uri`、`status` 标签如何分组。不同 name 不会成为新的 URI 标签，避免每个名字生成一条时间序列。

3. **区分抓取失败、进程重启与数据存储**：执行 `docker compose stop app`，等待 15–30 秒，在 Targets 和 Grafana 观察 `up=0`，历史曲线依然存在。执行 `docker compose start app`，等待恢复 UP，HTTP 累计计数从新进程重新开始。最后执行 `docker compose down` 再 `docker compose up -d`，确认 Prometheus 历史曲线仍可查询、Grafana 登录与面板仍存在。不要加 `-v`。

### 新增/修改文件

| 文件 | 作用 |
| --- | --- |
| `pom.xml` | Actuator 与 Micrometer Prometheus registry，版本由 Boot 管理 |
| `src/main/resources/application.properties` | 独立管理端口、端点白名单、应用指标标签 |
| `compose.yaml` | 三个服务、默认网络、本机端口绑定、持久卷 |
| `.env.example`、`.gitignore` | 本地 Grafana 密码模板与忽略规则 |
| `monitoring/prometheus/prometheus.yml` | 每 15 秒采集 `app:8081/actuator/prometheus` |
| `monitoring/grafana/provisioning/datasources/prometheus.yml` | 自动配置 Prometheus 数据源 |
| `monitoring/grafana/provisioning/dashboards/default.yml` | 自动加载面板 |
| `monitoring/grafana/dashboards/java-overview.json` | JVM、CPU、HTTP 等六张基础图表 |
| `src/test/java/com/example/devops/MonitoringEndpointIT.java` | 真实 HTTP 验证健康、指标及管理端口隔离 |
| `README.md` | 数据流、启动、验证、持久化、实验说明 |

新增集成测试仍由现有 Failsafe 的 `mvn clean verify` 自动执行，GitHub Actions 和 Docker 镜像构建流程不变。官方参考：[Spring Boot 指标](https://docs.spring.io/spring-boot/3.5/reference/actuator/metrics.html)、[管理端口](https://docs.spring.io/spring-boot/3.5/reference/actuator/monitoring.html)、[Prometheus 存储](https://prometheus.io/docs/prometheus/latest/storage/)、[Grafana 自动配置](https://grafana.com/docs/grafana/latest/administration/provisioning/)。

### 本次监控验证结果（2026-11-08）

- 本机 JDK 21.0.5 / Maven 3.9.11 执行完整 `clean verify` 成功：14 项测试，0 失败、0 错误、0 跳过。集成测试实际启动 Tomcat，验证 health 返回 UP、Prometheus 输出 JVM/CPU/HTTP 指标、业务端口无法访问 Actuator，以及 env 端点未暴露。测试类使用 `@AutoConfigureObservability` 显式启用测试环境中的指标导出。
- `docker compose config --quiet`、Dashboard JSON 解析、`git diff --check` 通过；Prometheus 镜像内的 `promtool check config` 通过。
- Prometheus 3.13.5 / Grafana 12.3.11 容器启动成功；Grafana API 确认数据库健康、Prometheus 数据源查询成功、六个面板自动加载。所有七条面板 PromQL 均经真实 Prometheus 执行，语法有效。
- **尚未验证完整容器链路**：两次应用镜像构建均因 Docker Hub `auth.docker.io` 连接超时，无法下载原 Dockerfile 的 Java 基础镜像。应用容器尚未启动，当前 `app:8081` Target 为 DOWN；不能宣称 Target UP、真实指标曲线展示或容器重建后的数据保留已验证。GitHub 远端 Actions 本次未触发。
- 本次已启动两个监控容器，并生成被 Git 忽略的 `.env` 随机密码。可在本地编辑器查看密码，以 `admin` 登录 Grafana。镜像下载恢复后执行 `docker compose up -d --build` 完成应用构建和启动；如需停止当前监控容器，执行 `docker compose down` 保留数据卷。

## GitHub Actions 与 GHCR

- 推送到 `master`、创建或更新目标为 `master` 的 PR 时运行 `verify`，也支持通过 Actions 页面的 `Run workflow` 手动验证。
- `verify` 使用 Java 21 执行 `mvn clean verify`，并尝试上传 Surefire / Failsafe 测试报告。在本次运行的 `Summary` 页面的 `Artifacts` 中下载 `test-reports`。
- 仅在推送到 `master` 且 `verify` 成功后运行 `docker`，构建镜像并通过自动提供的 `GITHUB_TOKEN` 发布到 GHCR。PR 和手动验证不发布镜像。
- 镜像名称是小写的 `ghcr.io/<仓库所有者>/<仓库名称>`，标签为 `latest` 和完整的提交 SHA。发布任务具备 `packages: write` 权限。

## 已完成的本地验证

2026-11-06 在 Windows / JDK 21 / Docker Desktop Linux 引擎上实际验证：

- 本机 `mvn clean verify` 成功：11 项测试，0 失败、0 错误、0 跳过。
- `.\mvnw.cmd --version` 成功，使用 Maven 3.9.11 和 Java 21。
- `docker build -t devops-learning:local .` 成功，镜像构建阶段也完整执行了 11 项测试。
- 启动最终镜像并通过本机映射端口请求 `/hello`，得到 HTTP 200、`text/plain;charset=UTF-8` 和 `Hello, DevOps!`。
- 新接口通过最终镜像的本机 `18080:8080` 映射端口验证：`GET /greet?name=Alice` 成功返回 `{"message":"Hello, Alice!"}`。
- 容器内 `id` 确认运行用户为 `uid=10001 gid=10001`；验证用容器已停止。
