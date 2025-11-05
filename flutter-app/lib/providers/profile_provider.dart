import 'package:flutter/material.dart';
import '../services/api_service.dart';
import '../models/user_profile.dart';

/// 用户画像状态管理
///
/// 提供画像数据的获取、缓存、加载状态管理
class ProfileProvider with ChangeNotifier {
  final ApiService _apiService;

  ProfileProvider(this._apiService);

  // 画像数据
  UserProfile? _currentProfile;
  Map<int, UserProfile> _profileCache = {};

  // 加载状态
  bool _isLoading = false;
  String? _error;

  // Getters
  UserProfile? get currentProfile => _currentProfile;
  bool get isLoading => _isLoading;
  String? get error => _error;
  bool get hasError => _error != null;
  bool get hasProfile => _currentProfile != null;

  /// 获取用户画像
  Future<void> fetchProfile(int userId) async {
    // 检查缓存
    if (_profileCache.containsKey(userId)) {
      _currentProfile = _profileCache[userId];
      notifyListeners();
      return;
    }

    _isLoading = true;
    _error = null;
    notifyListeners();

    try {
      final response = await _apiService.get('/api/profiles/user/$userId');

      if (response['code'] == 200) {
        _currentProfile = UserProfile.fromJson(response['data']);
        _profileCache[userId] = _currentProfile!;
        _error = null;
      } else {
        _error = response['message'] ?? '获取画像失败';
        _currentProfile = null;
      }
    } catch (e) {
      _error = '网络错误: ${e.toString()}';
      _currentProfile = null;
    } finally {
      _isLoading = false;
      notifyListeners();
    }
  }

  /// 计算用户画像
  Future<void> calculateProfile(int userId) async {
    _isLoading = true;
    _error = null;
    notifyListeners();

    try {
      final response = await _apiService.post(
        '/api/profiles/calculate/$userId',
        {},
      );

      if (response['code'] == 200) {
        _currentProfile = UserProfile.fromJson(response['data']);
        _profileCache[userId] = _currentProfile!;
        _error = null;
      } else {
        _error = response['message'] ?? '计算画像失败';
      }
    } catch (e) {
      _error = '网络错误: ${e.toString()}';
    } finally {
      _isLoading = false;
      notifyListeners();
    }
  }

  /// 获取用户推荐
  Future<List<Map<String, dynamic>>> fetchRecommendations(int userId) async {
    try {
      final response = await _apiService.get('/api/recommendations/user/$userId');

      if (response['code'] == 200) {
        return List<Map<String, dynamic>>.from(response['data'] ?? []);
      }
      return [];
    } catch (e) {
      debugPrint('获取推荐失败: $e');
      return [];
    }
  }

  /// 获取用户分群
  Future<List<String>> fetchSegments(int userId) async {
    try {
      final response = await _apiService.get('/api/segments/user/$userId');

      if (response['code'] == 200) {
        final List<dynamic> segments = response['data'] ?? [];
        return segments.map((s) => s['segmentName'] as String).toList();
      }
      return [];
    } catch (e) {
      debugPrint('获取分群失败: $e');
      return [];
    }
  }

  /// 刷新当前画像
  Future<void> refreshProfile() async {
    if (_currentProfile != null) {
      // 清除缓存并重新获取
      _profileCache.remove(_currentProfile!.userId);
      await fetchProfile(_currentProfile!.userId);
    }
  }

  /// 清除缓存
  void clearCache() {
    _profileCache.clear();
    _currentProfile = null;
    _error = null;
    notifyListeners();
  }

  /// 清除错误
  void clearError() {
    _error = null;
    notifyListeners();
  }
}
