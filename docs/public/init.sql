-- TMI Core 최초 1회 실행용 DB 초기화 스크립트
-- 적용 방법: psql -h localhost -p 15432 -U postgres -d tmi_word -f init.sql

-- ─────────────────────────────────────────────
-- 1. 테이블
-- ─────────────────────────────────────────────

CREATE TABLE sentence (
    id            varchar(36) PRIMARY KEY,
    english_text  text        NOT NULL,
    korean_ref    text        NOT NULL,
    level         varchar(20) NOT NULL
                  CHECK (level IN ('LEVEL_1', 'LEVEL_2', 'LEVEL_3', 'LEVEL_4', 'LEVEL_5')),
    category      varchar(20) NOT NULL
                  CHECK (category IN ('DAILY', 'BUSINESS', 'TECH')),
    created_at    timestamptz NOT NULL DEFAULT now(),
    updated_at    timestamptz NOT NULL DEFAULT now(),
    deleted_at    timestamptz NULL
);

-- ─────────────────────────────────────────────
-- 2. 인덱스 — 출제 쿼리 가속 (활성 행만 인덱싱)
-- ─────────────────────────────────────────────

CREATE INDEX idx_sentence_filter
    ON sentence (level, category)
    WHERE deleted_at IS NULL;

-- ─────────────────────────────────────────────
-- 3. 시드 데이터
-- ─────────────────────────────────────────────

INSERT INTO sentence (id, english_text, korean_ref, level, category) VALUES
    ('00000000-0000-0000-0000-000000000001',
     'Sounds good.',
     '좋아요.',
     'LEVEL_1', 'DAILY'),

    ('00000000-0000-0000-0000-000000000002',
     'What did you do yesterday?',
     '어제 뭐 했어요?',
     'LEVEL_2', 'DAILY'),

    ('00000000-0000-0000-0000-000000000003',
     'I had a rough day at work today.',
     '오늘 회사에서 고단한 하루를 보냈다.',
     'LEVEL_3', 'DAILY'),

    ('00000000-0000-0000-0000-000000000004',
     'Could you forward this email to the team?',
     '이 이메일을 팀에 전달해 주실 수 있나요?',
     'LEVEL_3', 'BUSINESS'),

    ('00000000-0000-0000-0000-000000000005',
     'The deployment failed because of a missing environment variable.',
     '환경 변수 누락 때문에 배포가 실패했어요.',
     'LEVEL_3', 'TECH');
