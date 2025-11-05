#!/bin/bash

###############################################################################
# User Profile System - Automated Performance Testing Script
#
# 用途: 自动化执行完整的性能测试套件
# 作者: 用户画像系统团队
# 版本: v1.0
# 日期: 2024-01-02
###############################################################################

set -e  # 遇到错误立即退出

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 配置
BASE_DIR="testing/jmeter"
JMX_FILE="$BASE_DIR/UserProfile-Performance-Test.jmx"
RESULTS_BASE_DIR="results"
TIMESTAMP=$(date +%Y%m%d_%H%M%S)
RESULTS_DIR="$RESULTS_BASE_DIR/$TIMESTAMP"

# 创建结果目录
mkdir -p "$RESULTS_DIR"

# 日志文件
LOG_FILE="$RESULTS_DIR/test-execution.log"

# 函数: 打印带颜色的消息
log_info() {
    echo -e "${BLUE}[INFO]${NC} $1" | tee -a "$LOG_FILE"
}

log_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1" | tee -a "$LOG_FILE"
}

log_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1" | tee -a "$LOG_FILE"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1" | tee -a "$LOG_FILE"
}

# 函数: 打印分隔线
print_separator() {
    echo "======================================================================" | tee -a "$LOG_FILE"
}

# 函数: 检查JMeter是否安装
check_jmeter() {
    if ! command -v jmeter &> /dev/null; then
        log_error "JMeter not found. Please install JMeter first."
        log_info "Installation guide: https://jmeter.apache.org/download_jmeter.cgi"
        exit 1
    fi

    JMETER_VERSION=$(jmeter --version 2>&1 | grep -oP '(?<=Version )\S+' | head -1)
    log_info "JMeter Version: $JMETER_VERSION"
}

# 函数: 检查服务健康状态
check_services() {
    log_info "Checking service health..."

    SERVICES=("gateway:8080" "user-service:8081" "profile-service:8082" "tag-service:8084")
    ALL_HEALTHY=true

    for service in "${SERVICES[@]}"; do
        IFS=':' read -r -a parts <<< "$service"
        name="${parts[0]}"
        port="${parts[1]}"

        if curl -sf "http://localhost:$port/actuator/health" > /dev/null 2>&1; then
            log_success "$name is healthy"
        else
            log_error "$name is NOT healthy"
            ALL_HEALTHY=false
        fi
    done

    if [ "$ALL_HEALTHY" = false ]; then
        log_error "Some services are not healthy. Please check and restart services."
        exit 1
    fi
}

# 函数: 计算错误率
calculate_error_rate() {
    local jtl_file=$1

    if [ ! -f "$jtl_file" ]; then
        echo "0"
        return
    fi

    # 跳过header行,计算错误率
    ERROR_RATE=$(awk -F',' 'NR>1 {total++; if($8=="false") errors++} END {if(total>0) print errors/total*100; else print 0}' "$jtl_file")
    echo "${ERROR_RATE:-0}"
}

# 函数: 计算平均响应时间
calculate_avg_response_time() {
    local jtl_file=$1

    if [ ! -f "$jtl_file" ]; then
        echo "0"
        return
    fi

    # 跳过header行,计算平均响应时间(第2列)
    AVG_TIME=$(awk -F',' 'NR>1 {sum+=$2; count++} END {if(count>0) print sum/count; else print 0}' "$jtl_file")
    echo "${AVG_TIME:-0}"
}

# 函数: 运行性能测试
run_test() {
    local test_name=$1
    local threads=$2
    local ramp_time=$3
    local duration=$4
    local description=$5

    print_separator
    log_info "Running $test_name - $description"
    log_info "Parameters: Threads=$threads, RampTime=${ramp_time}s, Duration=${duration}s"
    print_separator

    local test_dir="$RESULTS_DIR/${test_name,,}"
    mkdir -p "$test_dir"

    local jtl_file="$test_dir/results.jtl"
    local report_dir="$test_dir/html-report"

    # 执行JMeter测试
    log_info "Starting test execution..."
    local start_time=$(date +%s)

    if jmeter -n -t "$JMX_FILE" \
        -Jthreads="$threads" \
        -Jramp_time="$ramp_time" \
        -Jduration="$duration" \
        -l "$jtl_file" \
        -e -o "$report_dir" >> "$LOG_FILE" 2>&1; then

        local end_time=$(date +%s)
        local elapsed=$((end_time - start_time))

        log_success "$test_name completed in ${elapsed}s"

        # 计算关键指标
        local error_rate=$(calculate_error_rate "$jtl_file")
        local avg_response_time=$(calculate_avg_response_time "$jtl_file")

        log_info "Error Rate: ${error_rate}%"
        log_info "Avg Response Time: ${avg_response_time}ms"
        log_info "HTML Report: $report_dir/index.html"

        # 检查是否通过
        if (( $(echo "$error_rate > 5" | bc -l) )); then
            log_warning "$test_name: Error rate (${error_rate}%) exceeds 5% threshold"
            return 1
        else
            log_success "$test_name: PASSED"
            return 0
        fi
    else
        log_error "$test_name: FAILED to execute"
        return 1
    fi
}

