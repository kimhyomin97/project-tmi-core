#!/usr/bin/env python3
"""문서 링크 검사: 마크다운 상대 링크 `[..](path)` 가 실제 파일을 가리키는지 확인한다.

사용: python3 scripts/check-doc-links.py [--all]
- 기본은 docs/ai/archive/ (끝난 작업 기록)와 docs/public/ (팀 문서, 수정은 사람 확인)를 건너뛴다.
  --all 이면 전부 검사한다.
- 코드 블록 안의 링크는 검사하지 않는다.
- 종료 코드: 깨진 링크가 있으면 1.
"""
import os, re, subprocess, sys

check_all = '--all' in sys.argv
out = subprocess.run(['git', '-c', 'core.quotepath=false', 'ls-files', '*.md'],
                     capture_output=True, text=True).stdout
files = [f for f in out.split('\n') if f]
skip = () if check_all else ('docs/ai/archive/', 'docs/public/')
link_re = re.compile(r'\]\(([^)\s]+)\)')
broken = []
for f in files:
    if f.startswith(skip) or not os.path.exists(f):
        continue
    text = re.sub(r'```.*?```', lambda m: '\n' * m.group(0).count('\n'),
                  open(f, encoding='utf-8').read(), flags=re.S)
    for n, line in enumerate(text.split('\n'), 1):
        for t in link_re.findall(line):
            if re.match(r'^(https?:|mailto:|#)', t):
                continue
            p = t.split('#')[0]
            if p and not os.path.exists(os.path.normpath(os.path.join(os.path.dirname(f), p))):
                broken.append(f'{f}:{n}: {t}')
print('\n'.join(broken)) if broken else None
print(f'checked {len([f for f in files if not f.startswith(skip)])} files, broken links {len(broken)}', file=sys.stderr)
sys.exit(1 if broken else 0)
