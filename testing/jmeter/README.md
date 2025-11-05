# JMeter 性能测试指南

## 📦 测试文件

- **UserProfile-Performance-Test.jmx** - 性能测试脚本

## 🎯 测试目标

### 性能指标
- **并发用户数**: 100 (可配置)
- **响应时间 P95**: < 500ms
- **错误率**: < 1%
- **吞吐量 (TPS)**: > 200

### 测试场景

| 场景 | 流量占比 | 说明 |
|-----|---------|------|
| 用户查询操作 | 70% | 查询用户信息、用户画像 |
| 用户画像计算 | 20% | 触发画像计算(耗时操作) |
| 推荐查询 | 10% | 获取个性化推荐 |

---

## 🚀 快速开始

### 1. 安装JMeter

#### 方式一: 官网下载
1. 访问 https://jmeter.apache.org/download_jmeter.cgi
2. 下载最新版本 (推荐 5.6.3+)
3. 解压到任意目录

#### 方式二: 包管理器安装

**macOS (Homebrew)**:
```bash
brew install jmeter
```

**Ubuntu/Debian**:
```bash
sudo apt update
sudo apt install jmeter
```

**Windows (Chocolatey)**:
```bash
choco install jmeter
```

### 2. 验证安装

```bash
jmeter --version
# 输出: Apache JMeter 5.6.3
```

### 3. 启动后端服务

确保所有服务已启动:

```bash
cd deployment
docker-compose up -d

# 验证服务状态
docker-compose ps
curl http://localhost:8080/actuator/health
```

### 4. 运行性能测试

#### GUI模式 (开发调试)

```bash
# 打开JMeter GUI
jmeter -t testing/jmeter/UserProfile-Performance-Test.jmx

# 或在Windows上双击 jmeter.bat
```

**操作步骤**:
1. 点击绿色的 ▶️ 按钮启动测试
2. 查看实时结果(Summary Report, Aggregate Report, Graph Results)
3. 点击红色的 ⏹️ 按钮停止测试

#### 命令行模式 (性能测试)

**⚠️ 重要**: 实际性能测试必须使用命令行模式,GUI模式会严重影响性能!

```bash
# 基础运行(使用默认参数)
jmeter -n -t testing/jmeter/UserProfile-Performance-Test.jmx \
  -l results/result.jtl \
  -e -o results/html-report

# 自定义参数运行
jmeter -n -t testing/jmeter/UserProfile-Performance-Test.jmx \
  -Jbase_url=localhost \
  -Jbase_port=8080 \
  -Jthreads=200 \
  -Jramp_time=120 \
  -Jduration=600 \
  -l results/result-200users.jtl \
  -e -o results/html-report-200users
```

**参数说明**:
- `-n`: 非GUI模式
- `-t`: 测试脚本路径
- `-l`: 结果文件路径 (.jtl格式)
- `-e`: 生成HTML报告
- `-o`: HTML报告输出目录
- `-J`: 设置JMeter属性(覆盖默认值)

---

## ⚙️ 配置参数

### 可调整参数

| 参数 | 说明 | 默认值 | 调整方式 |
|-----|------|-------|---------|
| `base_url` | API服务器地址 | `localhost` | `-Jbase_url=192.168.1.100` |
| `base_port` | API服务器端口 | `8080` | `-Jbase_port=8080` |
| `threads` | 并发用户数 | `100` | `-Jthreads=200` |
| `ramp_time` | 启动时间(秒) | `60` | `-Jramp_time=120` |
| `duration` | 测试时长(秒) | `300` | `-Jduration=600` |

### 示例配置

#### 轻量级测试 (冒烟测试)
```bash
jmeter -n -t UserProfile-Performance-Test.jmx \
  -Jthreads=10 \
  -Jramp_time=10 \
  -Jduration=60 \
  -l results/smoke-test.jtl \
  -e -o results/smoke-test-report
```

#### 中等负载测试
```bash
jmeter -n -t UserProfile-Performance-Test.jmx \
  -Jthreads=100 \
  -Jramp_time=60 \
  -Jduration=300 \
  -l results/medium-load.jtl \
  -e -o results/medium-load-report
```

#### 高负载压力测试
```bash
jmeter -n -t UserProfile-Performance-Test.jmx \
  -Jthreads=500 \
  -Jramp_time=180 \
  -Jduration=600 \
  -l results/stress-test.jtl \
  -e -o results/stress-test-report
```

