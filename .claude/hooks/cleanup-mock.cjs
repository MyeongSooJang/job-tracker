#!/usr/bin/env node
'use strict'
// 임시 미리보기(mock) 파일이 있으면 지운다. 직접 실행하거나(`npm run clean:mock`), guard.js가 PR 생성·머지 직전에 부른다.
// git이 추적 중인 파일은 지우지 않는다(실수로 커밋된 것이므로 사람이 git rm으로 정리해야 한다).
const fs = require('fs')
const path = require('path')
const { execFileSync } = require('child_process')

const ROOT = path.resolve(__dirname, '..', '..')

function config() {
  try {
    return JSON.parse(fs.readFileSync(path.join(__dirname, 'guard.config.json'), 'utf8'))
  } catch {
    return { mockFiles: [], previewPort: null }
  }
}

function isTracked(file) {
  try {
    return execFileSync('git', ['ls-files', '--', file], { cwd: ROOT, encoding: 'utf8', stdio: ['ignore', 'pipe', 'ignore'] }).trim() !== ''
  } catch {
    return false
  }
}

// guard.config.json은 저장소 안의 파일이라 누구나 바꿀 수 있다. 목록에 "../../.ssh/id_rsa" 같은 경로를 넣어 저장소 밖 파일을
// 지우게 만들 수 없도록, 저장소 안의 상대 경로이면서 파일 이름이 mock-preview.* 인 것만 지운다.
const SAFE_NAME = /^mock-preview\.[A-Za-z0-9]+$/
function isSafeMockPath(file) {
  if (typeof file !== 'string' || path.isAbsolute(file) || file.split(/[\\/]/).includes('..')) return false
  if (!SAFE_NAME.test(path.basename(file))) return false
  const full = path.resolve(ROOT, file)
  return full.startsWith(ROOT + path.sep)
}

// 지운 파일 목록(저장소 기준 경로)을 돌려준다.
function removeMockFiles() {
  const removed = []
  for (const file of config().mockFiles.filter(isSafeMockPath)) {
    const full = path.join(ROOT, file)
    if (fs.existsSync(full) && !isTracked(file)) {
      fs.rmSync(full, { force: true })
      removed.push(file)
    }
  }
  return removed
}

module.exports = { removeMockFiles, config, ROOT }

if (require.main === module) {
  const removed = removeMockFiles()
  console.log(removed.length ? `삭제함: ${removed.join(', ')}` : '삭제할 mock 파일이 없습니다.')
}
