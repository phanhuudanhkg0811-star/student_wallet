# Contribution Guidelines

Welcome to **StudentWallet (Group 07 – FriendForever)**. To keep collaboration smooth across all 5 team members and avoid merge conflicts, every contributor **must** follow these rules.

---

## 1. Branching Strategy (Work on Separate Branches)

**Direct pushes to `main` or `dev` are strictly prohibited.**
- `main`: Stable release branch (only receives tested merges from `dev`).
- `dev`: Shared integration branch where all 5 team members merge completed features.
- Always create a new branch from the latest `dev` before making any changes.

### Branch Naming Convention

Use lowercase letters and hyphens (`kebab-case`):

| Purpose | Pattern | Examples |
| :--- | :--- | :--- |
| **New feature** | `feat/<module>-<short-description>` | `feat/transaction-numeric-keypad`<br>`feat/scanner-vietqr-reader`<br>`feat/group-min-cash-flow` |
| **Bug fix** | `fix/<module>-<short-description>` | `fix/scanner-ocr-crash`<br>`fix/budget-progress-rounding` |
| **Refactoring** | `refactor/<module>-<short-description>` | `refactor/core-room-entities` |
| **Documentation** | `docs/<short-description>` | `docs/update-feature-14-spec` |
| **Build / CI / Config** | `chore/<short-description>` | `chore/add-room-dependencies` |

### Standard Branch Workflow

```bash
# 1. Sync with the latest dev branch
git checkout dev
git pull origin dev

# 2. Create your feature branch (always use a separate branch, e.g. feat/...)
git checkout -b feat/transaction-numeric-keypad

# 3. Work, commit frequently using Conventional Commits, then push
git push -u origin feat/transaction-numeric-keypad
```

---

## 2. Conventional Commits Rules

Every commit message **must** follow the [Conventional Commits](https://www.conventionalcommits.org/) specification:

```text
<type>(<scope>): <imperative summary in English>

[optional body explaining why, not what]
```

### Allowed Types

- `feat`: A new feature or user-facing capability
- `fix`: A bug fix
- `docs`: Documentation changes (`README.md`, `docs/`, comments)
- `refactor`: Code restructuring that neither fixes a bug nor adds a feature
- `style`: Formatting, whitespace, missing semicolons (no logic changes)
- `test`: Adding or updating unit/instrumented tests
- `chore`: Maintenance tasks, dependency updates, Gradle config
- `ci`: Changes to GitHub Actions workflows (`.github/workflows/`)

### Recommended Scopes

Use the module or area you modified:
- `app`, `core`, `transaction`, `scanner`, `budget`, `group`, `security`, `docs`, `ci`, `deps`

### Examples

```text
# Good
feat(transaction): implement custom financial numeric keypad
feat(group): add greedy min-cash-flow debt optimizer
fix(scanner): handle empty OCR bounding box safely
test(group): verify 4-mode expense split rounds to zero difference
docs(readme): rewrite README in English with tech stack details
chore(core): add Room and DataStore dependencies to version catalog

# Bad (Do NOT commit messages like these)
update code
fix bug
lam xong man hinh roi
wip
```

---

## 3. Module Ownership & Anti-Conflict Rules

1. **Stay inside your module:** Only edit files inside your assigned `:feature:<name>` module (`transaction`, `scanner`, `budget`, `group`, or `security`).
2. **Never cross-import between features:** Feature modules must only depend on `:core`, never on another `:feature:*` module.
3. **Coordinate `:core` and `:app` edits:** If you need to add a shared Room `@Entity`, `@Dao`, domain model in `:core`, or modify navigation in `:app`, notify the team first and open a small dedicated PR (e.g., `feat/core-transaction-entity`) so everyone can pull the contract early.
4. **Prefix string resources:** Place feature-specific XML strings inside your feature module (`feature/<name>/src/main/res/values/strings_<name>.xml`) with your module name as prefix (e.g., `transaction_amount_hint`) to avoid resource collisions.

---

## 4. Pull Request & CI Checklist

Before opening a Pull Request on GitHub:

1. Rebase or merge the latest `dev` into your `feat/...` branch and resolve any conflicts locally.
2. Run tests and build verification locally:
   ```bash
   ./gradlew testDebugUnitTest assembleDebug
   ```
3. Open a Pull Request targeting **`dev`** and wait for the **Android CI** check (`Unit Test & Build Debug APK`) to turn **green**.
4. Request review from at least **1 teammate** before merging.
5. Delete the merged `feat/...` branch after the PR is merged to keep the repository clean.