#### 峰值测试 (Spike Test)
```bash
jmeter -n -t UserProfile-Performance-Test.jmx \
  -Jthreads=1000 \
  -Jramp_time=30 \
  -Jduration=120 \
  -l results/spike-test.jtl \
  -e -o results/spike-test-report
```

#### 持久性测试 (Endurance Test)
```bash
jmeter -n -t UserProfile-Performance-Test.jmx \
  -Jthreads=100 \
  -Jramp_time=60 \
  -Jduration=3600 \
  -l results/endurance-test.jtl \
  -e -o results/endurance-test-report
```

---

## 📊 结果分析

### 1. HTML报告

运行测试后,自动生成HTML报告:

```bash
# 在浏览器中打开报告
# Windows
start results/html-report/index.html

# macOS
open results/html-report/index.html

# Linux
xdg-open results/html-report/index.html
```

**报告包含**:
- **Dashboard**: 总览(TPS、响应时间、错误率)
- **APDEX**: 应用性能指数
- **Statistics**: 详细统计(Min、Max、Avg、P50、P90、P95、P99)
- **Errors**: 错误分析
- **Over Time**: 时间序列图表
- **Throughput**: 吞吐量分析
- **Response Times**: 响应时间分布

### 2. JTL结果文件分析

JTL文件是CSV格式,可以用Excel或其他工具分析:

```bash
# 查看前10行
head -10 results/result.jtl

# 统计错误数
grep "false" results/result.jtl | wc -l

# 提取响应时间列
cut -d',' -f2 results/result.jtl | tail -n +2 | sort -n
```

### 3. 关键指标解读

#### 响应时间指标

| 指标 | 说明 | 目标 |
|-----|------|------|
| Min | 最小响应时间 | - |
| Max | 最大响应时间 | < 5000ms |
| Avg | 平均响应时间 | < 300ms |
| P50 (Median) | 50%的请求响应时间 | < 200ms |
| P90 | 90%的请求响应时间 | < 400ms |
| P95 | 95%的请求响应时间 | < 500ms |
| P99 | 99%的请求响应时间 | < 1000ms |

#### 吞吐量指标

| 指标 | 说明 | 目标 |
|-----|------|------|
| TPS | 每秒事务数 | > 200 |
| Throughput | 吞吐量 (KB/s) | - |

#### 错误率指标

| 指标 | 说明 | 目标 |
|-----|------|------|
| Error % | 错误率 | < 1% |
| Samples | 总请求数 | - |
| KO | 失败请求数 | < 1% of Samples |

#### APDEX指标

APDEX (Application Performance Index) 应用性能指数:
- **满意**: 响应时间 ≤ T (默认 500ms)
- **可容忍**: T < 响应时间 ≤ 4T (2000ms)
- **不满意**: 响应时间 > 4T

**APDEX评分**:
- 0.94 - 1.00: 优秀
- 0.85 - 0.93: 良好
- 0.70 - 0.84: 一般
- 0.50 - 0.69: 较差
- < 0.50: 很差

### 4. 性能问题识别

#### 响应时间过长
**现象**: P95 > 1000ms

**可能原因**:
- 数据库慢查询
- 缺少缓存或缓存失效
- 资源不足(CPU/内存)
- 网络延迟

**排查方法**:
```bash
# 查看服务资源使用
docker stats

# 查看数据库慢查询
docker exec -it postgres psql -U userprofile -c "
SELECT query, mean_exec_time, calls
FROM pg_stat_statements
ORDER BY mean_exec_time DESC
LIMIT 10;"

# 查看应用日志
docker logs profile-service --tail=100 | grep "slow"

# 查看Grafana监控
open http://localhost:3000
```

#### 错误率过高
**现象**: Error % > 5%

**可能原因**:
- 服务崩溃或重启
- 数据库连接池耗尽
- 内存溢出
- 业务逻辑Bug

**排查方法**:
```bash
# 查看JMeter错误详情
grep "false" results/result.jtl | head -20

# 查看服务日志
docker logs user-service --tail=100 | grep "ERROR"

# 查看Pod状态(Kubernetes)
kubectl get pods
kubectl describe pod <pod-name>
```

