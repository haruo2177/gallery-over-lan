# Gallery Over LAN

LAN内のWindows PCの共有フォルダにSMB接続し、画像を閲覧するAndroidアプリです。

## 画面構成

| 画面名 | 説明 |
|--------|------|
| **接続設定画面** (Settings) | SMB接続先のPC名・認証情報を入力・保存する画面 |
| **共有一覧画面** (Shares) | 接続先PCの共有フォルダ一覧を表示する画面 |
| **フォルダブラウザ画面** (Browse) | フォルダ階層をナビゲーションし、サムネイル一覧を表示する画面 |
| **ビューア画面** (Viewer) | 1枚の画像を全画面表示する画面。スライドショー機能付き |

## 機能

- **SMB2/SMB3接続**: Windows共有フォルダにユーザー名・パスワード認証で接続
- **フォルダブラウザ画面**: サブフォルダの階層ナビゲーション（パンくず付き）、フォルダ名検索
- **フォルダブラウザ画面**: サムネイルグリッド表示（端末密度に応じた動的サイズキャッシュ＋プリフェッチ）、名前/日付/サイズでソート
- **ビューア画面**: スワイプまたは左右1/4タップで前後の画像へ移動、中央タップでコントロール表示切替
- **スライドショー**: 1〜30秒間隔で自動再生
- **セキュリティ**: Android Keystoreによるパスワード暗号化保存

## 技術スタック

| カテゴリ | 技術 |
|---------|------|
| 言語 | Kotlin |
| UI | Jetpack Compose + Material 3 |
| 状態管理 | ViewModel + StateFlow |
| 非同期 | Kotlin Coroutines |
| 画像表示 | Coil (カスタムSMB Fetcher) |
| SMBクライアント | SMBJ 0.13.0 |
| 設定保存 | DataStore |
| 暗号化 | Android Keystore (AES-GCM) |
| DI | Hilt |
| ナビゲーション | Navigation Compose |

## セットアップ

### 必要なもの

- Android Studio (最新版推奨)
- Android実機 (API 26以上 = Android 8.0以上)
- LAN内のWindows PC（共有フォルダ設定済み）

### ビルド & 実行

1. Android Studioでこのプロジェクトを開く
   - **File > Open** → このリポジトリのルートフォルダを選択
   - Gradle Syncが完了するまで待つ

2. 実機を接続する

   **USB接続の場合:**
   - Androidスマホの **設定 > 開発者向けオプション > USBデバッグ** を有効化
   - USBケーブルでPCに接続
   - 「USBデバッグを許可しますか？」→ OK

   **Wi-Fi接続の場合 (Android 11以上):**
   - スマホの **設定 > 開発者向けオプション > ワイヤレスデバッグ** を有効化
   - Android Studioの **Running Devices** パネル > **Pair Devices Using Wi-Fi**

3. ビルド＆実行
   - Android Studio上部のデバイスドロップダウンでスマホが表示されることを確認
   - **Run (緑の▶ボタン)** を押す

### リリースビルド（APK）のインストール

Android Studioなしでスマホに直接インストールする方法です。

1. リリースAPKをビルドする（コマンドラインの場合）:
   ```bash
   ./gradlew assembleRelease
   ```
   APKは `app/build/outputs/apk/release/app-release.apk` に出力されます。

2. APKファイルをスマホに転送する（USB、Google Drive、メール等）

3. スマホ側で「提供元不明のアプリ」のインストールを許可する:
   - **設定 > アプリ > 特別なアプリアクセス > 不明なアプリのインストール**
   - 転送に使ったアプリ（Files、Chrome等）を選び「許可」

4. スマホのファイルマネージャーでAPKファイルをタップしてインストール

### Windows PC側の準備

1. 共有するフォルダ（例: `C:\Photos`）に画像を入れる
2. フォルダを右クリック > **プロパティ > 共有 > 共有...**
3. ユーザーを追加してアクセス許可（読み取り）を設定
4. PC名を確認: コマンドプロンプトで `hostname` を実行

### アプリの使い方

1. **接続設定画面** で以下を入力:
   - **PC名またはIP**: Windowsの `hostname` の結果（例: `DESKTOP-ABCD123`）、またはIPアドレス
   - **共有名**: 共有フォルダの名前（例: `Photos`）
   - **ユーザー名 / パスワード**: Windowsのログイン情報
   - **ベースフォルダ**: 共有内のサブフォルダ（任意、空でもOK）
2. 「**接続テスト**」→ 成功を確認 →「**保存**」
3. 「**画像を見る**」でフォルダブラウザへ
4. フォルダをタップして移動、「このフォルダの画像を見る」で画像一覧へ
5. サムネイルタップで全画面ビューア、▶ボタンでスライドショー

## プロジェクト構成

```
app/src/main/java/com/example/galleryoverlan/
├── core/           # 基盤ユーティリティ
│   ├── dispatchers/  # コルーチンディスパッチャ抽象
│   ├── logging/      # ログ
│   ├── network/      # ネットワーク状態監視
│   └── result/       # AppResult型
├── data/           # データ層
│   ├── cache/        # サムネイルキャッシュ
│   ├── security/     # 認証情報の暗号化保存
│   ├── settings/     # 設定のDataStore保存
│   └── smb/          # SMBJラッパー, HostResolver
├── di/             # Hilt DIモジュール
├── domain/         # ドメイン層
│   ├── model/        # データモデル
│   └── usecase/      # ユースケース
└── ui/             # プレゼンテーション層
    ├── browser/      # フォルダブラウザ画面
    ├── components/   # 共通UIコンポーネント
    ├── navigation/   # ナビゲーション
    ├── settings/     # 接続設定画面
    ├── theme/        # Material 3テーマ
    └── viewer/       # 画像一覧 & 全画面ビューア
```

## 設計ドキュメント

詳細な設計は [docs/android_smb_viewer_design.md](docs/android_smb_viewer_design.md) を参照。
