# ずぼらカレンダー
by team SWK
<br><br>バックエンド[https://github.com/swkoubou/swk_hacku_2026_tokyo_backend](https://github.com/swkoubou/swk_hacku_2026_tokyo_backend)

---

## 概要
このアプリはズボラな人でも予定を管理できるようにすることを目的としたスマホアプリです。  
主に以下のことができます。

- ライブ壁紙を使って、ホーム画面からタスクの確認ができる
- 音声入力でスクの追加ができる
- チェックリストで完了したタスクを簡単に確認できる
- カレンダーで日付ごとのタスクを確認できる

---

## アプリ画面
| ホーム画面 | <img width="392" height="850" alt="ホーム画面" src="https://github.com/user-attachments/assets/efe5220a-858e-4a2c-96e8-38e2501210a7" /> |
| 予定確認画面 | ![detail](./images/detail.png) |

---

## UI構成
```
.
└── メイン画面
    ├── 予定入力（音声）
    ├── 予定確認
    ├── やることリスト
    ├── 設定
    │   ├── favicon.ico
    │   ├── index.php
    │   └── robots.txt
    └── vite.config.js
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
