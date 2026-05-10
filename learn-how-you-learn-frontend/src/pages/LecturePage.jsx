import { useState, useRef, useEffect } from 'react'
import { useParams, Link, useNavigate } from 'react-router-dom'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import ReactMarkdown from 'react-markdown'
import remarkGfm from 'remark-gfm'
import { lectureApi } from '../api/lectureApi'
import LoadingSpinner from '../components/common/LoadingSpinner'
import ErrorMessage from '../components/common/ErrorMessage'
import ConfirmDeleteModal from '../components/common/ConfirmDeleteModal'
import { formatDate } from '../utils/formatters'

const CONFUSION_TYPES = [
  { key: 'THEORY_UNCLEAR', label: '🤔 Theory unclear' },
  { key: 'EXAMPLE_UNCLEAR', label: '📝 Example unclear' },
  { key: 'WHEN_TO_USE_UNCLEAR', label: '❓ When to use?' },
  { key: 'CODE_CONFUSING', label: '💻 Code confusing' },
  { key: 'EDGE_CASE_CONFUSING', label: '⚠️ Edge case' },
  { key: 'LOST_FOCUS', label: '😵 Lost focus' },
]

function Md({ children }) {
  if (!children) return null
  return (
    <div className="md">
      <ReactMarkdown remarkPlugins={[remarkGfm]}>{children}</ReactMarkdown>
    </div>
  )
}

function FlashCard({ front, back }) {
  const [flipped, setFlipped] = useState(false)
  return (
    <div
      className="card"
      onClick={() => setFlipped(f => !f)}
      style={{ cursor: 'pointer', borderLeft: '3px solid var(--accent)', minHeight: 80, display: 'flex', flexDirection: 'column', justifyContent: 'center' }}
    >
      <div style={{ fontSize: 10, color: 'var(--muted)', marginBottom: 6, textTransform: 'uppercase', letterSpacing: 0.5 }}>
        {flipped ? 'Answer' : 'Question — tap to reveal'}
      </div>
      <div style={{ fontSize: 13 }}>{flipped ? back : front}</div>
    </div>
  )
}

function LearnTab({ notes }) {
  if (!notes?.fullCleanNotes) return <div className="empty-state">No notes yet. Generate them above.</div>
  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 20 }}>
      <div className="card" style={{ padding: '24px 28px' }}>
        <Md>{notes.fullCleanNotes}</Md>
      </div>
      {notes.chatAdditions && (
        <div className="card" style={{ padding: '24px 28px', borderLeft: '3px solid #8b5cf6' }}>
          <div className="section-title" style={{ marginBottom: 12 }}>Notes from Chat</div>
          <Md>{notes.chatAdditions}</Md>
        </div>
      )}
    </div>
  )
}

function SimpleTab({ notes }) {
  if (!notes?.simpleExplanation && !notes?.practicalUsage) return <div className="empty-state">No notes yet.</div>
  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 20 }}>
      {notes.simpleExplanation && (
        <div className="card" style={{ padding: '24px 28px', borderLeft: '3px solid #10b981' }}>
          <div className="section-title" style={{ marginBottom: 12 }}>Simple Explanation</div>
          <Md>{notes.simpleExplanation}</Md>
        </div>
      )}
      {notes.practicalUsage && (
        <div className="card" style={{ padding: '24px 28px', borderLeft: '3px solid #f59e0b' }}>
          <div className="section-title" style={{ marginBottom: 12 }}>Practical Usage</div>
          <Md>{notes.practicalUsage}</Md>
        </div>
      )}
    </div>
  )
}

function ExamplesTab({ notes }) {
  const examples = Array.isArray(notes?.examples) ? notes.examples : []
  if (!examples.length) return <div className="empty-state">No examples yet.</div>
  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 20 }}>
      {examples.map((ex, i) => (
        <div key={i} className="card" style={{ padding: '20px 24px' }}>
          {ex.title && <div style={{ fontWeight: 600, marginBottom: 12, fontSize: 15 }}>{ex.title}</div>}
          {ex.code && (
            <Md>{`\`\`\`${ex.language || ''}\n${ex.code}\n\`\`\``}</Md>
          )}
          {ex.explanation && (
            <div style={{ marginTop: 12, fontSize: 13, color: 'var(--muted)', lineHeight: 1.7 }}>{ex.explanation}</div>
          )}
        </div>
      ))}
    </div>
  )
}

