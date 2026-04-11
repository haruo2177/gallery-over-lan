---
name: review
description: コードレビュー（静的解析・ビルド含む）を実行する
disable-model-invocation: true
argument-hint: [対象の説明（省略時はgit diffの変更全体）]
allowed-tools: Bash(git diff*) Bash(git log*) Bash(git status*) Bash(*gradlew*) Bash(*lint*)
---

# コードレビュー

変更されたコードをレビューしてください。

**対象:** $ARGUMENTS

## 手順

### 1. 変更差分の確認
`git diff` で変更内容を確認してください。引数が指定されていない場合は、直近の未コミット変更またはHEADコミットの差分を対象とします。

### 2. 静的解析とビルド
以下を並列で実行してください:

```bash
# ビルド確認
JAVA_HOME="/c/Program Files/Android/Android Studio/jbr" ./gradlew assembleDebug

# Lint実行
JAVA_HOME="/c/Program Files/Android/Android Studio/jbr" ./gradlew lintDebug
```

### 3. コードレビュー

変更差分を読み、以下の観点でレビューしてください:

**正確性:**
- ロジックにバグがないか
- null安全性は確保されているか
- エッジケース（空リスト、境界値、0除算等）の処理

**設計:**
- 既存のアーキテクチャパターンに沿っているか
- 責務の分離は適切か（ViewModel / UseCase / Repository）
- 状態管理は正しいか（StateFlowの更新、recomposition）

**パフォーマンス:**
- 不要なrecompositionが発生しないか
- メモリリークの可能性（コルーチンスコープ、コールバック）
- 重い処理がメインスレッドで実行されていないか

**セキュリティ:**
- ハードコードされた認証情報がないか
- 入力値のバリデーション

**Androidベストプラクティス:**
- Compose の `remember` / `derivedStateOf` の適切な使用
- ライフサイクル対応（`collectAsStateWithLifecycle`等）

### 4. 出力フォーマット

```
## ビルド結果
- assembleDebug: ✅ 成功 / ❌ 失敗（エラー内容）
- lintDebug: ✅ 成功 / ⚠️ 警告あり / ❌ 失敗（エラー内容）

## レビュー結果: ✅ 問題なし / ⚠️ 軽微な指摘あり / ❌ 要修正

## 指摘事項
- 【重大】（あれば: ファイル:行番号 — 内容）
- 【軽微】（あれば: ファイル:行番号 — 内容）

## 良い点
- （あれば記述）
```
