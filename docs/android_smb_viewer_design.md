# Androidアプリ設計書: Windows共有フォルダ画像ビューア

作成日: 2026-03-15

## 1. 目的とスコープ
本アプリは、LAN内の1台のWindows PCが公開している共有フォルダへAndroidから接続し、画像ファイルのみを快適に閲覧することを目的とする。

### 対象機能
- 接続設定
- 共有フォルダ配下の画像一覧
- フォルダ名検索
- 全画面表示
- スライドショー

### 非対象
- アップロード
- 削除、移動、編集
- 端末への明示保存
- Windows側設定変更支援

### 前提
- Windows側はユーザー名・パスワード認証あり
- LAN内IPはDHCPで動的に変わる

## 2. 要件整理
### 必須
- SMB2/SMB3でWindows共有へ接続できる
- 指定したベースフォルダ配下のサブフォルダ一覧と画像一覧を表示できる
- フォルダ名で検索できる
- 画像を全画面で閲覧し、一定間隔でスライドショー再生できる
- 認証情報を安全寄りに保存できる

### 重要
- 接続先PCのIP変動に耐える
- 大量画像でもUIが固まらない

### 任意
- 複数接続先管理
- Exif表示
- 回転
- 動画対応

## 3. 推奨技術スタック
- 言語: Kotlin
- UI: Jetpack Compose
- 状態管理: ViewModel + StateFlow
- 非同期: Kotlin Coroutines
- 画像表示: Coil
- SMBクライアント: SMBJ
- 設定保存: DataStore
- 秘密情報保護: Android Keystoreを使った暗号化
- 開発環境: Android Studio + Gradle

### 補足
動的IP問題の解決のためにサーバー自動検出を最初から入れたくなるが、MVPではやりすぎ。まずは「接続対象PC名を保存して毎回その名前で接続を試みる」設計を推奨する。

## 4. 全体アーキテクチャ
### レイヤ構成
- Presentation: Compose画面群、ViewModel、UI state、ナビゲーション
- Domain: 接続、フォルダ探索、画像一覧取得、スライドショー制御、検索ロジック
- Data: SMBクライアント実装、キャッシュ、設定保存、認証情報保護
- Infra: ログ、ネットワーク状態監視、タイマー

### 代表クラス例
- UI: `SettingsScreen`, `FolderBrowserScreen`, `ViewerScreen`
- ViewModel: `SettingsViewModel`, `BrowserViewModel`, `ViewerViewModel`
- UseCase: `ConnectUseCase`, `SearchFoldersUseCase`, `StartSlideShowUseCase`
- Repository: `SmbRepository`, `SettingsRepository`, `CredentialRepository`
- Gateway: `SmbClient`, `ThumbnailCache`, `HostResolver`

## 5. 動的IPへの対応方針
### 保存する主キー
1. Windowsコンピューター名（例: `DESKTOP-ABCD123`）
2. 共有名（例: `Photos`）
3. ベースフォルダ相対パス（例: `Family/2025`）
4. 最終成功IPアドレス（補助情報として保持）

### 接続方針
- 通常接続: 保存済みコンピューター名で接続を開始し、成功したIPをキャッシュする
- 再接続: 前回成功IPがあれば最初に短時間で試し、失敗したらコンピューター名で再試行する
- 名前解決失敗時: ユーザーにPC名解決失敗を表示し、PC名再入力またはIP一時入力を許可する
- 初期実装でやらないこと: LAN全体スキャン、ワークグループ一覧、ブロードキャスト探索の常時実行

### 実装メモ
- SMBJはホスト名指定で接続できるため、まずはホスト名接続を基本とする
- ユーザー入力欄は「IPアドレス」ではなく「PC名またはIP」と表記する
- 将来、家庭内ネットワークでホスト名解決が不安定なら、NetBIOS名解決専用の補助実装を後付けできるよう`HostResolver`インターフェースを切る
- SMB実装本体と探索実装は分離する