function MistakeCard({ m }) {
  if (typeof m === 'string') {
    return (
      <div className="card" style={{ borderLeft: '3px solid var(--danger)' }}>
        <div style={{ display: 'flex', gap: 10, alignItems: 'flex-start' }}>
          <span style={{ color: 'var(--danger)', flexShrink: 0 }}>✗</span>
          <span style={{ fontSize: 13 }}>{m}</span>
        </div>
      </div>
    )
  }
  return (
    <div className="card" style={{ borderLeft: '3px solid var(--danger)', display: 'flex', flexDirection: 'column', gap: 12 }}>
      <div style={{ display: 'flex', gap: 10, alignItems: 'flex-start' }}>
        <span style={{ color: 'var(--danger)', flexShrink: 0, fontWeight: 700 }}>✗</span>
        <span style={{ fontSize: 14, fontWeight: 600 }}>{m.mistake}</span>
      </div>
      {m.explanation && (
        <div style={{ fontSize: 13, color: 'var(--muted)' }}>{m.explanation}</div>
      )}
      {m.wrong && (
        <div>
          <div style={{ fontSize: 11, color: 'var(--danger)', marginBottom: 4, fontWeight: 600 }}>✗ WRONG</div>
          <Md>{`\`\`\`\n${m.wrong}\n\`\`\``}</Md>
        </div>
      )}
      {m.right && (
        <div>
          <div style={{ fontSize: 11, color: 'var(--success)', marginBottom: 4, fontWeight: 600 }}>✓ CORRECT</div>
          <Md>{`\`\`\`\n${m.right}\n\`\`\``}</Md>
        </div>
      )}
    </div>
  )
}

function EdgeCard({ e }) {
  if (typeof e === 'string') {
    return (
      <div className="card" style={{ borderLeft: '3px solid var(--warn)', display: 'flex', gap: 10, alignItems: 'flex-start' }}>
        <span style={{ color: 'var(--warn)', flexShrink: 0 }}>⚠</span>
        <span style={{ fontSize: 13 }}>{e}</span>
      </div>
    )
  }
  return (
    <div className="card" style={{ borderLeft: '3px solid var(--warn)', display: 'flex', flexDirection: 'column', gap: 10 }}>
      <div style={{ display: 'flex', gap: 10, alignItems: 'flex-start' }}>
        <span style={{ color: 'var(--warn)', flexShrink: 0 }}>⚠</span>
        <span style={{ fontSize: 14, fontWeight: 600 }}>{e.case}</span>
      </div>
      {e.example && (
        <div>
          <div style={{ fontSize: 11, color: 'var(--muted)', marginBottom: 4 }}>Example</div>
          <Md>{`\`\`\`\n${e.example}\n\`\`\``}</Md>
        </div>
      )}
      {e.how_to_handle && (
        <div style={{ fontSize: 13, color: 'var(--success)' }}>→ {e.how_to_handle}</div>
      )}
    </div>
  )
}

function MistakesTab({ notes }) {
  const mistakes = Array.isArray(notes?.mistakesToAvoid) ? notes.mistakesToAvoid : []
  const edges = Array.isArray(notes?.edgeCases) ? notes.edgeCases : []
  if (!mistakes.length && !edges.length) return <div className="empty-state">No mistakes or edge cases noted. Regenerate notes to populate this tab.</div>
  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 24 }}>
      {mistakes.length > 0 && (
        <div>
          <div className="section-title" style={{ marginBottom: 12 }}>Mistakes to Avoid</div>
          <div style={{ display: 'flex', flexDirection: 'column', gap: 12 }}>
            {mistakes.map((m, i) => <MistakeCard key={i} m={m} />)}
          </div>
        </div>
      )}
      {edges.length > 0 && (
        <div>
          <div className="section-title" style={{ marginBottom: 12 }}>Edge Cases</div>
          <div style={{ display: 'flex', flexDirection: 'column', gap: 12 }}>
            {edges.map((e, i) => <EdgeCard key={i} e={e} />)}
          </div>
        </div>
      )}
    </div>
  )
}

function ReviseTab({ notes }) {
  const flashcards = Array.isArray(notes?.flashcards) ? notes.flashcards : []
  if (!notes?.revisionNotes && !flashcards.length) return <div className="empty-state">No revision material yet.</div>
  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 24 }}>
      {notes?.revisionNotes && (
        <div className="card" style={{ padding: '20px 24px', borderLeft: '3px solid var(--accent)' }}>
          <div className="section-title" style={{ marginBottom: 12 }}>Quick Revision Notes</div>
          <Md>{notes.revisionNotes}</Md>
        </div>
      )}
      {flashcards.length > 0 && (
        <div>
          <div className="section-title" style={{ marginBottom: 12 }}>Flashcards — tap to flip</div>
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(220px, 1fr))', gap: 10 }}>
            {flashcards.map((fc, i) => (
              <FlashCard key={i} front={fc.front} back={fc.back} />
            ))}
          </div>
        </div>
      )}
    </div>
  )
}

