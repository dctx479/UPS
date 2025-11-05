import 'package:flutter/material.dart';
import '../models/user.dart';
import '../services/api_service.dart';

class UserProvider with ChangeNotifier {
  final ApiService _apiService = ApiService();

  List<User> _users = [];
  bool _isLoading = false;
  String? _errorMessage;

  List<User> get users => _users;
  bool get isLoading => _isLoading;
  String? get errorMessage => _errorMessage;

  // 加载用户列表
  Future<void> loadUsers({int page = 1, int size = 10}) async {
    _isLoading = true;
    _errorMessage = null;
    notifyListeners();

    try {
      _users = await _apiService.getUsers(page: page, size: size);
    } catch (e) {
      _errorMessage = '加载用户列表失败: $e';
    } finally {
      _isLoading = false;
      notifyListeners();
    }
  }

  // 创建用户
  Future<bool> createUser(Map<String, dynamic> userData) async {
    _isLoading = true;
    _errorMessage = null;
    notifyListeners();

    try {
      final user = await _apiService.createUser(userData);
      _users.add(user);
      _isLoading = false;
      notifyListeners();
      return true;
    } catch (e) {
      _errorMessage = '创建用户失败: $e';
      _isLoading = false;
      notifyListeners();
      return false;
    }
  }

  // 更新用户
  Future<bool> updateUser(int id, Map<String, dynamic> userData) async {
    _isLoading = true;
    _errorMessage = null;
    notifyListeners();

    try {
      final updatedUser = await _apiService.updateUser(id, userData);
      final index = _users.indexWhere((user) => user.id == id);
      if (index != -1) {
        _users[index] = updatedUser;
      }
      _isLoading = false;
      notifyListeners();
      return true;
    } catch (e) {
      _errorMessage = '更新用户失败: $e';
      _isLoading = false;
      notifyListeners();
      return false;
    }
  }

  // 删除用户
  Future<bool> deleteUser(int id) async {
    _isLoading = true;
    _errorMessage = null;
    notifyListeners();

    try {
      await _apiService.deleteUser(id);
      _users.removeWhere((user) => user.id == id);
      _isLoading = false;
      notifyListeners();
      return true;
    } catch (e) {
      _errorMessage = '删除用户失败: $e';
      _isLoading = false;
      notifyListeners();
      return false;
    }
  }
}
