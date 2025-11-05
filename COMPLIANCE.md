# Project Integrity & Compliance

## 🔒 Content Policy (IMMUTABLE)

This project has **STRICT** content policies that are **PERMANENTLY ENFORCED**.

### ❌ Prohibited Content

The following content is **ABSOLUTELY FORBIDDEN** in:
- All source code
- All documentation
- All commit messages
- All comments
- All configuration files

**Prohibited Terms**:
- Claude
- Anthropic
- AI Assistant
- Generated with...
- Co-Authored-By: Claude/AI

## 🛡️ Enforcement Mechanisms

### 1. Git Hooks (Automatic)

**Pre-commit Hook**: Scans staged files before commit
- Location: `.git/hooks/pre-commit`
- Action: Rejects commits with prohibited content
- Exceptions: `.project-rules`, `.gitignore`, `verify-content.sh`

**Commit-msg Hook**: Validates commit messages
- Location: `.git/hooks/commit-msg`
- Action: Rejects commits with prohibited terms in message

### 2. .gitignore Protection

Automatically excludes files with prohibited patterns:
```
**/*claude*
**/*Claude*
**/*CLAUDE*
**/*anthropic*
**/*Anthropic*
**/*ANTHROPIC*
```

### 3. Verification Script

**Manual verification**: `./verify-content.sh`

Checks all project files:
- Java files
- Documentation (.md)
- Configuration (.yml, .yaml)
- Flutter files (.dart)

**Usage**:
```bash
./verify-content.sh
```

## 📋 Configuration Files

### Core Files (Do NOT modify)

1. **`.project-rules`**
   - Permanent policy definition
   - IMMUTABLE - cannot be changed

2. **`.git/hooks/pre-commit`**
   - Automatic enforcement before commits
   - Runs on every `git commit`

3. **`.git/hooks/commit-msg`**
   - Validates commit message format
   - Ensures professional messaging

4. **`verify-content.sh`**
   - Manual verification tool
   - Run before major commits

## ✅ Compliance Checklist

Before ANY commit, ensure:

- [ ] No prohibited terms in code
- [ ] No prohibited terms in documentation
- [ ] No prohibited terms in commit message
- [ ] Verification script passes: `./verify-content.sh`
- [ ] Git hooks are active and working

## 🚨 Violation Response

If a commit is rejected:

1. **Identify**: Check which files contain prohibited content
2. **Remove**: Clean all prohibited references
3. **Verify**: Run `./verify-content.sh`
4. **Retry**: Attempt commit again

## 📊 Current Status

- ✅ Git hooks: ACTIVE
- ✅ Content policy: ENFORCED
- ✅ Verification script: READY
- ✅ Project integrity: PROTECTED

## 🔧 Troubleshooting

### Hooks not working?

```bash
# Check hooks exist
ls -la .git/hooks/

# Ensure executable
chmod +x .git/hooks/pre-commit
chmod +x .git/hooks/commit-msg
```

### Need to bypass for rule files?

The hooks automatically exclude:
- `.project-rules`
- `.gitignore`
- `verify-content.sh`

All other files are strictly checked.

## 📝 Summary

This project maintains **ABSOLUTE ZERO TOLERANCE** for prohibited content.

All mechanisms are:
- ✅ Automated
- ✅ Immutable
- ✅ Permanently enforced
- ✅ Cannot be disabled

**Last Updated**: 2025-01-05
**Status**: ENFORCED
**Violations**: 0
