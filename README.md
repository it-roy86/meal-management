# 🍱 meal-management (구내식당 앱 운영 프로젝트)

구내식당 앱의 안정적인 운영과 개발을 위한 서버 구축 및 인프라 관리 기록입니다.

## 🛠 인프라 환경
- **Cloud Provider**: [AWS (Amazon Web Services)](https://aws.amazon.com/)
- **Service**: [Amazon Lightsail](https://lightsail.aws.amazon.com/)
- **Region**: Seoul (ap-northeast-2a)
- **OS**: Ubuntu 24.04 LTS
- **Plan**: 2 GB RAM, 2 vCPUs, 60 GB SSD

## 📅 진행 기록
[x] Docker 엔진 설치 및 권한 설정
[x] Docker Compose 설치 완료
[x] hello-world 컨테이너 실행 테스트 통과

### 1단계: 인프라 구축 (2026-05-05)
- [x] AWS 신규 계정 생성 및 결제 정보 등록
- [x] Lightsail 인스턴스([hansu-server](https://ap-northeast-2.lightsail.aws.amazon.com/ls/webapp/ap-northeast-2/instances/hansu-server/connect)) 생성
- [x] 서울 리전 및 Ubuntu OS 환경 설정
- [x] SSH 터미널 접속 확인 및 시스템 업데이트 수행

## 🚀 향후 계획
- [ ] Docker 및 Docker Compose 설치
- [ ] 구내식당 앱 소스 코드 배포 및 컨테이너화
- [ ] 고정 IP(Static IP) 설정 및 도메인 연결

