import 'package:flutter/material.dart';
import '../widgets/radar_chart_widget.dart';
import '../widgets/funnel_chart_widget.dart';
import '../widgets/trend_line_chart_widget.dart';

/// 数据可视化展示页面
class DataVisualizationScreen extends StatefulWidget {
  const DataVisualizationScreen({super.key});

  @override
  State<DataVisualizationScreen> createState() => _DataVisualizationScreenState();
}

class _DataVisualizationScreenState extends State<DataVisualizationScreen> {
  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('数据可视化分析'),
      ),
      body: SingleChildScrollView(
        padding: const EdgeInsets.all(16),
        child: Column(
          children: [
            // 雷达图 - 多维度画像分析
            RadarChartWidget(
              data: {
                '活跃度': 85.0,
                '消费力': 72.0,
                '忠诚度': 90.0,
                '传播力': 65.0,
                '成长性': 78.0,
              },
            ),
            const SizedBox(height: 20),

            // 漏斗图 - 转化率分析
            FunnelChartWidget(
              funnelData: {
                '浏览商品': 1000,
                '加入购物车': 450,
                '提交订单': 300,
                '完成支付': 250,
              },
            ),
            const SizedBox(height: 20),

            // 趋势图 - 画像评分变化
            TrendLineChartWidget(
              title: '近30天画像评分趋势',
              dataPoints: _generateTrendData(),
            ),
            const SizedBox(height: 20),

            // RFM分布图
            _buildRFMDistribution(),

            const SizedBox(height: 20),

            // 用户分层统计
            _buildUserSegmentation(),
          ],
        ),
      ),
    );
  }

  List<TrendDataPoint> _generateTrendData() {
    // 模拟数据，实际应从API获取
    return [
      TrendDataPoint(label: '1/1', value: 65),
      TrendDataPoint(label: '1/5', value: 70),
      TrendDataPoint(label: '1/10', value: 68),
      TrendDataPoint(label: '1/15', value: 75),
      TrendDataPoint(label: '1/20', value: 80),
      TrendDataPoint(label: '1/25', value: 85),
      TrendDataPoint(label: '1/30', value: 87),
    ];
  }

  Widget _buildRFMDistribution() {
    return Card(
      elevation: 4,
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            const Text(
              'RFM用户分布',
              style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold),
            ),
            const SizedBox(height: 20),
            Row(
              children: [
                Expanded(
                  child: _buildRFMCard('重要价值', 128, Colors.green),
                ),
                const SizedBox(width: 12),
                Expanded(
                  child: _buildRFMCard('重要发展', 256, Colors.blue),
                ),
              ],
            ),
            const SizedBox(height: 12),
            Row(
              children: [
                Expanded(
                  child: _buildRFMCard('重要保持', 189, Colors.orange),
                ),
                const SizedBox(width: 12),
                Expanded(
                  child: _buildRFMCard('一般客户', 412, Colors.grey),
                ),
              ],
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildRFMCard(String label, int count, Color color) {
    return Container(
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: color.withOpacity(0.1),
        border: Border.all(color: color, width: 2),
        borderRadius: BorderRadius.circular(8),
      ),
      child: Column(
        children: [
          Text(
            count.toString(),
            style: TextStyle(
              fontSize: 32,
              fontWeight: FontWeight.bold,
              color: color,
            ),
          ),
          const SizedBox(height: 4),
          Text(
            label,
            style: TextStyle(
              fontSize: 14,
              color: color,
              fontWeight: FontWeight.w500,
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildUserSegmentation() {
    return Card(
      elevation: 4,
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            const Text(
              '用户分层统计',
              style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold),
            ),
            const SizedBox(height: 20),
            _buildSegmentRow('高价值用户', 15.2, Colors.green),
            _buildSegmentRow('活跃用户', 28.5, Colors.blue),
            _buildSegmentRow('潜力用户', 35.8, Colors.orange),
            _buildSegmentRow('普通用户', 18.3, Colors.grey),
            _buildSegmentRow('流失风险', 2.2, Colors.red),
          ],
        ),
      ),
    );
  }

  Widget _buildSegmentRow(String label, double percentage, Color color) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 8),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              Text(label, style: const TextStyle(fontSize: 14)),
              Text(
                '${percentage.toStringAsFixed(1)}%',
                style: TextStyle(
                  fontSize: 14,
                  fontWeight: FontWeight.bold,
                  color: color,
                ),
              ),
            ],
          ),
          const SizedBox(height: 8),
          LinearProgressIndicator(
            value: percentage / 100,
            backgroundColor: Colors.grey[200],
            valueColor: AlwaysStoppedAnimation<Color>(color),
            minHeight: 8,
          ),
        ],
      ),
    );
  }
}