# 函数: 生成汇总报告
generate_summary() {
    print_separator
    log_info "Generating summary report..."
    print_separator

    local summary_file="$RESULTS_DIR/SUMMARY.md"

    cat > "$summary_file" << EOF
# Performance Test Summary Report

**Test Date**: $(date '+%Y-%m-%d %H:%M:%S')
**Results Directory**: \`$RESULTS_DIR\`

---

## Test Environment

- **Base URL**: http://localhost:8080
- **JMeter Version**: $JMETER_VERSION
- **Services**:
  - Gateway: http://localhost:8080
  - User Service: http://localhost:8081
  - Profile Service: http://localhost:8082
  - Tag Service: http://localhost:8084

---

## Test Results

EOF

    # 遍历所有测试结果
    for test_dir in "$RESULTS_DIR"/*/; do
        if [ -d "$test_dir" ] && [ -f "$test_dir/results.jtl" ]; then
            test_name=$(basename "$test_dir")
            jtl_file="$test_dir/results.jtl"

            # 计算指标
            total_samples=$(awk -F',' 'END {print NR-1}' "$jtl_file")
            error_rate=$(calculate_error_rate "$jtl_file")
            avg_response_time=$(calculate_avg_response_time "$jtl_file")

            # 计算最大/最小响应时间
            min_time=$(awk -F',' 'NR>1 {if(min==""){min=$2}; if($2<min){min=$2}} END {print min}' "$jtl_file")
            max_time=$(awk -F',' 'NR>1 {if(max==""){max=$2}; if($2>max){max=$2}} END {print max}' "$jtl_file")

            # 计算吞吐量(TPS)
            test_duration=$(awk -F',' 'NR==2 {start=$1} END {print ($1-start)/1000}' "$jtl_file")
            if [ -n "$test_duration" ] && (( $(echo "$test_duration > 0" | bc -l) )); then
                tps=$(echo "scale=2; $total_samples / $test_duration" | bc)
            else
                tps="N/A"
            fi

            # 写入汇总
            cat >> "$summary_file" << EOF
### $test_name

| Metric | Value |
|--------|-------|
| Total Samples | $total_samples |
| Error Rate | ${error_rate}% |
| Avg Response Time | ${avg_response_time}ms |
| Min Response Time | ${min_time}ms |
| Max Response Time | ${max_time}ms |
| Throughput (TPS) | $tps |
| Status | $(if (( $(echo "$error_rate < 5" | bc -l) )); then echo "✅ PASSED"; else echo "❌ FAILED"; fi) |

**HTML Report**: [\`html-report/index.html\`]($test_name/html-report/index.html)

---

EOF
        fi
    done

    cat >> "$summary_file" << EOF
## Conclusion

$(if [ -f "$RESULTS_DIR/smoke-test/results.jtl" ]; then
    smoke_error=$(calculate_error_rate "$RESULTS_DIR/smoke-test/results.jtl")
    if (( $(echo "$smoke_error == 0" | bc -l) )); then
        echo "✅ **Smoke Test**: PASSED - All basic functionalities are working"
    else
        echo "❌ **Smoke Test**: FAILED - Basic functionalities have issues"
    fi
fi)

$(if [ -f "$RESULTS_DIR/load-test/results.jtl" ]; then
    load_error=$(calculate_error_rate "$RESULTS_DIR/load-test/results.jtl")
    load_avg=$(calculate_avg_response_time "$RESULTS_DIR/load-test/results.jtl")
    if (( $(echo "$load_error < 1" | bc -l) )) && (( $(echo "$load_avg < 500" | bc -l) )); then
        echo "✅ **Load Test**: PASSED - System performs well under normal load"
    else
        echo "⚠️ **Load Test**: WARNING - Performance may need optimization"
    fi
fi)

$(if [ -f "$RESULTS_DIR/stress-test/results.jtl" ]; then
    stress_error=$(calculate_error_rate "$RESULTS_DIR/stress-test/results.jtl")
    if (( $(echo "$stress_error < 5" | bc -l) )); then
        echo "✅ **Stress Test**: PASSED - System handles high load well"
    else
        echo "⚠️ **Stress Test**: WARNING - System struggles under high load"
    fi
fi)

---

## Recommendations

1. Review detailed HTML reports for each test
2. Monitor system resources (CPU, Memory, Network) during tests
3. Check application logs for errors and warnings
4. Consider horizontal scaling if throughput is insufficient
5. Optimize database queries if response times are high

---

**Generated by**: User Profile System Performance Testing Framework
**Report Location**: \`$summary_file\`
EOF

    log_success "Summary report generated: $summary_file"
}

# 函数: 等待服务恢复
wait_for_recovery() {
    local wait_time=$1
    log_info "Waiting ${wait_time}s for service recovery..."
    sleep "$wait_time"
}

# 主函数
main() {
    print_separator
    echo -e "${BLUE}"
    cat << "EOF"
 _   _                 ____             __ _ _
| | | |___  ___ _ __  |  _ \ _ __ ___  / _(_) | ___
| | | / __|/ _ \ '__| | |_) | '__/ _ \| |_| | |/ _ \
| |_| \__ \  __/ |    |  __/| | | (_) |  _| | |  __/
 \___/|___/\___|_|    |_|   |_|  \___/|_| |_|_|\___|

    Performance Testing Framework v1.0
EOF
    echo -e "${NC}"
    print_separator

    log_info "Test execution started at $(date)"
    log_info "Results will be saved to: $RESULTS_DIR"
    print_separator

    # 1. 检查JMeter
    check_jmeter

    # 2. 检查服务健康状态
    check_services

    # 3. 运行测试套件
    local all_passed=true

    # Smoke Test (冒烟测试)
    if ! run_test "Smoke-Test" 10 10 60 "Basic functionality verification"; then
        log_error "Smoke test failed! Stopping test execution."
        exit 1
    fi
    wait_for_recovery 30

    # Load Test (负载测试)
    if ! run_test "Load-Test" 100 60 300 "Normal load performance test"; then
        all_passed=false
    fi
    wait_for_recovery 60

    # Stress Test (压力测试)
    if ! run_test "Stress-Test" 500 180 600 "High load stress test"; then
        all_passed=false
    fi
    wait_for_recovery 120

    # Spike Test (峰值测试)
    if ! run_test "Spike-Test" 1000 30 120 "Sudden traffic spike test"; then
        all_passed=false
    fi
    wait_for_recovery 120

    # Endurance Test (可选)
    read -p "$(echo -e ${YELLOW}Run Endurance Test \(1 hour\)? [y/N]: ${NC})" -n 1 -r
    echo
    if [[ $REPLY =~ ^[Yy]$ ]]; then
        run_test "Endurance-Test" 100 60 3600 "Long-running stability test"
    fi

    # 4. 生成汇总报告
    generate_summary

    # 5. 打印最终结果
    print_separator
    if [ "$all_passed" = true ]; then
        log_success "All tests PASSED! ✅"
    else
        log_warning "Some tests FAILED or had warnings. Please review the reports. ⚠️"
    fi
    print_separator

    log_info "Test execution completed at $(date)"
    log_info "Results location: $RESULTS_DIR"
    log_info "Summary report: $RESULTS_DIR/SUMMARY.md"
    log_info "Execution log: $LOG_FILE"
    print_separator

    # 6. 询问是否打开报告
    read -p "$(echo -e ${YELLOW}Open HTML reports in browser? [y/N]: ${NC})" -n 1 -r
    echo
    if [[ $REPLY =~ ^[Yy]$ ]]; then
        # 打开Load Test报告
        if [ -f "$RESULTS_DIR/load-test/html-report/index.html" ]; then
            log_info "Opening Load Test report..."
            if command -v xdg-open &> /dev/null; then
                xdg-open "$RESULTS_DIR/load-test/html-report/index.html"
            elif command -v open &> /dev/null; then
                open "$RESULTS_DIR/load-test/html-report/index.html"
            elif command -v start &> /dev/null; then
                start "$RESULTS_DIR/load-test/html-report/index.html"
            fi
        fi
    fi
}

# 错误处理
trap 'log_error "Script interrupted. Cleaning up..."; exit 1' INT TERM

# 执行主函数
main "$@"
