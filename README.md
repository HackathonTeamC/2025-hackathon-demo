# Universal Database Manager (UDB Manager)

様々なデータベースに接続し、SQLの知識が少ないユーザーでもデータの取得・編集・閲覧を直感的に行えるWebアプリケーション。

## 🚀 機能

### Phase 1 (MVP) - 実装済み
- ✅ **データベース接続管理**
  - MySQL, PostgreSQL, SQLite, H2 対応
  - 接続情報の作成・編集・削除
  - 接続テスト機能
  - パスワードの暗号化保存

### Phase 2 - 実装済み
- ✅ **エンタープライズデータベース対応**
  - Oracle Database 対応
  - Microsoft SQL Server 対応
  - データベース固有の接続文字列フォーマットに対応

- ✅ **メタデータ閲覧**
  - テーブル一覧表示
  - カラム情報の詳細表示（データ型、主キー、NULL許可など）
  - 検索・フィルタリング

- ✅ **データ操作**
  - データグリッド表示（AG-Grid）
  - ページネーション
  - ソート機能
  - CRUD操作（作成・読取・更新・削除）

- ✅ **SQL実行**
  - シンタックスハイライト付きSQLエディタ
  - クエリ実行と結果表示
  - 実行時間・影響行数の表示

## 📋 技術スタック

### バックエンド
- **言語**: Java 17
- **フレームワーク**: Spring Boot 3.2.0
- **ビルドツール**: Maven
- **データベース**:
  - H2 (接続情報保存用)
  - MySQL Connector
  - PostgreSQL Driver
  - Oracle JDBC Driver (ojdbc11)
  - Microsoft SQL Server JDBC Driver
  - HikariCP (コネクションプール)
- **セキュリティ**: Jasypt (パスワード暗号化)

### フロントエンド
- **フレームワーク**: React 18 + TypeScript
- **UIライブラリ**: Material-UI 5
- **データグリッド**: AG-Grid Community
- **SQLエディタ**: CodeMirror
- **HTTPクライアント**: Axios
- **ルーティング**: React Router

## 🛠️ セットアップ手順

### 前提条件
- Java 17 以上
- Node.js 16 以上
- Maven 3.6 以上

### 1. リポジトリのクローン
```bash
git clone <repository-url>
cd udb-manager
```

### 2. バックエンドの起動

**Linux/Mac の場合:**
```bash
cd backend
./mvnw spring-boot:run
```

または起動スクリプトを使用:
```bash
./start-backend.sh
```

**Windows の場合:**
```bash
cd backend
mvnw.cmd spring-boot:run
```

または起動スクリプトを使用:
```bash
start-backend.bat
```

バックエンドは `http://localhost:8080` で起動します。

### 3. フロントエンドの起動（別ターミナル）

**Linux/Mac の場合:**
```bash
cd frontend
npm install
npm start
```

または起動スクリプトを使用:
```bash
./start-frontend.sh
```

**Windows の場合:**
```bash
cd frontend
npm install
npm start
```

または起動スクリプトを使用:
```bash
start-frontend.bat
```

フロントエンドは `http://localhost:3000` で起動します。

## 📖 使い方

### 1. データベース接続の作成
1. トップページで「New Connection」ボタンをクリック
2. 接続情報を入力:
   - 接続名
   - データベースタイプ (MySQL/PostgreSQL/SQLite/H2)
   - ホスト名
   - ポート番号
   - データベース名
   - ユーザー名
   - パスワード
3. 「Test Connection」で接続確認
4. 「Create」で保存

### 2. データベースへの接続
1. 接続カードの「Connect」ボタンをクリック
2. 左サイドバーにテーブル一覧が表示されます

### 3. テーブルデータの閲覧
1. 左サイドバーからテーブルを選択
2. 「Table Structure」タブでカラム情報を確認
3. 「Data」タブでデータをグリッド表示

### 4. SQLの実行
1. 「SQL Editor」タブを選択
2. SQLクエリを入力
3. 「Execute」ボタンで実行
4. 結果がグリッド形式で表示されます

## 🔒 セキュリティ

- パスワードは Jasypt を使用して AES 暗号化
- SQL Injection 対策（PreparedStatement 使用）
- CORS 設定（開発環境: localhost:3000 のみ許可）

## ⚠️ 注意事項

- 本番環境では以下を変更してください:
  - `application.properties` の `app.encryption.secret-key`
  - CORS 設定
- 認証機能は未実装のため、内部ネットワークでの使用を推奨
- SQLite を使用する場合、ポートは不要です

## 🔧 設定

### バックエンド設定 (`backend/src/main/resources/application.properties`)
```properties
# サーバーポート
server.port=8080

# H2 データベース (接続情報保存用)
spring.datasource.url=jdbc:h2:file:./data/udbmanager

# 暗号化キー（本番環境では変更必須）
app.encryption.secret-key=<your-secret-key>
```

### フロントエンド設定 (`.env`)
```
REACT_APP_API_URL=http://localhost:8080/api
```

## 📁 プロジェクト構成

```
udb-manager/
├── backend/                    # Spring Boot アプリケーション
│   ├── src/main/java/com/udbmanager/
│   │   ├── config/            # 設定クラス
│   │   ├── controller/        # REST API コントローラー
│   │   ├── dto/               # データ転送オブジェクト
│   │   ├── exception/         # 例外処理
│   │   ├── model/             # エンティティモデル
│   │   ├── repository/        # データアクセス層
│   │   ├── service/           # ビジネスロジック
│   │   └── util/              # ユーティリティ
│   └── src/main/resources/
│       └── application.properties
└── frontend/                   # React アプリケーション
    ├── public/
    └── src/
        ├── components/         # React コンポーネント
        │   ├── connections/    # 接続管理UI
        │   ├── data/           # データグリッド
        │   ├── metadata/       # メタデータ表示
        │   └── sql/            # SQLエディタ
        ├── services/           # API クライアント
        └── types/              # TypeScript 型定義
```

## 🚧 今後の拡張予定（Phase 3以降）

- ユーザー認証・認可機能
- クエリ履歴・ブックマーク機能
- データエクスポート（CSV/JSON/Excel）
- クエリビルダー（GUI ベースのクエリ作成）
- MongoDB, Snowflake, Salesforce 対応
- ER図の自動生成
- パフォーマンス監視・チューニング支援

## 📄 ライセンス

MIT License

## 👥 開発者

SWE Agent

## 📊 対応データベース一覧

| データベース | サポート状況 | デフォルトポート | 備考 |
|------------|------------|-----------------|------|
| MySQL | ✅ Phase 1 | 3306 | |
| PostgreSQL | ✅ Phase 1 | 5432 | |
| SQLite | ✅ Phase 1 | - | ファイルベース |
| H2 | ✅ Phase 1 | 9092 | 組み込み/サーバーモード |
| Oracle Database | ✅ Phase 2 | 1521 | SID形式: `ORCL` または Service Name形式: `/XEPDB1` |
| Microsoft SQL Server | ✅ Phase 2 | 1433 | SSL暗号化対応 |
| MongoDB | 🔜 Phase 3 | 27017 | 予定 |
| Snowflake | 🔜 Phase 3 | - | 予定 |
| Salesforce | 🔜 Phase 3 | - | 予定 |

---

**バージョン**: 2.0.0  
**作成日**: 2025年11月27日
**最終更新**: 2025年11月27日（Phase 2 完了）
