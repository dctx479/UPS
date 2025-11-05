import 'package:flutter/material.dart';

class HomeScreen extends StatelessWidget {
  const HomeScreen({super.key});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('用户画像系统'),
        centerTitle: true,
      ),
      body: Padding(
        padding: const EdgeInsets.all(20.0),
        child: GridView.count(
          crossAxisCount: 2,
          mainAxisSpacing: 20,
          crossAxisSpacing: 20,
          children: [
            _buildMenuCard(
              context,
              title: '用户管理',
              icon: Icons.people,
              color: Colors.blue,
              onTap: () => Navigator.pushNamed(context, '/users'),
            ),
            _buildMenuCard(
              context,
              title: '用户画像',
              icon: Icons.account_circle,
              color: Colors.green,
              onTap: () => Navigator.pushNamed(context, '/profile'),
            ),
            _buildMenuCard(
              context,
              title: '标签管理',
              icon: Icons.label,
              color: Colors.orange,
              onTap: () {
                // TODO: 导航到标签管理
              },
            ),
            _buildMenuCard(
              context,
              title: '数据可视化',
              icon: Icons.analytics,
              color: Colors.purple,
              onTap: () => Navigator.pushNamed(context, '/visualization'),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildMenuCard(
    BuildContext context, {
    required String title,
    required IconData icon,
    required Color color,
    required VoidCallback onTap,
  }) {
    return Card(
      elevation: 4,
      shape: RoundedRectangleBorder(
        borderRadius: BorderRadius.circular(15),
      ),
      child: InkWell(
        onTap: onTap,
        borderRadius: BorderRadius.circular(15),
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            Icon(
              icon,
              size: 60,
              color: color,
            ),
            const SizedBox(height: 15),
            Text(
              title,
              style: const TextStyle(
                fontSize: 18,
                fontWeight: FontWeight.bold,
              ),
            ),
          ],
        ),
      ),
    );
  }
}