function PracticeTab({ notes }) {
  const practice = Array.isArray(notes?.practiceQuestions) ? notes.practiceQuestions : []
  const interview = Array.isArray(notes?.interviewQuestions) ? notes.interviewQuestions : []
  if (!practice.length && !interview.length) return <div className="empty-state">No practice questions yet.</div>
  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 24 }}>
      {interview.length > 0 && (
        <div>
          <div className="section-title" style={{ marginBottom: 12 }}>Interview Questions</div>
          <div style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
            {interview.map((q, i) => (
              <div key={i} className="card" style={{ display: 'flex', gap: 12, alignItems: 'flex-start' }}>
                <span style={{ color: 'var(--accent)', fontWeight: 700, flexShrink: 0 }}>Q{i + 1}</span>
                <span style={{ fontSize: 13 }}>{typeof q === 'string' ? q : JSON.stringify(q)}</span>
              </div>
            ))}
          </div>
        </div>
      )}
      {practice.length > 0 && (
        <div>
          <div className="section-title" style={{ marginBottom: 12 }}>Practice Problems</div>
          <div style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
            {practice.map((q, i) => (
              <div key={i} className="card" style={{ display: 'flex', gap: 12, alignItems: 'flex-start' }}>
                <span style={{ color: 'var(--success)', fontWeight: 700, flexShrink: 0 }}>{i + 1}.</span>
                <span style={{ fontSize: 13 }}>{typeof q === 'string' ? q : JSON.stringify(q)}</span>
              </div>
            ))}
          </div>
        </div>
      )}
    </div>
  )
}

function NotesSection({ notes }) {
  const [sub, setSub] = useState('learn')
  const subTabs = [
    { key: 'learn', label: '📖 Learn' },
    { key: 'simple', label: '💡 Simple' },
    { key: 'examples', label: '🔍 Examples' },
    { key: 'mistakes', label: '⚠️ Mistakes' },
    { key: 'revise', label: '🔁 Revise' },
    { key: 'practice', label: '🏋️ Practice' },
  ]

  if (!notes) return <div className="empty-state">No notes yet. Click Generate Notes above.</div>

  return (
    <div>
      <div className="tabs" style={{ marginBottom: 20 }}>
        {subTabs.map(t => (
          <button key={t.key} className={`tab ${sub === t.key ? 'active' : ''}`} onClick={() => setSub(t.key)}>
            {t.label}
          </button>
        ))}
      </div>
      {sub === 'learn' && <LearnTab notes={notes} />}
      {sub === 'simple' && <SimpleTab notes={notes} />}
      {sub === 'examples' && <ExamplesTab notes={notes} />}
      {sub === 'mistakes' && <MistakesTab notes={notes} />}
      {sub === 'revise' && <ReviseTab notes={notes} />}
      {sub === 'practice' && <PracticeTab notes={notes} />}
    </div>
  )
}

