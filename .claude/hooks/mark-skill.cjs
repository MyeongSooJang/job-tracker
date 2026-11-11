#!/usr/bin/env node
'use strict'
// PostToolUse(Skill) 훅. security-review / commit-and-pr-format 스킬을 실행하면 그 사실을 .claude/.state/ 에 기록한다.
// guard.js는 이 기록이 있어야 PR 생성·머지, 커밋을 허용한다.
//  - 보안 리뷰: 실행 시점의 HEAD 커밋을 기록한다. 이후 커밋이 바뀌면 다시 실행해야 한다.
//  - 커밋·PR 스킬: 실행한 세션 id를 기록한다. 새 세션에서는 다시 실행해야 한다.
// 보안 리뷰 직후에는 "문제 없으면 바로 push·PR" 안내를 additionalContext로 주입한다(강제가 아니라 상기).
// 한계: 스킬을 "호출한" 사실만 기록한다. 리뷰 결과를 반영했는지는 확인하지 못한다.
const fs = require('fs')
const path = require('path')
const { execFileSync } = require('child_process')

const ROOT = path.resolve(__dirname, '..', '..')
const STATE_DIR = path.join(ROOT, '.claude', '.state')

let input = {}
try {
  input = JSON.parse(fs.readFileSync(0, 'utf8'))
} catch {
  process.exit(0)
}

const toolInput = input.tool_input || {}
const skill = String(toolInput.skill || toolInput.name || '')
const is = name => new RegExp(`(^|:)${name}$`).test(skill)

function write(file, data) {
  fs.mkdirSync(STATE_DIR, { recursive: true })
  fs.writeFileSync(path.join(STATE_DIR, file), JSON.stringify({ ...data, at: new Date().toISOString() }, null, 2))
}

if (is('security-review')) {
  const sha = execFileSync('git', ['rev-parse', 'HEAD'], { cwd: ROOT, encoding: 'utf8' }).trim()
  write('security-review.json', { sha, session_id: input.session_id })
  // 리뷰 결과(문제 유무)는 훅이 알 수 없으므로, 보고서를 낸 뒤 어떻게 이어갈지를 모델에게 상기시킨다.
  process.stdout.write(JSON.stringify({
    hookSpecificOutput: {
      hookEventName: 'PostToolUse',
      additionalContext:
        '보안 리뷰 보고서를 낸 뒤, 취약점이 하나도 없으면 사용자에게 다시 묻지 말고 바로 push와 PR 생성까지 이어간다. ' +
        '취약점이 있으면 멈추고 보고한다. 머지는 사용자 지시가 있을 때만 한다.',
    },
  }))
}
if (is('commit-and-pr-format')) {
  write('commit-skill.json', { session_id: input.session_id })
}
