import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../models/user.dart';
import '../models/user_profile.dart';
import '../services/profile_service.dart';
import '../providers/user_provider.dart';
import '../widgets/loading_widget.dart';
import '../widgets/error_widget.dart' as custom_error;

class ProfileScreen extends StatefulWidget {
  const ProfileScreen({super.key});

  @override
  State<ProfileScreen> createState() => _ProfileScreenState();
}

class _ProfileScreenState extends State<ProfileScreen> {
  final ProfileService _profileService = ProfileService();
  UserProfile? _selectedProfile;
  User? _selectedUser;
  bool _isLoading = false;
  String? _errorMessage;

  @override
  void initState() {
    super.initState();
    _loadFirstUser();
  }

  Future<void> _loadFirstUser() async {
    final userProvider = context.read<UserProvider>();
    if (userProvider.users.isEmpty) {
      await userProvider.loadUsers();
    }
    if (userProvider.users.isNotEmpty) {
      _loadProfile(userProvider.users.first);
    }
  }

  Future<void> _loadProfile(User user) async {
    setState(() {
      _isLoading = true;
      _errorMessage = null;
      _selectedUser = user;
    });

    try {
      final profile = await _profileService.getProfileByUserId(user.id!);
      setState(() {
        _selectedProfile = profile;
        _isLoading = false;
      });
    } catch (e) {
      setState(() {
        _errorMessage = '加载画像失败: $e';
        _isLoading = false;
      });
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('用户画像'),
        actions: [
          IconButton(
            icon: const Icon(Icons.refresh),
            onPressed: () {
              if (_selectedUser != null) {
                _loadProfile(_selectedUser!);
              }
            },
          ),
        ],
      ),
      body: Row(
        children: [
          // 左侧：用户列表
          SizedBox(
            width: 250,
            child: _buildUserList(),
          ),
          const VerticalDivider(width: 1),
          // 右侧：画像详情
          Expanded(
            child: _buildProfileDetail(),
          ),
        ],
      ),
    );
  }

  Widget _buildUserList() {
    return Consumer<UserProvider>(
      builder: (context, userProvider, child) {
        if (userProvider.users.isEmpty) {
          return const Center(child: Text('暂无用户'));
        }

        return ListView.builder(
          itemCount: userProvider.users.length,
          itemBuilder: (context, index) {
            final user = userProvider.users[index];
            final isSelected = _selectedUser?.id == user.id;

            return ListTile(
              selected: isSelected,
              leading: CircleAvatar(
                backgroundColor: isSelected ? Colors.blue : Colors.grey,
                child: Text(
                  user.name?.substring(0, 1) ?? user.username.substring(0, 1),
                  style: const TextStyle(color: Colors.white),
                ),
              ),
              title: Text(user.name ?? user.username),
              subtitle: Text('ID: ${user.id}'),
              onTap: () => _loadProfile(user),
            );
          },
        );
      },
    );
  }

  Widget _buildProfileDetail() {
    // P2-2修复: 使用新的Loading组件
    if (_isLoading) {
      return const LoadingWidget(message: '加载用户画像中...');
    }

    // P2-2修复: 使用新的Error组件
    if (_errorMessage != null) {
      return custom_error.ErrorWidget(
        message: _errorMessage!,
        onRetry: () {
          if (_selectedUser != null) {
            _loadProfile(_selectedUser!);
          }
        },
      );
    }

    // P2-2修复: 使用新的Empty组件
    if (_selectedProfile == null) {
      return custom_error.EmptyDataWidget(
        message: '请从左侧列表选择用户\n查看详细画像信息',
        icon: Icons.person_search,
      );
    }

    return SingleChildScrollView(
      padding: const EdgeInsets.all(20),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          _buildScoreCard(),
          const SizedBox(height: 20),
          _buildDigitalBehaviorCard(),
          const SizedBox(height: 20),
          _buildCoreNeedsCard(),
          const SizedBox(height: 20),
          _buildValueAssessmentCard(),
          const SizedBox(height: 20),
          _buildStickinessCard(),
        ],
      ),
    );
  }

  Widget _buildScoreCard() {
    final score = _selectedProfile!.profileScore ?? 0;
    final level = _selectedProfile!.getScoreLevel();

    return Card(
      elevation: 4,
      child: Padding(
        padding: const EdgeInsets.all(20),
        child: Column(
          children: [
            const Text(
              '综合评分',
              style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold),
            ),
            const SizedBox(height: 20),
            SizedBox(
              width: 150,
              height: 150,
              child: Stack(
                alignment: Alignment.center,
                children: [
                  SizedBox(
                    width: 150,
                    height: 150,
                    child: CircularProgressIndicator(
                      value: score / 100,
                      strokeWidth: 12,
                      backgroundColor: Colors.grey[300],
                      valueColor: AlwaysStoppedAnimation<Color>(
                        score >= 80
                            ? Colors.green
                            : score >= 60
                                ? Colors.blue
                                : Colors.orange,
                      ),
                    ),
                  ),
                  Column(
                    mainAxisSize: MainAxisSize.min,
                    children: [
                      Text(
                        score.toStringAsFixed(1),
                        style: const TextStyle(
                          fontSize: 36,
                          fontWeight: FontWeight.bold,
                        ),
                      ),
                      Text(
                        level,
                        style: TextStyle(
                          fontSize: 16,
                          color: Colors.grey[600],
                        ),
                      ),
                    ],
                  ),
                ],
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildDigitalBehaviorCard() {
    final behavior = _selectedProfile!.digitalBehavior;
    if (behavior == null) return const SizedBox.shrink();

    return Card(
      elevation: 2,
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            const Row(
              children: [
                Icon(Icons.analytics, color: Colors.blue),
                SizedBox(width: 8),
                Text(
                  '数字行为分析',
                  style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold),
                ),
              ],
            ),
            const Divider(),
            if (behavior.productCategories != null &&
                behavior.productCategories!.isNotEmpty)
              _buildInfoRow(
                '产品品类',
                behavior.productCategories!.join(', '),
              ),
            if (behavior.infoAcquisitionHabit != null)
              _buildInfoRow('信息获取', behavior.infoAcquisitionHabit!),
            if (behavior.purchaseDecisionPreference != null)
              _buildInfoRow('购买偏好', behavior.purchaseDecisionPreference!),
            if (behavior.brandPreferences != null &&
                behavior.brandPreferences!.isNotEmpty)
              _buildInfoRow(
                '品牌偏好',
                behavior.brandPreferences!.join(', '),
              ),
          ],
        ),
      ),
    );
  }

  Widget _buildCoreNeedsCard() {
    final coreNeeds = _selectedProfile!.coreNeeds;
    if (coreNeeds == null) return const SizedBox.shrink();

    return Card(
      elevation: 2,
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            const Row(
              children: [
                Icon(Icons.favorite, color: Colors.red),
                SizedBox(width: 8),
                Text(
                  '核心需求',
                  style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold),
                ),
              ],
            ),
            const Divider(),
            if (coreNeeds.topConcerns != null &&
                coreNeeds.topConcerns!.isNotEmpty)
              _buildInfoRow(
                '核心关注',
                coreNeeds.topConcerns!.join(', '),
              ),
            if (coreNeeds.decisionPainPoint != null)
              _buildInfoRow('决策痛点', coreNeeds.decisionPainPoint!),
          ],
        ),
      ),
    );
  }

  Widget _buildValueAssessmentCard() {
    final value = _selectedProfile!.valueAssessment;
    if (value == null) return const SizedBox.shrink();

    return Card(
      elevation: 2,
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            const Row(
              children: [
                Icon(Icons.star, color: Colors.amber),
                SizedBox(width: 8),
                Text(
                  '价值评估',
                  style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold),
                ),
              ],
            ),
            const Divider(),
            if (value.profileQuality != null)
              _buildInfoRow('画像质量', value.profileQuality!),
            if (value.feedingMethod != null)
              _buildInfoRow('消费水平', value.feedingMethod!),
          ],
        ),
      ),
    );
  }

  Widget _buildStickinessCard() {
    final stickiness = _selectedProfile!.stickinessAndLoyalty;
    if (stickiness == null) return const SizedBox.shrink();

    return Card(
      elevation: 2,
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            const Row(
              children: [
                Icon(Icons.loyalty, color: Colors.purple),
                SizedBox(width: 8),
                Text(
                  '粘性与忠诚度',
                  style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold),
                ),
              ],
            ),
            const Divider(),
            if (stickiness.loyaltyScore != null)
              _buildInfoRow(
                '忠诚度评分',
                stickiness.loyaltyScore!.toStringAsFixed(1),
              ),
            if (stickiness.painPoint != null)
              _buildInfoRow('痛点分析', stickiness.painPoint!),
          ],
        ),
      ),
    );
  }

  Widget _buildInfoRow(String label, String value) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 8),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          SizedBox(
            width: 100,
            child: Text(
              label,
              style: TextStyle(
                fontWeight: FontWeight.w500,
                color: Colors.grey[700],
              ),
            ),
          ),
          Expanded(
            child: Text(
              value,
              style: const TextStyle(fontSize: 15),
            ),
          ),
        ],
      ),
    );
  }
}
