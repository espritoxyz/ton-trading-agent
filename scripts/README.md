# Import KANBAN.MD Tasks to GitHub Issues

This script automates the creation of GitHub issues from tasks defined in `KANBAN.MD`.

## Prerequisites

1. **GitHub CLI** must be installed:
   ```bash
   # macOS
   brew install gh
   
   # Linux
   sudo apt install gh
   
   # Windows
   winget install GitHub.cli
   ```

2. **Authenticate with GitHub CLI**:
   ```bash
   gh auth login
   ```

## Usage

### Dry Run (Preview)

To preview what issues will be created without actually creating them:

```bash
./scripts/import-kanban-to-issues.sh --dry-run
```

### Create Issues

To create all issues:

```bash
./scripts/import-kanban-to-issues.sh
```

## What It Does

The script will:

1. ✅ Create necessary labels:
   - `kanban:todo` - For To Do tasks
   - `kanban:backlog` - For Backlog tasks
   - `mvp` - For MVP features
   - `recipe` - For recipe implementations
   - `2026` - For 2026 planned features

2. 📋 Create **22 TO DO issues** including:
   - User Assets Widget (2 issues)
   - User Orders Widget (2 issues) 
   - User Transactions (2 issues)
   - User Account - Sign up (2 issues)
   - Deposit Assets (2 issues)
   - Withdraw Assets (1 issue)
   - Chat Notifications (2 issues)
   - Analytics (1 issue)
   - Infrastructure (2 issues)
   - Recipe implementations: lookup_assets, send_token_to_address, swap_token_to_ton, decide_jetton_master (4 issues)
   - Smart trading recipes: smart_buy, smart_sell (2 issues)

3. 📦 Create **20 BACKLOG issues** including:
   - Liquidity operations (2 issues)
   - Staking and deposits (3 issues)
   - NFT operations (3 issues)
   - Wallet management (2 issues)
   - Monitoring and analytics (3 issues)
   - Advanced swaps (2 issues)
   - Smart contract interactions (2 issues)
   - Social features (2 issues)

## Adding Issues to GitHub Project

After creating the issues, you can add them to your GitHub Project in several ways:

### Option 1: Manual Addition
1. Go to your GitHub Project
2. Click "Add items"
3. Select the newly created issues

### Option 2: Auto-add Rules
Set up automation rules in your GitHub Project:
1. Go to Project Settings → Workflows
2. Create an "Auto-add to project" rule:
   - When: Issue is opened
   - Filter: has label `kanban:todo` OR `kanban:backlog`
   - Then: Add to project

### Option 3: GitHub Project Automation via Settings
Alternatively, use the GitHub web interface:
1. Navigate to your GitHub Project
2. Click on "..." (menu) → Settings → Workflows
3. Enable "Auto-add to project" workflow
4. Configure filters to auto-add issues with specific labels

## Issue Structure

Each issue includes:
- **Title**: Clear, descriptive title in Russian
- **Body**: Detailed description with:
  - Description of the task
  - Examples (where applicable)
  - Source reference (KANBAN.MD)
  - Assignees (where specified in KANBAN.MD)
- **Labels**: Appropriate labels for filtering and organization

## Notes

- ✅ Issues marked as "Done" in KANBAN.MD are NOT created (they're already completed)
- 👀 Issues marked with "2026" get the `2026` label
- 🏷️ Labels are automatically created if they don't exist
- 📝 All descriptions preserve the original Russian text and context

## Troubleshooting

### "gh: command not found"
Install GitHub CLI using the installation instructions above.

### "gh: authentication required"
Run `gh auth login` and follow the prompts to authenticate.

### "Error creating issue"
Check that you have write access to the repository. You may need to fork the repository first.

### "Label already exists"
This is normal. The script uses `--force` flag to update existing labels.
