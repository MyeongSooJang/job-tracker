#!/usr/bin/env node
'use strict'
// PreToolUse(Bash|PowerShell) 훅. 커밋·푸시·PR 생성·머지 직전에 작업 규칙을 확인하고, 어기면 차단(exit 2)한다.
//
//  1. mock 미리보기 파일: 스테이징/추적된 채 커밋·푸시·PR하면 차단. PR 생성·머지 직전에는 남은 파일을 지우고,
//     머지 전에는 미리보기 개발 서버(previewPort)가 꺼져 있어야 한다.
//  2. 커밋·PR 스킬: commit-and-pr-format 스킬이 설치돼 있으면, 이 세션에서 실행한 뒤에만 git commit / gh pr create를 허용한다.
//     설치돼 있지 않으면 막지 않고 알리기만 한다(없는 스킬을 강제하면 모든 커밋이 영원히 막힌다).
//  3. 보안 리뷰: gh pr create / gh pr merge 전에 현재 커밋(HEAD, 머지는 PR의 최신 커밋)에 대한 /security-review 기록이 있어야 한다.
//
// 기록(마커)은 mark-skill.js가 스킬 실행 직후 .claude/.state/에 남긴다. 사용자가 직접 입력하는 `! 명령`은 이 훅을 거치지 않는다.
const fs = require('fs')
const os = require('os')
const net = require('net')
const path = require('path')
const { execFileSync } = require('child_process')
const { removeMockFiles, config, ROOT } = require('./cleanup-mock.cjs')

const STATE_DIR = path.join(ROOT, '.claude', '.state')
const COMMIT_SKILL = 'commit-and-pr-format'

const run = (cmd, args, cwd = ROOT) => execFileSync(cmd, args, { cwd, encoding: 'utf8', stdio: ['ignore', 'pipe', 'ignore'] }).trim()
const short = sha => String(sha).slice(0, 7)
const readJson = file => {
  try {
    return JSON.parse(fs.readFileSync(file, 'utf8'))
  } catch {
    return null
  }
}

