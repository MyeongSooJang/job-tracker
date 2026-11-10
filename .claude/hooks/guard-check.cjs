'use strict'
// 실행: node --test .claude/hooks/guard-check.cjs
// 임시 git 저장소에 훅 스크립트를 복사해 실제로 실행한다. 사용자의 ~/.claude는 GUARD_HOME으로 격리한다.
const test = require('node:test')
const assert = require('node:assert/strict')
const fs = require('fs')
const net = require('net')
const os = require('os')
const path = require('path')
const { spawnSync, execFileSync } = require('child_process')

const SCRIPTS = ['guard.cjs', 'mark-skill.cjs', 'cleanup-mock.cjs']
const MOCK = ['mock-preview.html', 'src/mock-preview.tsx']
const tmp = []

const git = (cwd, ...args) => execFileSync('git', args, { cwd, encoding: 'utf8' }).trim()

function makeRepo(config = { mockFiles: MOCK, previewPort: null }) {
  const dir = fs.mkdtempSync(path.join(os.tmpdir(), 'guard-test-'))
  tmp.push(dir)
  git(dir, 'init', '-q')
  git(dir, 'config', 'user.email', 'test@example.com')
  git(dir, 'config', 'user.name', 'test')
  fs.writeFileSync(path.join(dir, 'README.md'), 'x')
  git(dir, 'add', '-A')
  git(dir, 'commit', '-q', '-m', 'init')
  fs.mkdirSync(path.join(dir, '.claude', 'hooks'), { recursive: true })
  for (const file of SCRIPTS) fs.copyFileSync(path.join(__dirname, file), path.join(dir, '.claude', 'hooks', file))
  fs.writeFileSync(path.join(dir, '.claude', 'hooks', 'guard.config.json'), JSON.stringify(config))
  const home = fs.mkdtempSync(path.join(os.tmpdir(), 'guard-home-'))
  tmp.push(home)

  const exec = (script, input) =>
    spawnSync(process.execPath, [path.join(dir, '.claude', 'hooks', script)], {
      input: JSON.stringify(input),
      encoding: 'utf8',
      env: { ...process.env, GUARD_HOME: home },
    })
  return {
    dir,
    home,
    hook: (command, extra = {}) => exec('guard.cjs', { tool_name: 'Bash', tool_input: { command }, cwd: dir, session_id: 's1', ...extra }),
    mark: (skill, session = 's1') => exec('mark-skill.cjs', { tool_name: 'Skill', tool_input: { skill }, session_id: session }),
    write: (file, text = 'x') => {
      fs.mkdirSync(path.dirname(path.join(dir, file)), { recursive: true })
      fs.writeFileSync(path.join(dir, file), text)
    },
    exists: file => fs.existsSync(path.join(dir, file)),
    installCommitSkill: () => {
      const skillDir = path.join(home, '.claude', 'skills', 'commit-and-pr-format')
      fs.mkdirSync(skillDir, { recursive: true })
      fs.writeFileSync(path.join(skillDir, 'SKILL.md'), '# skill')
    },
  }
}

test.after(() => {
  for (const dir of tmp) fs.rmSync(dir, { recursive: true, force: true })
})

test('관련 없는 명령은 건드리지 않는다', () => {
  const repo = makeRepo()
  const result = repo.hook('ls -la && git status')
  assert.equal(result.status, 0)
  assert.equal(result.stdout, '')
})

test('커밋 메시지(heredoc)나 따옴표 안의 gh pr create는 PR 생성으로 보지 않는다', () => {
  const repo = makeRepo()
  const heredoc = repo.hook("git commit -q -F - <<'EOF'\nfix: gh pr create 와 gh pr merge 설명\nEOF")
  const quoted = repo.hook('echo "gh pr create" && git status')
  assert.equal(heredoc.status, 0)
  assert.equal(quoted.status, 0)
})

test('mock 파일이 스테이징된 채로는 커밋할 수 없다', () => {
  const repo = makeRepo()
  repo.write('mock-preview.html')
  git(repo.dir, 'add', 'mock-preview.html')
  const result = repo.hook('git commit -m x')
  assert.equal(result.status, 2)
  assert.match(result.stderr, /mock-preview\.html/)
})

test('mock 파일이 추적되고 있으면 푸시할 수 없다', () => {
  const repo = makeRepo()
  repo.write('src/mock-preview.tsx')
  git(repo.dir, 'add', '-A')
  git(repo.dir, 'commit', '-q', '-m', 'oops')
  const result = repo.hook('git push')
  assert.equal(result.status, 2)
  assert.match(result.stderr, /추적/)
})

test('보안 리뷰 기록이 없으면 PR을 만들 수 없다', () => {
  const repo = makeRepo()
  const result = repo.hook('gh pr create --title x')
  assert.equal(result.status, 2)
  assert.match(result.stderr, /보안 리뷰/)
})

test('같은 커밋에 대한 보안 리뷰가 있으면 PR을 만들 수 있고, 남은 mock 파일은 지워진다', () => {
  const repo = makeRepo()
  repo.write('mock-preview.html')
  repo.write('src/mock-preview.tsx')
  assert.equal(repo.mark('security-review').status, 0)

  const result = repo.hook('gh pr create --title x')

  assert.equal(result.status, 0)
  assert.equal(repo.exists('mock-preview.html'), false)
  assert.equal(repo.exists('src/mock-preview.tsx'), false)
  assert.match(result.stdout, /삭제/)
})