## 6. 画面設計
### 接続設定画面
- PC名またはIP
- 共有名
- ユーザー名
- パスワード
- ベースフォルダ
- 接続テスト

### フォルダブラウザ画面
- パンくず
- 検索ボックス
- フォルダ一覧
- 画像枚数
- 更新ボタン

### 画像一覧画面
- サムネイルグリッド
- ソート
- フォルダ情報

### ビューア画面
- 全画面画像
- 前後移動
- スライドショー開始/停止
- 間隔設定

### エラー表示
- 認証失敗
- 共有未到達
- パスなし
- 権限不足

## 7. 主要ユースケース
1. 初回設定: PC名・共有名・認証情報・ベースフォルダを入力し、接続テスト成功後に保存
2. フォルダ検索: ベースフォルダ配下のフォルダ名をインメモリ検索
3. 画像閲覧: 選択フォルダの画像一覧を取得し、サムネイル表示
4. スライドショー: 現在フォルダの画像配列を一定間隔で順送り表示

### UseCase案
- `TestConnectionUseCase`
- `BrowseFoldersUseCase`
- `SearchFoldersUseCase`
- `ListImagesUseCase`
- `StartSlideShowUseCase`

## 8. パッケージ構成案
```text
app/
  ui/
    settings/
    browser/
    viewer/
    components/
  domain/
    model/
    usecase/
  data/
    smb/
    settings/
    security/
    cache/
  core/
    dispatchers/
    result/
    logging/
    network/
```

## 9. データ保持とセキュリティ
- PC名、共有名、ベースフォルダ、表示設定、スライドショー間隔はDataStoreに保存
- パスワードは平文で保存しない
- Android Keystoreで管理する鍵により暗号化して保存
- Windows資格情報の表示はマスクし、再表示はしない
- アプリ権限は最小限にする
- SMB1はサポート対象外、SMB2/SMB3のみ

## 10. パフォーマンス設計
- フォルダ一覧取得と画像一覧取得は`Dispatchers.IO`で実行
- サムネイルはアプリキャッシュ領域へ保存
- 一覧画面では画面内に必要な分だけ遅延読み込み
- 元画像の全量読み込みは避ける
- スライドショーは次の1枚だけ先読み程度から始める

## 11. エラー処理とテスト
### エラー表示方針
- 認証失敗: ユーザー名またはパスワードを確認してください
- 共有未到達: PCに接続できません。PC名またはネットワークを確認してください
- パス不正: 指定フォルダが見つかりません
- 読み込み失敗: 一部画像を開けませんでした

### テスト
- ユニットテスト: UseCase、検索ロジック、スライドショー状態遷移、資格情報保存ラッパー
- 結合テスト: 実Windows共有への接続、認証失敗、IP変更後の再接続、共有名誤り、フォルダ不存在
- 実機確認: Wi-Fi切替、画面回転、バックグラウンド復帰、スリープ復帰、数千枚フォルダ

## 12. 実装順序
1. P0: 接続設定画面、接続テスト、単一フォルダの画像一覧表示
2. P1: フォルダブラウザ、フォルダ名検索、全画面ビューア
3. P2: スライドショー、サムネイルキャッシュ、エラー改善
4. P3: ホスト名解決の補助実装、複数接続先対応、細かなUX改善

## 13. 実装者向けの判断ポイント
- MVPでは「共有フォルダ1つ + ベースフォルダ1つ」に絞る
- ホスト名解決が不安定な環境でも使いたいなら、`HostResolver`抽象を切って後付け拡張できる形にする
- フォルダ検索はまずクライアント側のインメモリ検索でよい
- スライドショーはViewer側の状態機械として実装し、再生制御を明示的に持つ
- アプリが見る専用であることをUIにも反映し、編集系ボタンを置かない

## 参考資料
- Android Developers: Jetpack Compose
- Android Developers: Kotlin coroutines on Android
- Android Developers: DataStore
- Android Developers: Android Keystore system
- Android Developers: Local network permission
- Android Developers: NsdManager / Network Service Discovery
- GitHub: hierynomus/smbj