// 명령 문자열에서 heredoc 본문과 따옴표 안의 글을 지워, 커밋 메시지 속 "gh pr create" 같은 글에 반응하지 않게 한다.
function withoutHeredoc(command) {
  return command.replace(/<<-?\s*(['"]?)(\w+)\1[\s\S]*?\n\s*\2(?=\s|$)/g, '')
}
const withoutQuotes = command => command.replace(/"(?:[^"\\]|\\.)*"|'[^']*'/g, '""')

const normalize = p => path.resolve(p).replace(/\\/g, '/').toLowerCase()
const toNativePath = p => (process.platform === 'win32' ? p.replace(/^\/([a-zA-Z])\//, '$1:/') : p)

// 명령이 `cd 경로 &&`로 다른 폴더로 옮겨 간 뒤 실행되는 경우를 반영해 실제로 작업하는 폴더를 구한다.
function targetDir(command, cwd) {
  let dir = cwd || process.cwd()
  const cd = /(?:^|&&|;|\|\|)\s*cd\s+(?:"([^"]+)"|'([^']+)'|(\S+))/g
  let match
  while ((match = cd.exec(command)) !== null) {
    const next = toNativePath(match[1] || match[2] || match[3])
    dir = path.resolve(dir, next)
  }
  return dir
}

function isThisRepo(dir) {
  try {
    return normalize(run('git', ['rev-parse', '--show-toplevel'], dir)) === normalize(ROOT)
  } catch {
    return true // 판단할 수 없으면 적용한다(막는 쪽이 안전하다)
  }
}

function findDir(root, name, depth) {
  if (depth < 0 || !fs.existsSync(root)) return false
  let entries
  try {
    entries = fs.readdirSync(root, { withFileTypes: true })
  } catch {
    return false
  }
  for (const entry of entries) {
    if (!entry.isDirectory() || entry.name === 'node_modules') continue
    if (entry.name === name) return true
    if (findDir(path.join(root, entry.name), name, depth - 1)) return true
  }
  return false
}

function skillInstalled(name) {
  const home = process.env.GUARD_HOME || os.homedir()
  const dirs = [path.join(home, '.claude', 'skills', name), path.join(ROOT, '.claude', 'skills', name)]
  if (dirs.some(dir => fs.existsSync(path.join(dir, 'SKILL.md')))) return true
  return findDir(path.join(home, '.claude', 'plugins'), name, 7)
}

function portOpen(port) {
  return new Promise(resolve => {
    const socket = net.connect({ host: '127.0.0.1', port })
    const done = result => {
      socket.destroy()
      resolve(result)
    }
    socket.setTimeout(500, () => done(false))
    socket.once('connect', () => done(true))
    socket.once('error', () => done(false))
  })
}

function prHeadSha(number) {
  try {
    return run('gh', ['pr', 'view', ...(number ? [number] : []), '--json', 'headRefOid', '-q', '.headRefOid'])
  } catch {
    return null
  }
}

function securityProblem(headSha, what) {
  if (!headSha) return `${what}의 최신 커밋을 gh로 확인하지 못해 보안 리뷰 여부를 판단할 수 없습니다. gh 로그인·네트워크를 확인하세요.`
  const marker = readJson(path.join(STATE_DIR, 'security-review.json'))
  if (!marker) return `보안 리뷰 기록이 없습니다. /security-review 를 먼저 실행하세요. (${what} ${short(headSha)})`
  if (marker.sha !== headSha) {
    return `마지막 보안 리뷰(${short(marker.sha)}) 이후 커밋이 바뀌었습니다(${what} ${short(headSha)}). /security-review 를 다시 실행하세요.`
  }
  return null
}

async function inspect(input, flags) {
  const problems = []
  const notes = []
  const settings = config()
  const mockPattern = file => settings.mockFiles.includes(file)

  // 1) mock 파일
  if (flags.commit) {
    const staged = run('git', ['diff', '--cached', '--name-only']).split('\n').filter(mockPattern)
    if (staged.length) problems.push(`임시 미리보기 파일이 스테이징돼 있습니다: ${staged.join(', ')}. git restore --staged 로 빼고 커밋하세요.`)
  }
  if (flags.push || flags.prCreate || flags.prMerge) {
    const tracked = settings.mockFiles.filter(file => run('git', ['ls-files', '--', file]) !== '')
    if (tracked.length) problems.push(`임시 미리보기 파일이 git에 추적되고 있습니다: ${tracked.join(', ')}. git rm 으로 제거한 뒤 진행하세요.`)
  }
  if (flags.prCreate || flags.prMerge) {
    const removed = removeMockFiles()
    if (removed.length) notes.push(`임시 미리보기 파일을 삭제했습니다: ${removed.join(', ')}`)
  }
  if (flags.prMerge && settings.previewPort && (await portOpen(settings.previewPort))) {
    problems.push(`미리보기 개발 서버(127.0.0.1:${settings.previewPort})가 켜져 있습니다. 종료한 뒤 머지하세요.`)
  }

  // 2) 커밋·PR 스킬
  if (flags.commit || flags.prCreate) {
    if (skillInstalled(COMMIT_SKILL)) {
      const marker = readJson(path.join(STATE_DIR, 'commit-skill.json'))
      if (!marker || !input.session_id || marker.session_id !== input.session_id) {
        problems.push(`커밋·PR 작성 전에 ${COMMIT_SKILL} 스킬을 먼저 실행하세요(세션마다 한 번).`)
      }
    } else {
      notes.push(`${COMMIT_SKILL} 스킬이 이 컴퓨터에 없어 강제하지 못했습니다. ~/.claude/skills/${COMMIT_SKILL}/ 에 설치하면 자동으로 강제됩니다.`)
    }
  }

  // 3) 보안 리뷰
  if (flags.prCreate) {
    const problem = securityProblem(run('git', ['rev-parse', 'HEAD']), '현재 HEAD')
    if (problem) problems.push(problem)
  }
  if (flags.prMerge) {
    const problem = securityProblem(prHeadSha(flags.prNumber), `PR${flags.prNumber ? ` #${flags.prNumber}` : ''}`)
    if (problem) problems.push(problem)
  }
  return { problems, notes }
}

async function main() {
  let input = {}
  try {
    input = JSON.parse(fs.readFileSync(0, 'utf8'))
  } catch {
    // 입력이 없으면 아무것도 하지 않는다
  }
  const command = input.tool_input && input.tool_input.command
  if (typeof command !== 'string') return

  const noHeredoc = withoutHeredoc(command)
  const code = withoutQuotes(noHeredoc)
  const merge = code.match(/\bgh\s+pr\s+merge\b(?:\s+(\d+))?/)
  const flags = {
    commit: /\bgit(?:\s+-C\s+\S+)?\s+commit\b/.test(code),
    push: /\bgit(?:\s+-C\s+\S+)?\s+push\b/.test(code),
    prCreate: /\bgh\s+pr\s+create\b/.test(code),
    prMerge: merge !== null,
    prNumber: merge && merge[1],
  }
  if (!Object.values(flags).some(Boolean)) return
  if (!isThisRepo(targetDir(noHeredoc, input.cwd))) return

  let result
  try {
    result = await inspect(input, flags)
  } catch (error) {
    // 검사 자체가 실패하면 통과시키지 않는다.
    process.stderr.write(`[guard] 규칙을 확인하지 못해 차단합니다: ${error.message}\n`)
    process.exit(2)
  }
  if (result.problems.length) {
    process.stderr.write(`[guard] 차단: 작업 규칙을 먼저 지켜 주세요.\n${result.problems.map(p => `- ${p}`).join('\n')}\n`)
    process.exit(2)
  }
  if (result.notes.length) {
    process.stdout.write(JSON.stringify({ systemMessage: result.notes.join('\n') }))
  }
}

main()
