import 'package:json_annotation/json_annotation.dart';

part 'user.g.dart';

@JsonSerializable()
class User {
  final int? id;
  final String username;
  final String? name;
  final int? age;
  final int? gender;
  final String? phone;
  final String? email;
  final String? region;
  final String? zodiacSign;
  final String? occupation;
  final String? incomeLevel;
  final double? debtToAssetRatio;
  final int? status;
  final DateTime? createTime;
  final DateTime? updateTime;

  User({
    this.id,
    required this.username,
    this.name,
    this.age,
    this.gender,
    this.phone,
    this.email,
    this.region,
    this.zodiacSign,
    this.occupation,
    this.incomeLevel,
    this.debtToAssetRatio,
    this.status,
    this.createTime,
    this.updateTime,
  });

  factory User.fromJson(Map<String, dynamic> json) => _$UserFromJson(json);
  Map<String, dynamic> toJson() => _$UserToJson(this);

  String getGenderText() {
    switch (gender) {
      case 1:
        return '男';
      case 2:
        return '女';
      default:
        return '未知';
    }
  }

  String getStatusText() {
    return status == 1 ? '正常' : '禁用';
  }
}
