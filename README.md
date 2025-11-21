# Universal Database Manager (UDB Manager)

データベースの接続、閲覧、操作を直感的に行えるWebアプリケーションです。SQLの知識が少ないユーザーでもデータの取得・編集・閲覧が可能です。

## 📋 目次

- [機能概要](#機能概要)
- [技術スタック](#技術スタック)
- [必要な環境](#必要な環境)
- [インストール手順](#インストール手順)
- [実行方法](#実行方法)
- [使い方](#使い方)
- [プロジェクト構成](#プロジェクト構成)
- [トラブルシューティング](#トラブルシューティング)

## 🚀 機能概要

### Phase 1 (MVP) - 実装済み

- ✅ **データベース接続管理**
  - MySQL, PostgreSQL への接続
  - 接続情報の保存・編集・削除
  - 接続テスト機能
  - パスワードの暗号化保存

- ✅ **データベース構造の閲覧**
  - テーブル一覧表示
  - テーブル詳細情報（カラム、データ型、制約など）
  - レコード件数の表示

- ✅ **データ閲覧機能**
  - テーブルデータのグリッド表示
  - ページネーション機能
  - ソート・フィルタリング機能

- ✅ **SQL実行機能**
  - SQLエディタ（シンタックスハイライト対応）
  - クエリ実行と結果表示
  - 実行時間の表示

## 🛠 技術スタック

### バックエンド
- **Java 17**
- **Spring Boot 3.2.0**
- **Maven**
- **H2 Database** (接続情報管理用)
- **MySQL / PostgreSQL** (接続先データベース)

### フロントエンド
- **React 18**
- **Material-UI**
- **AG-Grid** (データグリッド)
- **Monaco Editor** (SQLエディタ)

## 💻 必要な環境

Windows 11 で動作確認済み

### 必須ソフトウェア

1. **Java 17 以上**
   - ダウンロード: https://adoptium.net/

2. **Maven 3.6 以上**
   - ダウンロード: https://maven.apache.org/download.cgi

3. **Node.js 16 以上**
   - ダウンロード: https://nodejs.org/

### オプション（接続先データベース）

- **MySQL** または **PostgreSQL** のインストール
- テスト用のデータベースとテーブルの準備

## 📥 インストール手順

### 1. リポジトリのクローン

```bash
git clone https://github.com/HackathonTeamC/2025-hackathon-demo.git
cd 2025-hackathon-demo
git checkout reitetsu
```

### 2. フロントエンドの依存関係をインストール

```bash
cd frontend
install.bat
```

または

```bash
cd frontend
npm install
```

## ▶️ 実行方法

### 方法1: 一括起動（推奨）

**ルートディレクトリで実行:**

```bash
start-all.bat
```

このバッチファイルは以下を実行します：
- バックエンドサーバーを別ウィンドウで起動
- フロントエンドサーバーを別ウィンドウで起動

### 方法2: 個別に起動

#### バックエンドの起動

**`backend/` ディレクトリで実行:**

```bash
cd backend
start-backend.bat
```

バックエンドは http://localhost:8080 で起動します。

#### フロントエンドの起動

**`frontend/` ディレクトリで実行:**

```bash
cd frontend
start-frontend.bat
```

フロントエンドは http://localhost:3000 で起動します。

## 📖 使い方

### 1. データベース接続の追加

1. アプリケーションを起動 (http://localhost:3000)
2. 「Connections」画面で「New Connection」ボタンをクリック
3. 接続情報を入力:
   - Connection Name: 任意の名前
   - Database Type: MYSQL または POSTGRESQL
   - Host: データベースのホスト名 (例: localhost)
   - Port: ポート番号 (MySQL: 3306, PostgreSQL: 5432)
   - Database Name: 接続先のデータベース名
   - Username: ユーザー名
   - Password: パスワード
4. 「Test Connection」で接続テスト
5. 「Save」で保存

### 2. テーブル構造の閲覧

1. サイドバーから接続を選択
2. 「Explorer」をクリック
3. 左側のリストからテーブルを選択
4. テーブルの詳細情報（カラム、データ型など）が右側に表示されます

### 3. データの閲覧

1. 「Explorer」でテーブルを選択
2. 「Data Grid」をクリック
3. テーブルのデータがグリッド形式で表示されます
4. ページネーション、ソート、フィルタリングが利用可能

### 4. SQLクエリの実行

1. 「SQL Editor」をクリック
2. SQLクエリを入力
3. 「Execute」ボタンでクエリを実行
4. 結果が下部に表示されます

## 📁 プロジェクト構成

```
2025-hackathon-demo/
├── backend/                    # Spring Boot バックエンド
│   ├── src/
│   │   └── main/
│   │       ├── java/
│   │       │   └── com/udb/manager/
│   │       │       ├── controller/    # REST API エンドポイント
│   │       │       ├── service/       # ビジネスロジック
│   │       │       ├── model/         # エンティティ
│   │       │       ├── repository/    # データアクセス
│   │       │       ├── dto/           # データ転送オブジェクト
│   │       │       ├── config/        # 設定クラス
│   │       │       └── exception/     # 例外処理
│   │       └── resources/
│   │           └── application.yml    # アプリケーション設定
│   ├── pom.xml                        # Maven 依存関係
│   ├── start-backend.bat              # ← ここで実行
│   ├── build.bat                      # ← ここで実行
│   └── README.md
│
├── frontend/                   # React フロントエンド
│   ├── src/
│   │   ├── components/        # React コンポーネント
│   │   │   ├── Sidebar.js
│   │   │   ├── ConnectionManager.js
│   │   │   ├── DatabaseExplorer.js
│   │   │   ├── DataGrid.js
│   │   │   └── SQLEditor.js
│   │   ├── services/          # API クライアント
│   │   │   └── api.js
│   │   ├── types/             # 型定義
│   │   │   └── index.js
│   │   ├── App.js             # メインアプリケーション
│   │   └── index.js           # エントリーポイント
│   ├── public/
│   │   └── index.html
│   ├── package.json           # npm 依存関係
│   ├── install.bat            # ← 初回のみ実行
│   ├── start-frontend.bat     # ← ここで実行
│   └── README.md
│
├── start-all.bat              # ← ルートで実行（推奨）
└── README.md                  # このファイル
```

## 🔧 トラブルシューティング

### ポートが既に使用されている

**エラー:** "Port 8080 is already in use" または "Port 3000 is already in use"

**解決策:**
1. 既存のプロセスを終了する
2. または `application.yml` (バックエンド) や `package.json` (フロントエンド) でポート番号を変更

### データベースに接続できない

**確認事項:**
- データベースサーバーが起動しているか
- ホスト名、ポート番号、データベース名が正しいか
- ユーザー名とパスワードが正しいか
- ファイアウォールがブロックしていないか

### Mavenのビルドエラー

**解決策:**
```bash
cd backend
mvn clean install -U
```

### Reactのビルドエラー

**解決策:**
```bash
cd frontend
rmdir /s /q node_modules
del package-lock.json
npm install
```

### Java/Node.jsが見つからない

**解決策:**
1. Java/Node.js がインストールされているか確認
2. 環境変数 PATH に Java/Node.js のパスが含まれているか確認
3. コマンドプロンプトを再起動

### H2 Console にアクセスしたい

バックエンドが起動している状態で以下にアクセス:
- URL: http://localhost:8080/h2-console
- JDBC URL: `jdbc:h2:file:./data/udbmanager`
- Username: `sa`
- Password: (空欄)

## 🔒 セキュリティに関する注意

- 本アプリケーションはMVP版のため、ユーザー認証機能がありません
- 接続情報のパスワードは暗号化されて保存されますが、ローカル環境での使用を推奨します
- 本番環境のデータベースへの直接接続は避けてください

## 📝 今後の拡張予定

- CRUD操作 (CREATE/UPDATE/DELETE)
- クエリビルダー機能
- SQLite, SQL Server, Oracle 対応
- データエクスポート機能 (CSV, JSON, Excel)
- クエリ履歴・ブックマーク機能

## 📄 ライセンス

このプロジェクトは開発中です。

## 👥 開発者

HackathonTeamC

---

**作成日**: 2025年11月21日  
**バージョン**: 1.0.0 (MVP)