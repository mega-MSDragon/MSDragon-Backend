# Notification Domain

Notification 도메인은 FCM 푸시 기기 토큰 관리와 이벤트 발생 시 알림 발송을 담당합니다.

---

## 책임

- 로그인한 사용자의 FCM 기기 토큰을 등록·갱신·해제합니다.
- 토큰은 기기마다 하나이므로 전역 unique로 저장하고, 같은 기기에 다른 사용자가 로그인하면 소유자를 옮깁니다.
- 다른 도메인이 알림을 보낼 때 쓰는 `NotificationService.notifyUsers`를 제공합니다. 알림을 끈 사용자와 탈퇴한 사용자를 걸러내고, 사용할 수 없는 토큰을 정리합니다.
- **발송 실패가 알림을 유발한 API를 실패시키지 않습니다.** 모든 예외를 삼키고 로그만 남깁니다.
- Firebase 서비스 계정 키가 없으면 발송을 건너뛰어 키 없이도 배포할 수 있습니다.
- 발송 트리거는 각 도메인이 가집니다. feedback 도메인이 평가 요청 알림, parentprofile 도메인이 프로필 작성 요청 알림을 호출합니다.

---

## 패키지 구조

```text
notification
├── config
├── controller
├── dto
├── entity
├── repository
└── service
```

---

## 관련 테이블

- `user_device_tokens`
- `users` (`notification_enabled`)

---

## 관련 API

- `POST /api/v1/users/me/device-tokens`
- `DELETE /api/v1/users/me/device-tokens`

---

## 후속 범위

- 일정 알림처럼 시간 기반 발송. 스케줄러와 중복 발송 방지 기록이 필요합니다.
- 안전 알림. 무엇을 언제 보낼지 기획이 정해지지 않았습니다.
- 알림 종류별 수신 설정. 현재는 전체 on/off 하나입니다.