#### 吞吐量低
**现象**: TPS < 100

**可能原因**:
- 服务实例数不足
- 资源瓶颈(CPU/内存/网络)
- 同步阻塞操作过多
- 数据库连接数不足

**排查方法**:
```bash
# 扩容服务
docker-compose up -d --scale user-service=3

# 调整数据库连接池
# backend/user-service/src/main/resources/application.yml
spring:
  datasource:
    hikari:
      maximum-pool-size: 30

# 查看JVM线程状态
docker exec -it user-service jstack 1
```

---

## 🔧 高级配置

### 1. 与InfluxDB集成(实时监控)

#### 启动InfluxDB和Grafana

```bash
# docker-compose.yml
services:
  influxdb:
    image: influxdb:1.8
    ports:
      - "8086:8086"
    environment:
      - INFLUXDB_DB=jmeter
      - INFLUXDB_ADMIN_USER=admin
      - INFLUXDB_ADMIN_PASSWORD=admin123
    volumes:
      - influxdb-data:/var/lib/influxdb

  grafana:
    image: grafana/grafana:latest
    ports:
      - "3001:3000"
    environment:
      - GF_SECURITY_ADMIN_PASSWORD=admin
    depends_on:
      - influxdb
```

#### 启动服务
```bash
docker-compose up -d influxdb grafana
```

#### 配置JMeter

在JMeter脚本中启用 **Backend Listener - InfluxDB**:
1. 打开 `UserProfile-Performance-Test.jmx`
2. 找到 "Backend Listener - InfluxDB"
3. 右键 -> Enable
4. 修改 `influxdbUrl`: `http://localhost:8086/write?db=jmeter`

#### 配置Grafana

1. 访问 http://localhost:3001 (admin/admin)
2. 添加InfluxDB数据源:
   - URL: http://influxdb:8086
   - Database: jmeter
   - User: admin
   - Password: admin123
3. 导入JMeter Dashboard (Dashboard ID: 4026)

### 2. 分布式测试(多台机器)

#### Master节点

```bash
# 启动JMeter Server
jmeter-server -Djava.rmi.server.hostname=192.168.1.100

# 运行分布式测试
jmeter -n -t UserProfile-Performance-Test.jmx \
  -R 192.168.1.101,192.168.1.102,192.168.1.103 \
  -l results/distributed-test.jtl \
  -e -o results/distributed-report
```

#### Slave节点

```bash
# 在每台Slave机器上启动JMeter Server
jmeter-server -Djava.rmi.server.hostname=192.168.1.101
```

### 3. 参数化测试数据

#### 创建CSV文件 (users.csv)

```csv
username,password
user1,Pass@123
user2,Pass@456
user3,Pass@789
```

#### 在JMeter中添加CSV Data Set Config

1. 右键Thread Group -> Add -> Config Element -> CSV Data Set Config
2. 配置:
   - Filename: `users.csv`
   - Variable Names: `username,password`
   - Recycle: `True`
   - Stop thread on EOF: `False`
3. 在请求中使用: `${username}`, `${password}`

### 4. 自定义Java请求

创建自定义Sampler:

```java
// CustomUserProfileSampler.java
package com.userprofile.jmeter;

import org.apache.jmeter.samplers.AbstractSampler;
import org.apache.jmeter.samplers.Entry;
import org.apache.jmeter.samplers.SampleResult;

public class CustomUserProfileSampler extends AbstractSampler {
    @Override
    public SampleResult sample(Entry entry) {
        SampleResult result = new SampleResult();
        result.sampleStart();

        try {
            // 自定义逻辑
            result.setSuccessful(true);
            result.setResponseCode("200");
            result.setResponseMessage("OK");
        } catch (Exception e) {
            result.setSuccessful(false);
            result.setResponseCode("500");
            result.setResponseMessage(e.getMessage());
        }

        result.sampleEnd();
        return result;
    }
}
```

编译并放入 `lib/ext/` 目录。

---

## 📈 性能测试最佳实践

### 1. 测试前准备

#### 环境准备
- [ ] 所有服务启动完成
- [ ] 数据库已预填充数据
- [ ] 缓存已预热
- [ ] 监控系统已启动(Prometheus + Grafana)