function ChatDrawer({ lectureId }) {
  const [open, setOpen] = useState(false)
  const [msg, setMsg] = useState('')
  const [messages, setMessages] = useState([])
  const [savedIdx, setSavedIdx] = useState(new Set())
  const bottomRef = useRef()
  const inputRef = useRef()
  const qc = useQueryClient()

  const chat = useMutation({
    mutationFn: (message) => lectureApi.chat(lectureId, message),
    onSuccess: (data) => {
      setMessages((prev) => [...prev, { role: 'ASSISTANT', text: data.message }])
    },
  })

  const saveToNotes = useMutation({
    mutationFn: ({ content }) => lectureApi.addToNotes(lectureId, content),
    onSuccess: (data, variables) => {
      qc.setQueryData(['lecture', String(lectureId)], data)
      setSavedIdx((prev) => new Set([...prev, variables.index]))
    },
  })

  const send = () => {
    if (!msg.trim()) return
    const text = msg
    setMessages((prev) => [...prev, { role: 'USER', text }])
    setMsg('')
    chat.mutate(text)
  }

  useEffect(() => {
    bottomRef.current?.scrollIntoView({ behavior: 'smooth' })
  }, [messages])

  useEffect(() => {
    if (open) setTimeout(() => inputRef.current?.focus(), 100)
  }, [open])

  return (
    <>
      {/* Floating button */}
      <button
        onClick={() => setOpen(o => !o)}
        style={{
          position: 'fixed',
          bottom: 24,
          right: 24,
          width: 52,
          height: 52,
          borderRadius: '50%',
          background: open ? 'var(--surface2)' : 'var(--accent)',
          border: '1px solid var(--border)',
          boxShadow: '0 4px 20px rgba(0,0,0,0.4)',
          fontSize: 22,
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          zIndex: 300,
          transition: 'all 0.2s',
          cursor: 'pointer',
        }}
        title="Ask Tutor"
      >
        {open ? '✕' : '💬'}
      </button>

      {/* Chat panel */}
      {open && (
        <div style={{
          position: 'fixed',
          bottom: 88,
          right: 24,
          width: 'min(420px, calc(100vw - 48px))',
          height: 'min(520px, calc(100vh - 120px))',
          background: 'var(--surface)',
          border: '1px solid var(--border)',
          borderRadius: 12,
          boxShadow: '0 8px 40px rgba(0,0,0,0.5)',
          display: 'flex',
          flexDirection: 'column',
          zIndex: 299,
          overflow: 'hidden',
        }}>
          {/* Header */}
          <div style={{
            padding: '12px 16px',
            borderBottom: '1px solid var(--border)',
            display: 'flex',
            alignItems: 'center',
            gap: 8,
            flexShrink: 0,
          }}>
            <span style={{ fontSize: 16 }}>💬</span>
            <span style={{ fontWeight: 600, fontSize: 13 }}>Ask Tutor</span>
            {messages.length > 0 && (
              <button
                onClick={() => setMessages([])}
                style={{ marginLeft: 'auto', fontSize: 11, color: 'var(--muted)' }}
              >
                Clear
              </button>
            )}
          </div>

          {/* Messages */}
          <div style={{ flex: 1, overflowY: 'auto', padding: '12px 16px', display: 'flex', flexDirection: 'column', gap: 10 }}>
            {messages.length === 0 && (
              <div style={{ textAlign: 'center', color: 'var(--muted)', fontSize: 13, marginTop: 20 }}>
                Ask anything about this lecture.
              </div>
            )}
            {messages.map((m, i) => (
              <div key={i} style={{
                alignSelf: m.role === 'USER' ? 'flex-end' : 'flex-start',
                maxWidth: '90%',
                display: 'flex',
                flexDirection: 'column',
                gap: 4,
              }}>
                <div style={{
                  background: m.role === 'USER' ? 'var(--accent)' : 'var(--surface2)',
                  padding: '8px 12px',
                  borderRadius: 10,
                  fontSize: 13,
                }}>
                  {m.role === 'USER'
                    ? <span style={{ whiteSpace: 'pre-wrap', lineHeight: 1.6 }}>{m.text}</span>
                    : <Md>{m.text}</Md>
                  }
                </div>
                {m.role === 'ASSISTANT' && (
                  <div style={{ display: 'flex', flexDirection: 'column', gap: 2 }}>
                    <button
                      onClick={() => saveToNotes.mutate({ content: m.text, index: i })}
                      disabled={savedIdx.has(i) || saveToNotes.isPending}
                      style={{
                        alignSelf: 'flex-start',
                        fontSize: 11,
                        color: savedIdx.has(i) ? 'var(--success)' : 'var(--muted)',
                        padding: '2px 6px',
                        borderRadius: 4,
                        border: '1px solid var(--border)',
                        background: 'transparent',
                        cursor: savedIdx.has(i) ? 'default' : 'pointer',
                      }}
                    >
                      {saveToNotes.isPending && !savedIdx.has(i)
                        ? 'Saving…'
                        : savedIdx.has(i)
                        ? '✓ Saved to notes'
                        : '＋ Save to notes'}
                    </button>
                    {saveToNotes.error && (
                      <span style={{ fontSize: 11, color: 'var(--danger)' }}>
                        {saveToNotes.error.message}
                      </span>
                    )}
                  </div>
                )}
              </div>
            ))}
            {chat.isPending && (
              <div style={{ alignSelf: 'flex-start', color: 'var(--muted)', fontSize: 13 }}>Thinking…</div>
            )}
            <div ref={bottomRef} />
          </div>

          {/* Input */}
          <div style={{ padding: '10px 12px', borderTop: '1px solid var(--border)', display: 'flex', gap: 8, flexShrink: 0 }}>
            <textarea
              ref={inputRef}
              rows={2}
              value={msg}
              onChange={(e) => setMsg(e.target.value)}
              onKeyDown={(e) => { if (e.key === 'Enter' && !e.shiftKey) { e.preventDefault(); send() } }}
              placeholder="Ask… (Enter to send)"
              style={{ flex: 1, fontSize: 13, resize: 'none' }}
            />
            <button
              className="btn btn-primary btn-sm"
              onClick={send}
              disabled={chat.isPending || !msg.trim()}
              style={{ alignSelf: 'flex-end' }}
            >
              Send
            </button>
          </div>
          {chat.error && (
            <div className="error-box" style={{ margin: '0 12px 10px', fontSize: 12 }}>{chat.error.message}</div>
          )}
        </div>
      )}
    </>
  )
}

