# Java DevOps 学习项目

独立运行的 Spring Boot 3.5.7 项目，使用 Java 21、Maven 和 JUnit 5。提供 `GET /hello`，返回 HTTP 200、`text/plain` 和 `Hello, DevOps!`。

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
│   └── HelloController.java        # GET /hello
├── src/main/resources/
│   └── application.properties     # 应用名称
├── src/test/java/com/example/devops/
│   ├── HelloControllerTest.java    # 普通单元测试
│   ├── DevOpsApplicationTests.java # Spring 上下文启动测试
│   └── HelloEndpointIT.java        # 真实 HTTP 集成测试
├── Dockerfile                      # 多阶段镜像构建
└── .dockerignore                   # 仅发送构建所需的源码与 POM
```

`target/` 是生成目录，不提交到 Git，也不发送到 Docker 构建上下文。可执行 JAR 为 `target/app.jar`，包含应用代码、依赖和内嵌 Tomcat，无需另行安装应用服务器。

## 测试机制

所有测试使用 JUnit Jupiter（JUnit 5），由 `spring-boot-starter-test` 提供。

- Maven Surefire 在 `test` 阶段运行 `*Test` 和 `*Tests`：检查控制器返回值，以及 Spring 上下文能否启动。
- Maven Failsafe 在 `integration-test` 阶段运行 `*IT`，在 `verify` 阶段判断结果。`HelloEndpointIT` 启动完整应用和内嵌 Tomcat，使用随机空闲端口发送真实 HTTP 请求，检查 `/hello` 的状态码、响应体、内容类型，以及不存在路径的 404。
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

本阶段只包含本地应用、测试和 Docker 构建，不依赖数据库或外部服务。

## 已完成的本地验证

2026-11-06 在 Windows / JDK 21 / Docker Desktop Linux 引擎上实际验证：

- 本机 `mvn clean verify` 成功：4 项测试，0 失败、0 错误、0 跳过。
- `.\mvnw.cmd --version` 成功，使用 Maven 3.9.11 和 Java 21。
- `docker build -t devops-learning:local .` 成功，镜像构建阶段也完整执行了 4 项测试。
- 启动最终镜像并通过本机映射端口请求 `/hello`，得到 HTTP 200、`text/plain;charset=UTF-8` 和 `Hello, DevOps!`。
- 容器内 `id` 确认运行用户为 `uid=10001 gid=10001`；验证用容器已停止。