test('보안 리뷰 뒤에 커밋이 바뀌면 다시 리뷰해야 한다', () => {
  const repo = makeRepo()
  repo.mark('security-review')
  repo.write('later.txt')
  git(repo.dir, 'add', '-A')
  git(repo.dir, 'commit', '-q', '-m', 'later')

  const result = repo.hook('gh pr create --title x')

  assert.equal(result.status, 2)
  assert.match(result.stderr, /다시 실행/)
})

test('플러그인 접두사가 붙은 이름의 보안 리뷰도 기록된다', () => {
  const repo = makeRepo()
  repo.mark('some-plugin:security-review')
  assert.equal(repo.hook('gh pr create').status, 0)
})

test('관련 없는 스킬은 보안 리뷰로 기록하지 않는다', () => {
  const repo = makeRepo()
  repo.mark('code-review')
  assert.equal(repo.hook('gh pr create').status, 2)
})

test('커밋 스킬이 설치돼 있으면 이 세션에서 실행해야 커밋할 수 있다', () => {
  const repo = makeRepo()
  repo.installCommitSkill()

  assert.equal(repo.hook('git commit -m x').status, 2)
  repo.mark('commit-and-pr-format', 's1')
  assert.equal(repo.hook('git commit -m x', { session_id: 's1' }).status, 0)
  assert.equal(repo.hook('git commit -m x', { session_id: 's2' }).status, 2)
})

test('커밋 스킬이 없으면 막지 않고 알리기만 한다', () => {
  const repo = makeRepo()
  const result = repo.hook('git commit -m x')
  assert.equal(result.status, 0)
  assert.match(JSON.parse(result.stdout).systemMessage, /강제하지 못/)
})

test('다른 저장소에서 실행하는 명령에는 이 저장소의 규칙을 적용하지 않는다', () => {
  const repo = makeRepo()
  const other = makeRepo()
  assert.equal(repo.hook('gh pr create', { cwd: other.dir }).status, 0)
})

test('cd로 옮겨 간 저장소를 기준으로 판단한다', () => {
  const repo = makeRepo()
  const other = makeRepo()
  const toOther = repo.hook(`cd "${other.dir}" && gh pr create`)
  const toRepo = repo.hook(`cd "${repo.dir}" && gh pr create`, { cwd: other.dir })
  assert.equal(toOther.status, 0)
  assert.equal(toRepo.status, 2)
})

test('머지는 미리보기 개발 서버가 켜져 있으면 차단된다', async () => {
  const server = net.createServer()
  await new Promise(resolve => server.listen(0, '127.0.0.1', resolve))
  const repo = makeRepo({ mockFiles: MOCK, previewPort: server.address().port })
  const result = await new Promise(resolve => {
    // spawnSync는 이벤트 루프를 막으므로 비동기로 실행한다.
    const child = require('child_process').spawn(process.execPath, [path.join(repo.dir, '.claude', 'hooks', 'guard.cjs')], {
      env: { ...process.env, GUARD_HOME: repo.home },
    })
    let stderr = ''
    child.stderr.on('data', chunk => (stderr += chunk))
    child.on('close', status => resolve({ status, stderr }))
    child.stdin.end(JSON.stringify({ tool_input: { command: 'gh pr merge 1 --merge' }, cwd: repo.dir, session_id: 's1' }))
  })
  server.close()
  assert.equal(result.status, 2)
  assert.match(result.stderr, /개발 서버/)
})

test('머지 대상 PR의 최신 커밋을 확인하지 못하면 통과시키지 않는다', () => {
  const repo = makeRepo()
  repo.mark('security-review')
  const result = repo.hook('gh pr merge 999999 --merge')
  assert.equal(result.status, 2)
})

test('설정에 저장소 밖 경로나 mock 파일이 아닌 이름이 들어 있어도 지우지 않는다', () => {
  const victim = path.join(os.tmpdir(), `guard-victim-${process.pid}.txt`)
  fs.writeFileSync(victim, 'keep')
  tmp.push(victim)
  const relative = path.relative(path.join(os.tmpdir(), 'guard-test-x'), victim).replace(/\\/g, '/')
  const repo = makeRepo({ mockFiles: [`../${path.basename(victim)}`, relative, victim, 'README.md', '.git/config', 'mock-preview.html'], previewPort: null })
  repo.write('mock-preview.html')
  const script = path.join(repo.dir, '.claude', 'hooks', 'cleanup-mock.cjs')

  const result = spawnSync(process.execPath, [script], { encoding: 'utf8' })

  assert.equal(fs.existsSync(victim), true, '저장소 밖 파일이 지워졌다')
  assert.equal(repo.exists('README.md'), true)
  assert.equal(repo.exists('.git/config'), true)
  assert.equal(repo.exists('mock-preview.html'), false)
  assert.match(result.stdout, /삭제함: mock-preview\.html$/m)
})

test('npm run clean:mock 용 스크립트는 mock 파일이 있으면 지우고, 없으면 아무 일도 하지 않는다', () => {
  const repo = makeRepo()
  repo.write('mock-preview.html')
  const script = path.join(repo.dir, '.claude', 'hooks', 'cleanup-mock.cjs')

  const first = spawnSync(process.execPath, [script], { encoding: 'utf8' })
  const second = spawnSync(process.execPath, [script], { encoding: 'utf8' })

  assert.match(first.stdout, /삭제함: mock-preview\.html/)
  assert.equal(repo.exists('mock-preview.html'), false)
  assert.match(second.stdout, /삭제할 mock 파일이 없습니다/)
})