function ConfusionPanel({ lectureId }) {
  const [note, setNote] = useState('')
  const log = useMutation({ mutationFn: (data) => lectureApi.logConfusion(lectureId, data) })

  return (
    <div>
      <div className="section-title" style={{ marginBottom: 12 }}>What's confusing?</div>
      <div style={{ display: 'flex', flexWrap: 'wrap', gap: 8, marginBottom: 16 }}>
        {CONFUSION_TYPES.map(({ key, label }) => (
          <button key={key} className="btn btn-secondary btn-sm"
            disabled={log.isPending}
            onClick={() => log.mutate({ confusionType: key, note: note || undefined })}>
            {label}
          </button>
        ))}
      </div>
      <input value={note} onChange={(e) => setNote(e.target.value)} placeholder="Optional note about what's confusing…" />
      {log.isSuccess && <div style={{ color: 'var(--success)', fontSize: 13, marginTop: 8 }}>✓ Logged</div>}
      {log.error && <div className="error-box" style={{ marginTop: 8 }}>{log.error.message}</div>}
    </div>
  )
}

export default function LecturePage() {
  const { id } = useParams()
  const navigate = useNavigate()
  const [tab, setTab] = useState('notes')
  const [showDeleteModal, setShowDeleteModal] = useState(false)
  const qc = useQueryClient()

  const { data: lecture, isLoading, error } = useQuery({
    queryKey: ['lecture', id],
    queryFn: () => lectureApi.getById(id),
  })

  const generateNotes = useMutation({
    mutationFn: () => lectureApi.generateNotes(id),
    onSuccess: (data) => qc.setQueryData(['lecture', id], data),
  })

  const deleteLecture = useMutation({
    mutationFn: () => lectureApi.delete(id),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['dashboard'] })
      navigate(lecture?.courseId ? `/courses/${lecture.courseId}` : '/courses')
    },
  })

  if (isLoading) return <LoadingSpinner />
  if (error) return <ErrorMessage message={error.message} />

  const tabs = ['notes', 'confusions', 'raw']

  return (
    <div>
      <div style={{ marginBottom: 16 }}>
        <div style={{ fontSize: 12, color: 'var(--muted)', marginBottom: 4 }}>
          <Link to={`/courses/${lecture.courseId}`} style={{ color: 'var(--accent)' }}>← Course</Link>
          {lecture.moduleName && ` · ${lecture.moduleName}`}
        </div>
        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', flexWrap: 'wrap', gap: 8 }}>
          <h1 style={{ fontSize: 20, fontWeight: 600 }}>{lecture.title}</h1>
          <div style={{ display: 'flex', gap: 8 }}>
            <button
              className="btn btn-primary"
              onClick={() => generateNotes.mutate()}
              disabled={generateNotes.isPending}
            >
              {generateNotes.isPending ? '⏳ Generating…' : lecture.notes ? '↻ Regenerate Notes' : '✨ Generate Notes'}
            </button>
            <button className="btn btn-danger btn-sm" onClick={() => setShowDeleteModal(true)}>Delete</button>
          </div>
        </div>
        {generateNotes.error && <div className="error-box" style={{ marginTop: 8 }}>{generateNotes.error.message}</div>}
      </div>

      <div className="tabs">
        {tabs.map((t) => (
          <button key={t} className={`tab ${tab === t ? 'active' : ''}`} onClick={() => setTab(t)}>
            {t === 'notes' ? '📝 Notes' : t === 'confusions' ? '😵 Confusions' : '📄 Raw'}
          </button>
        ))}
      </div>

      {tab === 'notes' && <NotesSection notes={lecture.notes} />}
      {tab === 'confusions' && <ConfusionPanel lectureId={id} />}
      {tab === 'raw' && (
        <div className="card prose" style={{ maxHeight: '60vh', overflowY: 'auto' }}>
          {lecture.rawContent || <span style={{ color: 'var(--muted)' }}>No raw content yet.</span>}
        </div>
      )}

      <ChatDrawer lectureId={id} />

      {showDeleteModal && (
        <ConfirmDeleteModal
          title="Delete Lecture"
          itemName={lecture.title}
          isPending={deleteLecture.isPending}
          onConfirm={() => deleteLecture.mutate()}
          onClose={() => setShowDeleteModal(false)}
        />
      )}
    </div>
  )
}
