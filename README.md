# ずぼらカレンダー
by team SWK
<br><br>バックエンド[https://github.com/swkoubou/swk_hacku_2026_tokyo_backend](https://github.com/swkoubou/swk_hacku_2026_tokyo_backend)

---

## 概要
このアプリはズボラな人でも予定を管理できるようにすることを目的としたスマホアプリです。  
主に以下のことができます。

- ライブ壁紙を使って、ホーム画面から予定の確認ができる
- 音声入力で予定の追加ができる
- チェックリストで予定の終了処理が簡単にできる
- カレンダーで１日ごとの予定を確認できる

---

## アプリ画面
| ホーム画面 | ライブ壁紙画面 | 音声入力画面 | 予定確認画面 | やることリスト画面 | 設定画面 |
|---------|---------|---------|---------|---------|---------|
<img width="392" height="850" alt="ホーム画面" src="https://github.com/user-attachments/assets/efe5220a-858e-4a2c-96e8-38e2501210a7" /> | <img width="388" height="852" alt="ライブ壁紙画面" src="https://github.com/user-attachments/assets/bcab30a9-64cc-4bb3-8f13-e624e15b9495" /> | <img width="392" height="850" alt="音声入力画面" src="https://github.com/user-attachments/assets/f10ed336-c3fd-4164-8ce2-0fd62e578646" /> | <img width="394" height="853" alt="予定確認画面" src="https://github.com/user-attachments/assets/48ae5170-f67b-4d52-ace6-32096d316983" /> | <img width="395" height="852" alt="やることリスト画面" src="https://github.com/user-attachments/assets/29a48ead-b2f1-46ea-9328-37992c78cb45" /> | <img width="393" height="860" alt="設定画面" src="https://github.com/user-attachments/assets/3624a723-eb47-4aab-bea2-773a646c0868" /> |

---

## UI構成
```
.
├── ホーム画面
│   ├── 予定入力（音声）
│   │   ├── 登録
│   │   ├── 再録音
│   │   ├── 処理レベルを上げて再生成する
│   │   └── 結果を編集する
│   ├── 予定確認
│   │   ├── 予定の追加
│   │   ├── 予定の変更
│   │   └── 予定の削除
│   ├── やることリスト
│   │   └── 予定の完了・未完了のリスト
│   └── 設定
│       ├── フォントサイズ
│       ├── 予定の表示数
│       ├── UIの色
│       ├── ユーザーID
│       └── ライブ壁紙の適用の確認
│
└── ライブ壁紙
```

---

## 技術スタック
- 言語: Kotlin
- フレームワーク: Android SDK
- ライブラリ:
  - OkHttp
  - Retrofit
  - Gson
  - Compose Calendar
  - Jetpack Compose
  - Navigation Compose

---
