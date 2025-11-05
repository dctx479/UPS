import 'package:dio/dio.dart';
import 'package:logger/logger.dart';
import '../models/user_profile.dart';

class ProfileService {
  static final ProfileService _instance = ProfileService._internal();
  factory ProfileService() => _instance;

  late Dio _dio;
  final Logger _logger = Logger();

  static const String baseUrl = 'http://localhost:8082';

  ProfileService._internal() {
    _dio = Dio(BaseOptions(
      baseUrl: baseUrl,
      connectTimeout: const Duration(seconds: 5),
      receiveTimeout: const Duration(seconds: 3),
      headers: {
        'Content-Type': 'application/json',
      },
    ));

    _dio.interceptors.add(InterceptorsWrapper(
      onRequest: (options, handler) {
        _logger.d('请求: ${options.method} ${options.uri}');
        return handler.next(options);
      },
      onResponse: (response, handler) {
        _logger.i('响应: ${response.statusCode}');
        return handler.next(response);
      },
      onError: (error, handler) {
        _logger.e('错误: ${error.message}');
        return handler.next(error);
      },
    ));
  }

  // 获取用户画像
  Future<UserProfile> getProfileByUserId(int userId) async {
    try {
      final response = await _dio.get('/api/profiles/user/$userId');

      if (response.data['code'] == 200) {
        return UserProfile.fromJson(response.data['data']);
      } else {
        throw Exception(response.data['message']);
      }
    } catch (e) {
      _logger.e('获取用户画像失败: $e');
      rethrow;
    }
  }

  // 获取所有画像
  Future<List<UserProfile>> getAllProfiles() async {
    try {
      final response = await _dio.get('/api/profiles');

      if (response.data['code'] == 200) {
        final data = response.data['data'] as List;
        return data.map((json) => UserProfile.fromJson(json)).toList();
      } else {
        throw Exception(response.data['message']);
      }
    } catch (e) {
      _logger.e('获取画像列表失败: $e');
      rethrow;
    }
  }

  // 创建或更新画像
  Future<UserProfile> createOrUpdateProfile(
      Map<String, dynamic> profileData) async {
    try {
      final response = await _dio.post('/api/profiles', data: profileData);

      if (response.data['code'] == 200) {
        return UserProfile.fromJson(response.data['data']);
      } else {
        throw Exception(response.data['message']);
      }
    } catch (e) {
      _logger.e('保存画像失败: $e');
      rethrow;
    }
  }

  // 分析用户类型
  Future<String> analyzeUserType(int userId) async {
    try {
      final response = await _dio.get('/api/profiles/user/$userId/type');

      if (response.data['code'] == 200) {
        return response.data['data']['userType'];
      } else {
        throw Exception(response.data['message']);
      }
    } catch (e) {
      _logger.e('分析用户类型失败: $e');
      rethrow;
    }
  }

  // 生成用户标签
  Future<List<String>> generateUserTags(int userId) async {
    try {
      final response = await _dio.get('/api/profiles/user/$userId/tags');

      if (response.data['code'] == 200) {
        return List<String>.from(response.data['data']);
      } else {
        throw Exception(response.data['message']);
      }
    } catch (e) {
      _logger.e('生成用户标签失败: $e');
      rethrow;
    }
  }

  // 推荐营销策略
  Future<Map<String, String>> recommendStrategy(int userId) async {
    try {
      final response = await _dio.get('/api/profiles/user/$userId/strategy');

      if (response.data['code'] == 200) {
        return Map<String, String>.from(response.data['data']);
      } else {
        throw Exception(response.data['message']);
      }
    } catch (e) {
      _logger.e('获取营销策略失败: $e');
      rethrow;
    }
  }

  // 获取统计信息
  Future<Map<String, dynamic>> getStatistics() async {
    try {
      final response = await _dio.get('/api/profiles/statistics');

      if (response.data['code'] == 200) {
        return Map<String, dynamic>.from(response.data['data']);
      } else {
        throw Exception(response.data['message']);
      }
    } catch (e) {
      _logger.e('获取统计信息失败: $e');
      rethrow;
    }
  }

  // 更新基础URL
  void updateBaseUrl(String newBaseUrl) {
    _dio.options.baseUrl = newBaseUrl;
  }
}
