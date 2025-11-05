# Flutter 代码生成说明

由于当前环境未安装Flutter SDK，需要手动运行以下命令生成.g.dart文件。

## 生成步骤

### 1. 安装依赖
```bash
cd flutter-app
flutter pub get
```

### 2. 生成代码
```bash
flutter pub run build_runner build --delete-conflicting-outputs
```

### 3. 预期生成的文件

执行成功后，将生成以下文件：

- `lib/models/user.g.dart`
- `lib/models/user_profile.g.dart`

### 4. 验证生成

检查生成的文件是否包含JSON序列化代码：

```dart
// user.g.dart 示例内容
part of 'user.dart';

User _$UserFromJson(Map<String, dynamic> json) => User(
      id: json['id'] as int?,
      username: json['username'] as String,
      name: json['name'] as String?,
      // ...
    );

Map<String, dynamic> _$UserToJson(User instance) => <String, dynamic>{
      'id': instance.id,
      'username': instance.username,
      'name': instance.name,
      // ...
    };
```

## 常见问题

### 冲突错误
如果遇到冲突，使用 `--delete-conflicting-outputs` 参数：
```bash
flutter pub run build_runner build --delete-conflicting-outputs
```

### 监听模式
开发时可以使用监听模式，自动重新生成：
```bash
flutter pub run build_runner watch
```

### 清理生成
清理所有生成的文件：
```bash
flutter pub run build_runner clean
```

## 注意事项

1. 确保模型类使用了正确的注解：
   ```dart
   @JsonSerializable()
   class User {
     // ...
   }
   ```

2. 确保包含了part指令：
   ```dart
   part 'user.g.dart';
   ```

3. 确保pubspec.yaml中包含必要的依赖：
   ```yaml
   dependencies:
     json_annotation: ^4.8.1

   dev_dependencies:
     build_runner: ^2.4.7
     json_serializable: ^6.7.1
   ```

## 验证命令

生成后可以运行以下命令验证：
```bash
# 检查是否有语法错误
flutter analyze

# 运行测试
flutter test

# 尝试构建
flutter build apk --debug  # Android
flutter build windows      # Windows
```
