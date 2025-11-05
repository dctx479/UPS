// MongoDB Initialization Script for Profile and Behavior Services

// Switch to admin database for authentication
db = db.getSiblingDB('admin');

// Create application user with readWrite permissions
db.createUser({
  user: 'userprofile',
  pwd: 'userprofile123',
  roles: [
    {
      role: 'readWrite',
      db: 'userprofile'
    }
  ]
});

// Switch to userprofile database
db = db.getSiblingDB('userprofile');

// Create collections with validation
db.createCollection('user_profiles', {
  validator: {
    $jsonSchema: {
      bsonType: 'object',
      required: ['userId', 'username'],
      properties: {
        userId: {
          bsonType: 'long',
          description: 'User ID - required'
        },
        username: {
          bsonType: 'string',
          description: 'Username - required'
        },
        profileScore: {
          bsonType: ['double', 'null'],
          minimum: 0,
          maximum: 100,
          description: 'Profile score between 0-100'
        }
      }
    }
  }
});

db.createCollection('user_behaviors', {
  validator: {
    $jsonSchema: {
      bsonType: 'object',
      required: ['userId', 'behaviorType', 'timestamp'],
      properties: {
        userId: {
          bsonType: 'long',
          description: 'User ID - required'
        },
        behaviorType: {
          bsonType: 'string',
          enum: ['BROWSE', 'SEARCH', 'CLICK', 'PURCHASE', 'SHARE', 'FAVORITE', 'COMMENT'],
          description: 'Behavior type - required'
        },
        timestamp: {
          bsonType: 'date',
          description: 'Behavior timestamp - required'
        }
      }
    }
  }
});

db.createCollection('user_tags');

// Create indexes for user_profiles
db.user_profiles.createIndex({ userId: 1 }, { unique: true, name: 'idx_userId' });
db.user_profiles.createIndex({ username: 1 }, { name: 'idx_username' });
db.user_profiles.createIndex({ updateTime: -1 }, { name: 'idx_updateTime' });
db.user_profiles.createIndex({ profileScore: -1 }, { name: 'idx_profileScore' });
db.user_profiles.createIndex({ userId: 1, updateTime: -1 }, { name: 'idx_userId_updateTime' });

// Create indexes for user_behaviors
db.user_behaviors.createIndex({ userId: 1 }, { name: 'idx_userId' });
db.user_behaviors.createIndex({ behaviorType: 1 }, { name: 'idx_behaviorType' });
db.user_behaviors.createIndex({ timestamp: -1 }, { name: 'idx_timestamp' });
db.user_behaviors.createIndex({ userId: 1, timestamp: -1 }, { name: 'idx_userId_timestamp' });
db.user_behaviors.createIndex({ userId: 1, behaviorType: 1 }, { name: 'idx_userId_behaviorType' });

// Create indexes for user_tags
db.user_tags.createIndex({ userId: 1 }, { unique: true, name: 'idx_userId' });
db.user_tags.createIndex({ 'tags.tagName': 1 }, { name: 'idx_tagName' });
db.user_tags.createIndex({ 'tags.category': 1 }, { name: 'idx_category' });

// Insert demo data
db.user_profiles.insertOne({
  userId: NumberLong(1),
  username: 'admin',
  basicInfo: {
    gender: 'UNKNOWN',
    ageRange: 'UNKNOWN',
    location: 'Unknown'
  },
  behaviorSummary: {
    totalBehaviors: 0,
    activeDays: 0,
    lastActiveTime: new Date(),
    frequentActions: []
  },
  preferenceAnalysis: {
    interests: [],
    categories: []
  },
  valueAssessment: {
    profileQuality: 'INCOMPLETE',
    consumptionLevel: 'UNKNOWN',
    preferenceAnalysis: {},
    avgOrderValue: 0.0,
    feedingMethod: 'UNKNOWN',
    teachability: 'UNKNOWN'
  },
  profileScore: 50.0,
  createTime: new Date(),
  updateTime: new Date()
});

print('MongoDB initialization completed successfully!');
