// Keyword sets — order matters only within each group
const OPTIONAL_KEYWORDS    = ['optional class', 'townhall', 'lab session', 'doubt solving']
const CORE_HIGH_KEYWORDS   = [
  'dp', 'dynamic programming', 'graph', 'tree', 'binary search',
  'two pointers', 'hashing', 'linked list', 'stack', 'queue',
  'heap', 'greedy', 'backtracking', 'recursion',
]

/**
 * Infer lecture type from title.
 * OPTIONAL always wins if the title contains any optional-class keyword.
 */
export function inferLectureType(title) {
  const t = (title ?? '').toLowerCase()
  if (OPTIONAL_KEYWORDS.some(k => t.includes(k))) return 'OPTIONAL'
  if (t.includes('contest') && t.includes('discussion')) return 'DISCUSSION'
  if (t.includes('contest'))                             return 'CONTEST'
  if (t.includes('revision'))                      return 'REVISION'
  if (t.includes('problem solving session'))       return 'PRACTICE'
  return 'CORE'
}

/**
 * Infer priority from type + title.
 * CORE lectures are further classified by topic keywords.
 */
export function inferPriority(type, title) {
  if (type === 'OPTIONAL')    return 'LOW'
  if (type === 'DISCUSSION')  return 'LOW'
  if (type === 'CONTEST')     return 'MEDIUM'
  if (type === 'REVISION')    return 'MEDIUM'
  if (type === 'PRACTICE')    return 'MEDIUM'
  // CORE
  const t = (title ?? '').toLowerCase()
  if (CORE_HIGH_KEYWORDS.some(k => t.includes(k))) return 'HIGH'
  return 'MEDIUM'
}

/**
 * Return stored minutes when available; otherwise infer from type + priority.
 * "Not stored" means null, 0, or the raw default of 60 cannot be distinguished —
 * so the stored value is always used if > 0. The table below is the fallback.
 */
export function inferEstimatedMinutes(type, priority, storedMinutes) {
  if (storedMinutes != null && storedMinutes > 0) return storedMinutes
  const defaults = {
    CORE:       { HIGH: 120, MEDIUM: 90, LOW: 60 },
    PRACTICE:   90,
    CONTEST:    120,
    REVISION:   60,
    DISCUSSION: 45,
    OPTIONAL:   60,
  }
  if (type === 'CORE') return defaults.CORE[priority] ?? 90
  return defaults[type] ?? 60
}

/**
 * Build a globally unique lecture id: MODULE_SLUG_SOURCECODE
 *
 * Examples:
 *   "Advanced DSA 1" + 29   → ADVANCED_DSA_1_029
 *   "Introduction to Problem Solving (Intermediate) 1" + 3 → INTRODUCTION_TO_PROBLEM_SOLVING_INTERMEDIATE_1_003
 */
export function makeLectureId(moduleName, sourceOrder, fallbackId) {
  const slug = (moduleName ?? 'GENERAL')
    .toUpperCase()
    .replace(/[^A-Z0-9]+/g, '_')
    .replace(/^_+|_+$/g, '')
  const code = makeSourceCode(sourceOrder, fallbackId)
  return `${slug}_${code}`
}

/** Zero-padded 3-digit source code, or fallback to DB id string. */
export function makeSourceCode(sourceOrder, fallbackId) {
  return sourceOrder != null ? String(sourceOrder).padStart(3, '0') : String(fallbackId)
}
