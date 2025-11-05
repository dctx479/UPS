/// 应用环境配置
///
/// 支持多环境配置（开发、测试、生产）
/// 使用方法：flutter run --dart-define=ENV=prod
class AppConfig {
  /// 当前环境（dev/staging/prod）
  static const String ENV = String.fromEnvironment('ENV', defaultValue: 'dev');

  /// API基础URL
  static String get apiBaseUrl {
    switch (ENV) {
      case 'prod':
        return const String.fromEnvironment(
          'API_URL',
          defaultValue: 'https://api.userprofile.com',
        );
      case 'staging':
        return const String.fromEnvironment(
          'API_URL',
          defaultValue: 'https://api-staging.userprofile.com',
        );
      default:
        // 开发环境：通过API网关访问
        return const String.fromEnvironment(
          'API_URL',
          defaultValue: 'http://localhost:8080',
        );
    }
  }

  /// 用户服务URL
  static String get userServiceUrl => '$apiBaseUrl/api/users';

  /// 画像服务URL
  static String get profileServiceUrl => '$apiBaseUrl/api/profiles';

  /// 标签服务URL
  static String get tagServiceUrl => '$apiBaseUrl/api/tags';

  /// 事件服务URL
  static String get eventServiceUrl => '$apiBaseUrl/api/events';

  /// 分群服务URL
  static String get segmentServiceUrl => '$apiBaseUrl/api/segments';

  /// 推荐服务URL
  static String get recommendationServiceUrl => '$apiBaseUrl/api/recommendations';

  /// 认证服务URL
  static String get authServiceUrl => '$apiBaseUrl/api/auth';

  /// 是否启用调试模式
  static bool get isDebugMode => ENV == 'dev';

  /// 是否启用日志
  static bool get enableLogging => ENV != 'prod';

  /// 网络超时时间（秒）
  static int get connectTimeout {
    switch (ENV) {
      case 'prod':
        return 10;
      case 'staging':
        return 15;
      default:
        return 30; // 开发环境允许更长的超时
    }
  }

  /// 接收超时时间（秒）
  static int get receiveTimeout {
    switch (ENV) {
      case 'prod':
        return 10;
      case 'staging':
        return 15;
      default:
        return 30;
    }
  }

  /// 缓存过期时间（分钟）
  static int get cacheExpiration {
    switch (ENV) {
      case 'prod':
        return 30;
      case 'staging':
        return 15;
      default:
        return 5;
    }
  }

  /// JWT Token Key
  static const String tokenKey = 'jwt_token';

  /// Refresh Token Key
  static const String refreshTokenKey = 'refresh_token';

  /// 用户信息Key
  static const String userInfoKey = 'user_info';

  /// 打印配置信息
  static void printConfig() {
    if (!enableLogging) return;

    print('==================== App Config ====================');
    print('Environment: $ENV');
    print('API Base URL: $apiBaseUrl');
    print('Debug Mode: $isDebugMode');
    print('Connect Timeout: ${connectTimeout}s');
    print('Receive Timeout: ${receiveTimeout}s');
    print('Cache Expiration: ${cacheExpiration}min');
    print('====================================================');
  }

  /// 获取环境名称
  static String get environmentName {
    switch (ENV) {
      case 'prod':
        return '生产环境';
      case 'staging':
        return '测试环境';
      default:
        return '开发环境';
    }
  }

  /// 是否是生产环境
  static bool get isProduction => ENV == 'prod';

  /// 是否是测试环境
  static bool get isStaging => ENV == 'staging';

  /// 是否是开发环境
  static bool get isDevelopment => ENV == 'dev';
}