#### 数据准备
```bash
# 创建测试用户
for i in {1..1000}; do
  curl -X POST http://localhost:8080/api/users \
    -H "Authorization: Bearer $TOKEN" \
    -H "Content-Type: application/json" \
    -d "{\"username\":\"user$i\",\"password\":\"Pass@123\",\"name\":\"User $i\",\"email\":\"user$i@example.com\"}"
done

# 计算用户画像
for i in {1..1000}; do
  curl -X POST http://localhost:8080/api/profiles/$i/calculate \
    -H "Authorization: Bearer $TOKEN"
done
```

#### 系统检查
```bash
# 检查资源使用
docker stats --no-stream

# 检查连接数
docker exec -it postgres psql -U userprofile -c "SELECT count(*) FROM pg_stat_activity;"

# 检查Redis内存
docker exec -it redis redis-cli INFO memory
```

### 2. 测试策略

#### 测试金字塔

```
        ┌───────────────┐
        │  Spike Test   │  峰值测试(1000用户, 2分钟)
        │   (Very High) │
        └───────────────┘
       ┌─────────────────┐
       │  Stress Test    │  压力测试(500用户, 10分钟)
       │      (High)     │
       └─────────────────┘
      ┌───────────────────┐
      │   Load Test       │  负载测试(100用户, 5分钟)
      │     (Medium)      │
      └───────────────────┘
     ┌─────────────────────┐
     │   Smoke Test        │  冒烟测试(10用户, 1分钟)
     │      (Low)          │
     └─────────────────────┘
```

#### 测试流程

1. **冒烟测试** (Smoke Test)
   - 目的: 验证基本功能
   - 参数: 10用户, 1分钟
   - 预期: 0%错误率

2. **负载测试** (Load Test)
   - 目的: 验证正常负载下的性能
   - 参数: 100用户, 5分钟
   - 预期: P95 < 500ms, 错误率 < 1%

3. **压力测试** (Stress Test)
   - 目的: 找到系统承受极限
   - 参数: 从100逐步增加到500用户
   - 预期: 找到临界点

4. **峰值测试** (Spike Test)
   - 目的: 验证突发流量处理能力
   - 参数: 瞬间升至1000用户
   - 预期: 系统不崩溃,能自动恢复

5. **持久性测试** (Endurance Test)
   - 目的: 检查内存泄漏等长期问题
   - 参数: 100用户, 持续1-24小时
   - 预期: 性能不下降,无内存泄漏

### 3. 测试执行

#### 自动化脚本 (run-performance-tests.sh)

```bash
#!/bin/bash

# 性能测试自动化脚本

BASE_DIR="testing/jmeter"
RESULTS_DIR="results/$(date +%Y%m%d_%H%M%S)"
mkdir -p "$RESULTS_DIR"

echo "======================================"
echo "User Profile System Performance Test"
echo "Start Time: $(date)"
echo "======================================"

# 1. 冒烟测试
echo "[1/5] Running Smoke Test..."
jmeter -n -t "$BASE_DIR/UserProfile-Performance-Test.jmx" \
  -Jthreads=10 -Jramp_time=10 -Jduration=60 \
  -l "$RESULTS_DIR/smoke-test.jtl" \
  -e -o "$RESULTS_DIR/smoke-test-report"

# 检查结果
ERROR_RATE=$(awk -F',' 'NR>1 {total++; if($8=="false") errors++} END {print errors/total*100}' "$RESULTS_DIR/smoke-test.jtl")
if (( $(echo "$ERROR_RATE > 0" | bc -l) )); then
    echo "❌ Smoke Test FAILED: Error rate $ERROR_RATE%"
    exit 1
fi
echo "✅ Smoke Test PASSED"
sleep 30

# 2. 负载测试
echo "[2/5] Running Load Test..."
jmeter -n -t "$BASE_DIR/UserProfile-Performance-Test.jmx" \
  -Jthreads=100 -Jramp_time=60 -Jduration=300 \
  -l "$RESULTS_DIR/load-test.jtl" \
  -e -o "$RESULTS_DIR/load-test-report"
echo "✅ Load Test COMPLETED"
sleep 60

# 3. 压力测试
echo "[3/5] Running Stress Test..."
jmeter -n -t "$BASE_DIR/UserProfile-Performance-Test.jmx" \
  -Jthreads=500 -Jramp_time=180 -Jduration=600 \
  -l "$RESULTS_DIR/stress-test.jtl" \
  -e -o "$RESULTS_DIR/stress-test-report"
echo "✅ Stress Test COMPLETED"
sleep 120

# 4. 峰值测试
echo "[4/5] Running Spike Test..."
jmeter -n -t "$BASE_DIR/UserProfile-Performance-Test.jmx" \
  -Jthreads=1000 -Jramp_time=30 -Jduration=120 \
  -l "$RESULTS_DIR/spike-test.jtl" \
  -e -o "$RESULTS_DIR/spike-test-report"
echo "✅ Spike Test COMPLETED"
sleep 120

# 5. 持久性测试(可选)
read -p "Run Endurance Test (1 hour)? [y/N] " -n 1 -r
echo
if [[ $REPLY =~ ^[Yy]$ ]]; then
    echo "[5/5] Running Endurance Test..."
    jmeter -n -t "$BASE_DIR/UserProfile-Performance-Test.jmx" \
      -Jthreads=100 -Jramp_time=60 -Jduration=3600 \
      -l "$RESULTS_DIR/endurance-test.jtl" \
      -e -o "$RESULTS_DIR/endurance-test-report"
    echo "✅ Endurance Test COMPLETED"
fi

echo "======================================"
echo "All Tests Completed!"
echo "Results saved to: $RESULTS_DIR"
echo "End Time: $(date)"
echo "======================================"

# 打开报告
open "$RESULTS_DIR/load-test-report/index.html"
```

