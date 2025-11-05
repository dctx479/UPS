import 'package:fl_chart/fl_chart.dart';
import 'package:flutter/material.dart';

/// 漏斗图组件 - 用于转化率可视化
class FunnelChartWidget extends StatelessWidget {
  final Map<String, num> funnelData;

  const FunnelChartWidget({
    super.key,
    required this.funnelData,
  });

  @override
  Widget build(BuildContext context) {
    final sortedData = funnelData.entries.toList();
    final maxValue = sortedData.map((e) => e.value).reduce((a, b) => a > b ? a : b);

    return Card(
      elevation: 4,
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            const Text(
              '购物漏斗分析',
              style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold),
            ),
            const SizedBox(height: 20),
            ...sortedData.asMap().entries.map((entry) {
              int index = entry.key;
              var data = entry.value;
              double percentage = data.value / maxValue;

              String conversionRate = '';
              if (index > 0) {
                double rate = (data.value / sortedData[index - 1].value) * 100;
                conversionRate = ' (转化率: ${rate.toStringAsFixed(1)}%)';
              }

              return Padding(
                padding: const EdgeInsets.symmetric(vertical: 8),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      '${data.key}: ${data.value}$conversionRate',
                      style: const TextStyle(fontSize: 14, fontWeight: FontWeight.w500),
                    ),
                    const SizedBox(height: 8),
                    Stack(
                      children: [
                        Container(
                          height: 40,
                          decoration: BoxDecoration(
                            color: Colors.grey[200],
                            borderRadius: BorderRadius.circular(4),
                          ),
                        ),
                        Container(
                          height: 40,
                          width: MediaQuery.of(context).size.width * percentage * 0.8,
                          decoration: BoxDecoration(
                            gradient: LinearGradient(
                              colors: [
                                _getColorForIndex(index),
                                _getColorForIndex(index).withOpacity(0.6),
                              ],
                            ),
                            borderRadius: BorderRadius.circular(4),
                          ),
                          alignment: Alignment.centerRight,
                          padding: const EdgeInsets.only(right: 12),
                          child: Text(
                            '${(percentage * 100).toStringAsFixed(0)}%',
                            style: const TextStyle(
                              color: Colors.white,
                              fontWeight: FontWeight.bold,
                            ),
                          ),
                        ),
                      ],
                    ),
                  ],
                ),
              );
            }).toList(),
          ],
        ),
      ),
    );
  }

  Color _getColorForIndex(int index) {
    final colors = [
      Colors.blue,
      Colors.green,
      Colors.orange,
      Colors.purple,
      Colors.teal,
    ];
    return colors[index % colors.length];
  }
}
