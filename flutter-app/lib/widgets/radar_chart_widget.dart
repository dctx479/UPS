import 'package:fl_chart/fl_chart.dart';
import 'package:flutter/material.dart';

///雷达图组件 - 用于多维度画像对比
class RadarChartWidget extends StatelessWidget {
  final Map<String, double> data;
  final List<Color> colors;

  const RadarChartWidget({
    super.key,
    required this.data,
    this.colors = const [Colors.blue, Colors.orange],
  });

  @override
  Widget build(BuildContext context) {
    return Card(
      elevation: 4,
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            const Text(
              '画像雷达图',
              style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold),
            ),
            const SizedBox(height: 20),
            SizedBox(
              height: 300,
              child: RadarChart(
                RadarChartData(
                  radarShape: RadarShape.polygon,
                  tickCount: 5,
                  ticksTextStyle: const TextStyle(fontSize: 10, color: Colors.transparent),
                  radarBorderData: const BorderSide(color: Colors.grey, width: 1),
                  gridBorderData: const BorderSide(color: Colors.grey, width: 0.5),
                  tickBorderData: const BorderSide(color: Colors.transparent),
                  dataSets: [
                    RadarDataSet(
                      fillColor: colors[0].withOpacity(0.3),
                      borderColor: colors[0],
                      borderWidth: 2,
                      dataEntries: data.entries
                          .map((e) => RadarEntry(value: e.value))
                          .toList(),
                    ),
                  ],
                  radarTouchData: RadarTouchData(
                    touchCallback: (FlTouchEvent event, response) {},
                  ),
                  titleTextStyle: const TextStyle(fontSize: 12, fontWeight: FontWeight.w500),
                  titlePositionPercentageOffset: 0.2,
                  getTitle: (index, angle) {
                    final titles = data.keys.toList();
                    if (index < titles.length) {
                      return RadarChartTitle(text: titles[index]);
                    }
                    return const RadarChartTitle(text: '');
                  },
                ),
              ),
            ),
            const SizedBox(height: 16),
            _buildLegend(),
          ],
        ),
      ),
    );
  }

  Widget _buildLegend() {
    return Wrap(
      spacing: 16,
      runSpacing: 8,
      children: data.entries.map((e) {
        return Row(
          mainAxisSize: MainAxisSize.min,
          children: [
            Container(
              width: 12,
              height: 12,
              color: colors[0],
            ),
            const SizedBox(width: 4),
            Text(
              '${e.key}: ${e.value.toStringAsFixed(1)}',
              style: const TextStyle(fontSize: 12),
            ),
          ],
        );
      }).toList(),
    );
  }
}