#### 运行脚本

```bash
chmod +x run-performance-tests.sh
./run-performance-tests.sh
```

### 4. 测试后清理

```bash
# 重启服务(清理状态)
docker-compose restart

# 清理数据库(可选)
docker exec -it postgres psql -U userprofile -c "TRUNCATE TABLE users CASCADE;"

# 清理Redis缓存
docker exec -it redis redis-cli FLUSHALL

# 清理MongoDB
docker exec -it mongodb mongosh --eval "db.userProfiles.deleteMany({})"
```

---

## 🐛 常见问题

### 问题1: OutOfMemoryError

**现象**: JMeter运行时报内存溢出

**解决方案**:
```bash
# 编辑 jmeter 或 jmeter.bat
# 增加堆内存大小
export HEAP="-Xms1g -Xmx4g -XX:MaxMetaspaceSize=512m"

# 或在运行时指定
JVM_ARGS="-Xmx4g" jmeter -n -t UserProfile-Performance-Test.jmx ...
```

### 问题2: Connection refused

**现象**: 请求返回连接被拒绝

**解决方案**:
```bash
# 1. 检查服务是否启动
curl http://localhost:8080/actuator/health

# 2. 检查防火墙
sudo ufw status

# 3. 检查base_url和base_port配置
jmeter -Jbase_url=192.168.1.100 -Jbase_port=8080 ...
```

### 问题3: Token过期

**现象**: 大量401错误

**解决方案**:
- Token有效期为24小时
- 如果测试超过24小时,需要实现Token刷新逻辑
- 或在Setup Thread Group中定期重新登录

### 问题4: 结果文件过大

**现象**: .jtl文件超过1GB

**解决方案**:
```bash
# 只保存必要字段
jmeter -n -t UserProfile-Performance-Test.jmx \
  -l results/result.jtl \
  -Jjmeter.save.saveservice.output_format=csv \
  -Jjmeter.save.saveservice.response_data=false \
  -Jjmeter.save.saveservice.samplerData=false \
  -Jjmeter.save.saveservice.response_headers=false \
  -Jjmeter.save.saveservice.request_headers=false
```

---

## 📚 参考资源

- **JMeter官方文档**: https://jmeter.apache.org/usermanual/
- **JMeter最佳实践**: https://jmeter.apache.org/usermanual/best-practices.html
- **JMeter插件**: https://jmeter-plugins.org/
- **Grafana JMeter Dashboard**: https://grafana.com/grafana/dashboards/4026

---

## 📞 技术支持

遇到问题?

1. 查看 [常见问题](#-常见问题) 章节
2. 查看 [故障排查指南](../../docs/TROUBLESHOOTING_GUIDE.md)
3. 联系技术支持: support@userprofile.com

---

**版本**: v1.0
**最后更新**: 2024-01-02
**维护者**: 用户画像系统团队
