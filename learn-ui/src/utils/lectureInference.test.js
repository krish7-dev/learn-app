/**
 * Standalone validation script — no test framework needed.
 * Run: node src/utils/lectureInference.test.js
 */
import { inferLectureType, inferPriority, inferEstimatedMinutes, makeLectureId, makeSourceCode } from './lectureInference.js'

let passed = 0
let failed = 0

function expect(label, actual, expected) {
  if (actual === expected) {
    console.log(`  ✓ ${label}`)
    passed++
  } else {
    console.error(`  ✗ ${label}\n    expected: ${expected}\n    got:      ${actual}`)
    failed++
  }
}

function section(name) {
  console.log(`\n${name}`)
  console.log('─'.repeat(name.length))
}

// ─── inferLectureType ────────────────────────────────────────────────────────

section('inferLectureType')

expect(
  'Problem Solving Session + Optional Class → OPTIONAL',
  inferLectureType('036A - Advanced DSA - Problem Solving Session - Optional Class'),
  'OPTIONAL'
)
expect(
  'Townhall + Optional Class → OPTIONAL',
  inferLectureType('038C - Advanced DSA - Townhall - Optional Class'),
  'OPTIONAL'
)
expect(
  'Contest 2 → CONTEST',
  inferLectureType('051 - Advanced DSA - Contest 2'),
  'CONTEST'
)
expect(
  'Contest Discussion + Optional Class → OPTIONAL (OPTIONAL wins)',
  inferLectureType('051A - Academy Advanced DSA - Contest 2 Discussion - Optional Class'),
  'OPTIONAL'
)
expect(
  'DP lecture → CORE',
  inferLectureType('095 - DSA - DP 1 - One Dimensional'),
  'CORE'
)
expect(
  'Graphs lecture → CORE',
  inferLectureType('099 - DSA - Graphs 1 - Introduction, DFS & Cycle Detection'),
  'CORE'
)
expect(
  'Binary Search → CORE',
  inferLectureType('044 - Advanced DSA - Searching 1 - Binary Search on Array'),
  'CORE'
)
expect(
  'Doubt Solving → OPTIONAL',
  inferLectureType('012 - Doubt Solving Session'),
  'OPTIONAL'
)
expect(
  'Lab Session → OPTIONAL',
  inferLectureType('020 - Lab Session 3'),
  'OPTIONAL'
)
expect(
  'Revision → REVISION',
  inferLectureType('088 - Revision - Arrays and Strings'),
  'REVISION'
)
expect(
  'Problem Solving Session (no optional) → PRACTICE',
  inferLectureType('033 - Problem Solving Session 2'),
  'PRACTICE'
)
expect(
  'Contest Discussion (no optional) → DISCUSSION',
  inferLectureType('052 - Contest 2 Discussion'),
  'DISCUSSION'
)

// ─── inferPriority ───────────────────────────────────────────────────────────

section('inferPriority')

expect(
  'OPTIONAL → LOW',
  inferPriority('OPTIONAL', 'anything'),
  'LOW'
)
expect(
  'DISCUSSION → LOW',
  inferPriority('DISCUSSION', 'Contest 2 Discussion'),
  'LOW'
)
expect(
  'CONTEST → MEDIUM',
  inferPriority('CONTEST', 'Contest 2'),
  'MEDIUM'
)
expect(
  'REVISION → MEDIUM',
  inferPriority('REVISION', 'Revision - Arrays'),
  'MEDIUM'
)
expect(
  'PRACTICE → MEDIUM',
  inferPriority('PRACTICE', 'Problem Solving Session'),
  'MEDIUM'
)
expect(
  'CORE + DP keyword → HIGH',
  inferPriority('CORE', '095 - DSA - DP 1 - One Dimensional'),
  'HIGH'
)
expect(
  'CORE + Graphs keyword → HIGH',
  inferPriority('CORE', '099 - DSA - Graphs 1 - Introduction, DFS & Cycle Detection'),
  'HIGH'
)
expect(
  'CORE + Binary Search → HIGH',
  inferPriority('CORE', '044 - Advanced DSA - Searching 1 - Binary Search on Array'),
  'HIGH'
)
expect(
  'CORE + no high keyword → MEDIUM',
  inferPriority('CORE', '001 - Introduction to the Course'),
  'MEDIUM'
)
expect(
  'CORE + Linked List → HIGH',
  inferPriority('CORE', 'Linked List - Reversal'),
  'HIGH'
)

// ─── inferEstimatedMinutes ───────────────────────────────────────────────────

section('inferEstimatedMinutes')

expect('stored value wins', inferEstimatedMinutes('CORE', 'HIGH', 45), 45)
expect('stored value wins even if low', inferEstimatedMinutes('OPTIONAL', 'LOW', 30), 30)
expect('CORE HIGH fallback → 120', inferEstimatedMinutes('CORE', 'HIGH', null), 120)
expect('CORE MEDIUM fallback → 90', inferEstimatedMinutes('CORE', 'MEDIUM', null), 90)
expect('CORE LOW fallback → 60', inferEstimatedMinutes('CORE', 'LOW', null), 60)
expect('PRACTICE fallback → 90', inferEstimatedMinutes('PRACTICE', 'MEDIUM', null), 90)
expect('CONTEST fallback → 120', inferEstimatedMinutes('CONTEST', 'MEDIUM', null), 120)
expect('REVISION fallback → 60', inferEstimatedMinutes('REVISION', 'MEDIUM', null), 60)
expect('DISCUSSION fallback → 45', inferEstimatedMinutes('DISCUSSION', 'LOW', null), 45)
expect('OPTIONAL fallback → 60', inferEstimatedMinutes('OPTIONAL', 'LOW', null), 60)

// ─── makeLectureId ───────────────────────────────────────────────────────────

section('makeLectureId')

expect(
  'Advanced DSA 1 + 29 → ADVANCED_DSA_1_029',
  makeLectureId('Advanced DSA 1', 29, null),
  'ADVANCED_DSA_1_029'
)
expect(
  'Advanced DSA 2 + 39 → ADVANCED_DSA_2_039',
  makeLectureId('Advanced DSA 2', 39, null),
  'ADVANCED_DSA_2_039'
)
expect(
  'Introduction to Problem Solving (Intermediate) 1 + 3',
  makeLectureId('Introduction to Problem Solving (Intermediate) 1', 3, null),
  'INTRODUCTION_TO_PROBLEM_SOLVING_INTERMEDIATE_1_003'
)
expect(
  'null sourceOrder falls back to DB id',
  makeLectureId('Advanced DSA 1', null, 42),
  'ADVANCED_DSA_1_42'
)
expect(
  'null module defaults to GENERAL',
  makeLectureId(null, 1, null),
  'GENERAL_001'
)

// ─── Summary ─────────────────────────────────────────────────────────────────

console.log(`\n${'─'.repeat(40)}`)
console.log(`Results: ${passed} passed, ${failed} failed`)
if (failed > 0) process.exit(1)
