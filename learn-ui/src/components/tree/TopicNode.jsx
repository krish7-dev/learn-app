import { memo } from 'react'
import { Handle, Position } from '@xyflow/react'
import { STATUS_CONFIG, NODE_W } from '../../utils/treeConfig'

function TopicNode({ data, selected }) {
  const { raw, hasChildren, isCollapsed } = data
  const cfg = STATUS_CONFIG[raw.status] ?? STATUS_CONFIG.NO_NOTES
  const isRoot = raw.nodeType === 'ROOT'

  return (
    <div
      style={{
        width: NODE_W,
        minHeight: 72,
        background: 'var(--surface)',
        border: `1px solid ${selected ? cfg.color : cfg.color + '70'}`,
        borderLeft: `4px solid ${cfg.color}`,
        borderRadius: 8,
        padding: '8px 10px',
        cursor: 'pointer',
        boxShadow: selected ? `0 0 0 2px ${cfg.color}55` : '0 2px 8px rgba(0,0,0,0.3)',
        position: 'relative',
        userSelect: 'none',
        transition: 'box-shadow 0.15s',
      }}
    >
      <Handle type="target" position={Position.Left} style={{ opacity: 0, pointerEvents: 'none' }} />

      {/* Status dot + label */}
      <div style={{ display: 'flex', alignItems: 'center', gap: 5, marginBottom: 4 }}>
        <span style={{ width: 7, height: 7, borderRadius: '50%', background: cfg.color, flexShrink: 0 }} />
        <span style={{
          fontSize: 9, fontWeight: 600, color: cfg.color,
          textTransform: 'uppercase', letterSpacing: '0.05em',
        }}>
          {cfg.label}
        </span>
      </div>

      {/* Full label — no truncation */}
      <div style={{
        fontSize: isRoot ? 13 : 12,
        fontWeight: isRoot ? 700 : 500,
        color: 'var(--text)',
        lineHeight: 1.3,
        marginBottom: 6,
        wordBreak: 'break-word',
      }}>
        {raw.label}
      </div>

      {/* Progress bar */}
      {raw.progressPercent > 0 && (
        <div style={{ height: 3, background: 'var(--surface2)', borderRadius: 2, marginBottom: 5 }}>
          <div style={{
            height: '100%',
            width: `${Math.min(100, raw.progressPercent)}%`,
            background: raw.progressPercent >= 80 ? '#10b981' : raw.progressPercent >= 40 ? '#6366f1' : '#f59e0b',
            borderRadius: 2,
          }} />
        </div>
      )}

      {/* Badges */}
      <div style={{ display: 'flex', gap: 5 }}>
        {raw.weakAreaCount > 0 && (
          <span style={{ fontSize: 10, color: '#ef4444', fontWeight: 600 }}>{raw.weakAreaCount} ⚠</span>
        )}
        {raw.revisionDueCount > 0 && (
          <span style={{ fontSize: 10, color: '#f59e0b', fontWeight: 600 }}>{raw.revisionDueCount} ↻</span>
        )}
      </div>

      {/* Collapse/expand indicator */}
      {hasChildren && (
        <div style={{
          position: 'absolute', right: -10, top: '50%', transform: 'translateY(-50%)',
          width: 18, height: 18, borderRadius: '50%',
          background: 'var(--surface2)', border: '1px solid var(--border)',
          display: 'flex', alignItems: 'center', justifyContent: 'center',
          fontSize: 10, color: 'var(--muted)', zIndex: 10,
          pointerEvents: 'none',
        }}>
          {isCollapsed ? '+' : '−'}
        </div>
      )}

      <Handle type="source" position={Position.Right} style={{ opacity: 0, pointerEvents: 'none' }} />
    </div>
  )
}

export default memo(TopicNode)
