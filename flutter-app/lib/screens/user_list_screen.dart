import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../providers/user_provider.dart';
import '../models/user.dart';
import '../widgets/loading_widget.dart';
import '../widgets/error_widget.dart' as custom_error;

class UserListScreen extends StatefulWidget {
  const UserListScreen({super.key});

  @override
  State<UserListScreen> createState() => _UserListScreenState();
}

class _UserListScreenState extends State<UserListScreen> {
  @override
  void initState() {
    super.initState();
    // 加载用户列表
    WidgetsBinding.instance.addPostFrameCallback((_) {
      context.read<UserProvider>().loadUsers();
    });
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('用户列表'),
        actions: [
          IconButton(
            icon: const Icon(Icons.refresh),
            onPressed: () {
              context.read<UserProvider>().loadUsers();
            },
          ),
        ],
      ),
      body: Consumer<UserProvider>(
        builder: (context, userProvider, child) {
          // P2-2修复: 使用新的Loading组件
          if (userProvider.isLoading) {
            return const LoadingWidget(message: '加载用户列表中...');
          }

          // P2-2修复: 使用新的Error组件
          if (userProvider.errorMessage != null) {
            return custom_error.NetworkErrorWidget(
              onRetry: () => userProvider.loadUsers(),
            );
          }

          // P2-2修复: 使用新的Empty组件
          if (userProvider.users.isEmpty) {
            return custom_error.EmptyDataWidget(
              message: '还没有用户数据\n点击右下角按钮添加用户',
              icon: Icons.people_outline,
              onAction: () => _showAddUserDialog(context),
              actionText: '添加用户',
            );
          }

          return ListView.builder(
            itemCount: userProvider.users.length,
            padding: const EdgeInsets.all(8),
            itemBuilder: (context, index) {
              final user = userProvider.users[index];
              return _buildUserCard(context, user, userProvider);
            },
          );
        },
      ),
      floatingActionButton: FloatingActionButton(
        onPressed: () {
          _showAddUserDialog(context);
        },
        child: const Icon(Icons.add),
      ),
    );
  }

  Widget _buildUserCard(
      BuildContext context, User user, UserProvider userProvider) {
    return Card(
      margin: const EdgeInsets.symmetric(vertical: 8, horizontal: 4),
      child: ListTile(
        leading: CircleAvatar(
          backgroundColor: Colors.blue,
          child: Text(
            user.name?.substring(0, 1) ?? user.username.substring(0, 1),
            style: const TextStyle(color: Colors.white),
          ),
        ),
        title: Text(user.name ?? user.username),
        subtitle: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text('用户名: ${user.username}'),
            if (user.age != null) Text('年龄: ${user.age}岁'),
            if (user.phone != null) Text('电话: ${user.phone}'),
          ],
        ),
        trailing: PopupMenuButton(
          itemBuilder: (context) => [
            const PopupMenuItem(
              value: 'view',
              child: Row(
                children: [
                  Icon(Icons.visibility),
                  SizedBox(width: 8),
                  Text('查看'),
                ],
              ),
            ),
            const PopupMenuItem(
              value: 'edit',
              child: Row(
                children: [
                  Icon(Icons.edit),
                  SizedBox(width: 8),
                  Text('编辑'),
                ],
              ),
            ),
            const PopupMenuItem(
              value: 'delete',
              child: Row(
                children: [
                  Icon(Icons.delete, color: Colors.red),
                  SizedBox(width: 8),
                  Text('删除', style: TextStyle(color: Colors.red)),
                ],
              ),
            ),
          ],
          onSelected: (value) {
            if (value == 'delete' && user.id != null) {
              _showDeleteConfirmDialog(context, user.id!, userProvider);
            }
            // TODO: 实现查看和编辑功能
          },
        ),
      ),
    );
  }

  void _showAddUserDialog(BuildContext context) {
    final usernameController = TextEditingController();
    final nameController = TextEditingController();

    showDialog(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text('添加用户'),
        content: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            TextField(
              controller: usernameController,
              decoration: const InputDecoration(
                labelText: '用户名',
                border: OutlineInputBorder(),
              ),
            ),
            const SizedBox(height: 16),
            TextField(
              controller: nameController,
              decoration: const InputDecoration(
                labelText: '姓名',
                border: OutlineInputBorder(),
              ),
            ),
          ],
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(context),
            child: const Text('取消'),
          ),
          ElevatedButton(
            onPressed: () async {
              if (usernameController.text.isNotEmpty) {
                final success = await context.read<UserProvider>().createUser({
                  'username': usernameController.text,
                  'name': nameController.text,
                });

                if (success && context.mounted) {
                  Navigator.pop(context);
                  ScaffoldMessenger.of(context).showSnackBar(
                    const SnackBar(content: Text('用户创建成功')),
                  );
                }
              }
            },
            child: const Text('确定'),
          ),
        ],
      ),
    );
  }

  void _showDeleteConfirmDialog(
      BuildContext context, int userId, UserProvider userProvider) {
    showDialog(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text('确认删除'),
        content: const Text('确定要删除这个用户吗？'),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(context),
            child: const Text('取消'),
          ),
          ElevatedButton(
            style: ElevatedButton.styleFrom(backgroundColor: Colors.red),
            onPressed: () async {
              final success = await userProvider.deleteUser(userId);
              if (success && context.mounted) {
                Navigator.pop(context);
                ScaffoldMessenger.of(context).showSnackBar(
                  const SnackBar(content: Text('用户删除成功')),
                );
              }
            },
            child: const Text('删除'),
          ),
        ],
      ),
    );
  }
}
