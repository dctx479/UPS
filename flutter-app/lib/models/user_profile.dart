import 'package:json_annotation/json_annotation.dart';

part 'user_profile.g.dart';

@JsonSerializable()
class UserProfile {
  final String? id;
  final int userId;
  final String username;
  final DigitalBehavior? digitalBehavior;
  final CoreNeeds? coreNeeds;
  final ValueAssessment? valueAssessment;
  final StickinessAndLoyalty? stickinessAndLoyalty;
  final double? profileScore;
  final DateTime? createTime;
  final DateTime? updateTime;

  UserProfile({
    this.id,
    required this.userId,
    required this.username,
    this.digitalBehavior,
    this.coreNeeds,
    this.valueAssessment,
    this.stickinessAndLoyalty,
    this.profileScore,
    this.createTime,
    this.updateTime,
  });

  factory UserProfile.fromJson(Map<String, dynamic> json) =>
      _$UserProfileFromJson(json);
  Map<String, dynamic> toJson() => _$UserProfileToJson(this);

  String getScoreLevel() {
    if (profileScore == null) return '未评分';
    if (profileScore! >= 80) return '优秀';
    if (profileScore! >= 60) return '良好';
    if (profileScore! >= 40) return '中等';
    return '一般';
  }
}

@JsonSerializable()
class DigitalBehavior {
  final List<String>? productCategories;
  final String? infoAcquisitionHabit;
  final String? purchaseDecisionPreference;
  final List<String>? brandPreferences;

  DigitalBehavior({
    this.productCategories,
    this.infoAcquisitionHabit,
    this.purchaseDecisionPreference,
    this.brandPreferences,
  });

  factory DigitalBehavior.fromJson(Map<String, dynamic> json) =>
      _$DigitalBehaviorFromJson(json);
  Map<String, dynamic> toJson() => _$DigitalBehaviorToJson(this);
}

@JsonSerializable()
class CoreNeeds {
  final List<String>? topConcerns;
  final String? decisionPainPoint;

  CoreNeeds({
    this.topConcerns,
    this.decisionPainPoint,
  });

  factory CoreNeeds.fromJson(Map<String, dynamic> json) =>
      _$CoreNeedsFromJson(json);
  Map<String, dynamic> toJson() => _$CoreNeedsToJson(this);
}

@JsonSerializable()
class ValueAssessment {
  final String? profileQuality;
  final Map<String, dynamic>? preferenceAnalysis;
  final String? feedingMethod;
  final String? teachability;

  ValueAssessment({
    this.profileQuality,
    this.preferenceAnalysis,
    this.feedingMethod,
    this.teachability,
  });

  factory ValueAssessment.fromJson(Map<String, dynamic> json) =>
      _$ValueAssessmentFromJson(json);
  Map<String, dynamic> toJson() => _$ValueAssessmentToJson(this);
}

@JsonSerializable()
class StickinessAndLoyalty {
  final List<String>? concerns;
  final String? painPoint;
  final double? loyaltyScore;

  StickinessAndLoyalty({
    this.concerns,
    this.painPoint,
    this.loyaltyScore,
  });

  factory StickinessAndLoyalty.fromJson(Map<String, dynamic> json) =>
      _$StickinessAndLoyaltyFromJson(json);
  Map<String, dynamic> toJson() => _$StickinessAndLoyaltyToJson(this);
}
