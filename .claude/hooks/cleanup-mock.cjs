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

// 지운 파일 목록(저장소 기준 경로)을 돌려준다.
function removeMockFiles() {
  const removed = []
  for (const file of config().mockFiles) {
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
