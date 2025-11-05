import 'package:dio/dio.dart';
import 'package:logger/logger.dart';
import 'package:shared_preferences/shared_preferences.dart';
import '../models/user.dart';
import '../config/app_config.dart';

class ApiService {
  static final ApiService _instance = ApiService._internal();
  factory ApiService() => _instance;

  late Dio _dio;
  final Logger _logger = Logger();

  ApiService._internal() {
    _dio = Dio(BaseOptions(
      baseUrl: AppConfig.apiBaseUrl,
      connectTimeout: Duration(seconds: AppConfig.connectTimeout),
      receiveTimeout: Duration(seconds: AppConfig.receiveTimeout),
      headers: {
        'Content-Type': 'application/json',
      },
    ));

    // 添加拦截器
    _dio.interceptors.add(InterceptorsWrapper(
      onRequest: (options, handler) async {
        // 自动添加JWT Token
        final prefs = await SharedPreferences.getInstance();
        final token = prefs.getString(AppConfig.tokenKey);
        if (token != null) {
          options.headers['Authorization'] = 'Bearer $token';
        }

        if (AppConfig.enableLogging) {
          _logger.d('请求: ${options.method} ${options.uri}');
        }
        return handler.next(options);
      },
      onResponse: (response, handler) {
        if (AppConfig.enableLogging) {
          _logger.i('响应: ${response.statusCode} ${response.data}');
        }
        return handler.next(response);
      },
      onError: (error, handler) async {
        if (AppConfig.enableLogging) {
          _logger.e('错误: ${error.message}');
        }

        // Token过期，尝试刷新
        if (error.response?.statusCode == 401) {
          if (await _refreshToken()) {
            // 重试请求
            return handler.resolve(await _dio.fetch(error.requestOptions));
          }
        }

        return handler.next(error);
      },
    ));
  }

  /// 刷新Token
  Future<bool> _refreshToken() async {
    try {
      final prefs = await SharedPreferences.getInstance();
      final refreshToken = prefs.getString(AppConfig.refreshTokenKey);

      if (refreshToken == null) return false;

      final response = await _dio.post(
        '${AppConfig.authServiceUrl}/refresh',
        queryParameters: {'refreshToken': refreshToken},
      );

      if (response.data['code'] == 200) {
        final data = response.data['data'];
        await prefs.setString(AppConfig.tokenKey, data['accessToken']);
        await prefs.setString(AppConfig.refreshTokenKey, data['refreshToken']);
        return true;
      }
    } catch (e) {
      _logger.e('刷新Token失败: $e');
    }
    return false;
  }

  // 获取用户列表
  Future<List<User>> getUsers({int page = 1, int size = 10}) async {
    try {
      final response = await _dio.get(
        '${AppConfig.userServiceUrl}',
        queryParameters: {
          'page': page,
          'size': size,
        },
      );

      if (response.data['code'] == 200) {
        final data = response.data['data'];
        final records = data['records'] as List;
        return records.map((json) => User.fromJson(json)).toList();
      } else {
        throw Exception(response.data['message']);
      }
    } catch (e) {
      _logger.e('获取用户列表失败: $e');
      rethrow;
    }
  }

  // 获取单个用户
  Future<User> getUser(int id) async {
    try {
      final response = await _dio.get('/api/users/$id');

      if (response.data['code'] == 200) {
        return User.fromJson(response.data['data']);
      } else {
        throw Exception(response.data['message']);
      }
    } catch (e) {
      _logger.e('获取用户失败: $e');
      rethrow;
    }
  }

  // 创建用户
  Future<User> createUser(Map<String, dynamic> userData) async {
    try {
      final response = await _dio.post('/api/users', data: userData);

      if (response.data['code'] == 200) {
        return User.fromJson(response.data['data']);
      } else {
        throw Exception(response.data['message']);
      }
    } catch (e) {
      _logger.e('创建用户失败: $e');
      rethrow;
    }
  }

  // 更新用户
  Future<User> updateUser(int id, Map<String, dynamic> userData) async {
    try {
      final response = await _dio.put('/api/users/$id', data: userData);

      if (response.data['code'] == 200) {
        return User.fromJson(response.data['data']);
      } else {
        throw Exception(response.data['message']);
      }
    } catch (e) {
      _logger.e('更新用户失败: $e');
      rethrow;
    }
  }

  // 删除用户
  Future<void> deleteUser(int id) async {
    try {
      final response = await _dio.delete('/api/users/$id');

      if (response.data['code'] != 200) {
        throw Exception(response.data['message']);
      }
    } catch (e) {
      _logger.e('删除用户失败: $e');
      rethrow;
    }
  }

  // 更新API基础URL（用于配置不同环境）
  void updateBaseUrl(String newBaseUrl) {
    _dio.options.baseUrl = newBaseUrl;
  }
}
