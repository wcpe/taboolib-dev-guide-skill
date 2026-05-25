# Pre-Generation Verification Checklist

**Purpose**: Ensure all necessary information is gathered before generating TabooLib code.

Use this checklist before generating any code to prevent API hallucination and ensure compatibility.

---

## Phase 1: Project Context

### User's Project Structure
- [ ] Read `build.gradle.kts` to detect TabooLib version
- [ ] Read `build.gradle.kts` to detect Kotlin version
- [ ] Read `build.gradle.kts` to detect Java version
- [ ] Identify project package name (e.g., `com.example.plugin`)
- [ ] Check if using IoC (`database-ioc` module installed)
- [ ] Check if `relocate` configured (required for IoC)
- [ ] Identify existing module structure (command/, service/, listener/, etc.)

**Commands**:
```bash
# Find build.gradle.kts
find . -name "build.gradle.kts"

# Check TabooLib version
grep "taboolib =" build.gradle.kts

# Check installed modules
grep "install(" build.gradle.kts

# Find package structure
find src/main/kotlin -type d
```

---

## Phase 2: Existing Code Style

### Import Style
- [ ] Check if using wildcard imports (`import taboolib.common.platform.*`)
- [ ] Check if using explicit imports (`import taboolib.common.platform.Awake`)
- [ ] Extract most common import style from existing files

**Commands**:
```bash
# Check import style
grep -r "^import taboolib" src/ | head -20
```

### Naming Conventions
- [ ] Check class naming (PascalCase, suffixes like Command/Service/Listener)
- [ ] Check object vs class usage
- [ ] Check field naming (camelCase)
- [ ] Check file naming convention

**Commands**:
```bash
# Find existing commands
find src/ -name "*Command.kt"

# Find existing services
find src/ -name "*Service.kt"
```

### Code Formatting
- [ ] Check indentation (spaces vs tabs, 2 vs 4 spaces)
- [ ] Check brace style (same line vs new line)
- [ ] Check string quote style (single vs double)

---

## Phase 3: API Verification

### Annotations
- [ ] Verify `@Awake` package: `grep -r "@Awake" src/`
- [ ] Verify `@CommandHeader` package: `grep -r "@CommandHeader" src/`
- [ ] Verify `@SubscribeEvent` package: `grep -r "@SubscribeEvent" src/`
- [ ] Verify `@Config` package: `grep -r "@Config" src/`
- [ ] Verify `@Component` package (if using IoC): `grep -r "@Component" src/`

### Common Classes
- [ ] Verify `Configuration` usage: `grep -r "Configuration" src/`
- [ ] Verify `submit` usage: `grep -r "submit" src/`
- [ ] Verify `CommandContext` usage: `grep -r "CommandContext" src/`

### Package Paths
- [ ] Extract actual package paths from existing imports
- [ ] Note any custom or unusual package usage
- [ ] Check for version-specific package differences

---

## Phase 4: Feature-Specific Checks

### For Commands
- [ ] Check existing command structure (simple vs complex)
- [ ] Check if using IoC for service injection
- [ ] Check argument parsing patterns
- [ ] Check permission handling patterns

### For Config
- [ ] Check config file locations (`src/main/resources/`)
- [ ] Check config access patterns (direct vs service)
- [ ] Check reload mechanism usage

### For IoC
- [ ] Verify `database-ioc` module installed
- [ ] Verify `relocate` configured
- [ ] Check existing bean definitions
- [ ] Check dependency injection patterns (field vs constructor)

### For Listeners
- [ ] Check existing listener structure (object vs class)
- [ ] Check event priority usage
- [ ] Check async handling patterns

### For Scheduler
- [ ] Check existing scheduler usage
- [ ] Check async/sync patterns
- [ ] Check task cancellation patterns

### For Database
- [ ] Check if using EasyQuery
- [ ] Check entity definitions
- [ ] Check repository patterns
- [ ] Note: Recommend `$taboolib-corelib-easyquery-persistence-standards` for detailed implementation

---

## Phase 5: Compatibility Checks

### TabooLib Version
- [ ] Confirm APIs exist in user's TabooLib version
- [ ] Check for deprecated APIs
- [ ] Note version-specific features

### Module Dependencies
- [ ] Verify all required modules installed
- [ ] Check for missing dependencies
- [ ] Verify module compatibility

### Platform Compatibility
- [ ] Check if Bukkit-only or multi-platform
- [ ] Verify platform-specific APIs

---

## Phase 6: Risk Assessment

### API Uncertainty
- [ ] List any APIs that couldn't be verified
- [ ] Mark uncertain APIs as "unverified - check project"
- [ ] Provide fallback options for uncertain APIs

### Breaking Changes
- [ ] Note any potential breaking changes
- [ ] Warn about version-specific issues
- [ ] Suggest verification steps

---

## Checklist Summary

**Before generating code, ensure**:

✅ **Project Context**:
- TabooLib version known
- Package structure identified
- Module setup verified

✅ **Code Style**:
- Import style matched
- Naming conventions matched
- Formatting matched

✅ **API Verification**:
- All annotations verified
- All classes verified
- All packages verified

✅ **Feature-Specific**:
- Feature requirements checked
- Existing patterns identified
- Dependencies verified

✅ **Compatibility**:
- Version compatibility confirmed
- Module dependencies satisfied
- Platform compatibility checked

✅ **Risk Assessment**:
- Uncertain APIs marked
- Fallbacks provided
- Verification steps included

---

## If Checks Fail

**Missing Information**:
- Ask user for clarification
- Provide specific questions
- Suggest commands to run

**API Uncertainty**:
- Mark as "unverified - check project"
- Provide conservative fallback
- Include verification steps in output

**Version Mismatch**:
- Warn user about potential issues
- Suggest checking wiki/source code
- Provide version-specific notes

---

## Output Template

After completing checklist, include in response:

```markdown
## Verification Summary

**Project Context**:
- TabooLib Version: [version]
- Kotlin Version: [version]
- Package: [package]
- IoC Enabled: [yes/no]

**API Verification**:
- [✅/❌] All annotations verified
- [✅/❌] All classes verified
- [✅/❌] All packages verified

**Unverified APIs** (if any):
- [API name] - marked as "unverified - check project"

**Recommendations**:
- [Any specific recommendations]
```

---

## References

- Related cards: All cards
- Related maps: `annotation_map.md`, `package_map.md`, `common_class_map.md`
- Related tests: `troubleshooting_checklist.md`
