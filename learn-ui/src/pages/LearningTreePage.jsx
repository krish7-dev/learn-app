import { useState, useMemo, useCallback, useEffect, useRef } from 'react'
import { Link } from 'react-router-dom'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { ReactFlow, Controls, MiniMap, Background, BackgroundVariant } from '@xyflow/react'
import '@xyflow/react/dist/style.css'
import { treeApi } from '../api/treeApi'
import LoadingSpinner from '../components/common/LoadingSpinner'
import ErrorMessage from '../components/common/ErrorMessage'
import TopicNode from '../components/tree/TopicNode'
import { STATUS_CONFIG, NODE_W, NODE_H, H_GAP, V_GAP } from '../utils/treeConfig'

// Stable reference — must be outside component to avoid React Flow warnings
const nodeTypes = { topicNode: TopicNode }

// ── Layout ──────────────────────────────────────────────────────────────────

function buildLayout(forest, collapsedIds) {
  if (!forest?.length) return { nodes: [], edges: [] }

  let leafIdx = 0

  // Left-to-right: Y position determined by leaf index, X by depth
  function assignY(node) {
    const visible = collapsedIds.has(node.id) ? [] : (node.children ?? [])
    if (!visible.length) {
      node._y = leafIdx++ * (NODE_H + V_GAP)
    } else {
      visible.forEach(assignY)
      const ys = visible.map(c => c._y)
      node._y = (Math.min(...ys) + Math.max(...ys)) / 2
    }
  }
  forest.forEach(assignY)

  const nodes = []
  const edges = []

  function collect(node, parentId, depth) {
    const isCollapsed = collapsedIds.has(node.id)
    const hasChildren = !!(node.children?.length)
    nodes.push({
      id: String(node.id),
      type: 'topicNode',
      position: { x: depth * (NODE_W + H_GAP), y: node._y },
      data: { raw: node, hasChildren, isCollapsed: isCollapsed && hasChildren },
    })
    if (parentId !== null) {
      edges.push({
        id: `e-${parentId}-${node.id}`,
        source: String(parentId),
        target: String(node.id),
        type: 'smoothstep',
      })
    }
    if (!isCollapsed) {
      node.children?.forEach(c => collect(c, node.id, depth + 1))
    }
  }
  forest.forEach(r => collect(r, null, 0))

  return { nodes, edges }
}

// ── Shell pruning ─────────────────────────────────────────────────────────────

function pruneShells(nodes) {
  return nodes
    .map(n => ({ ...n, children: pruneShells(n.children ?? []) }))
    .filter(n => n.nodeType !== 'SHELL')
}

// ── Filter ───────────────────────────────────────────────────────────────────

function hasStatus(node, s) {
  return node.status === s || node.children?.some(c => hasStatus(c, s))
}

function filterTree(nodes, query, statusFilter) {
  return nodes
    .filter(node => {
      const q = !query || node.label.toLowerCase().includes(query.toLowerCase())
      const s = !statusFilter || hasStatus(node, statusFilter)
      return q || s
    })
    .map(node => ({ ...node, children: node.children ? filterTree(node.children, query, statusFilter) : [] }))
}

// ── Collect IDs of nodes with children below depth threshold ─────────────────

function collectDeepIds(nodes, depth = 0, collapseFromDepth = 1) {
  const ids = new Set()
  for (const node of nodes) {
    if (depth >= collapseFromDepth && node.children?.length) ids.add(node.id)
    for (const id of collectDeepIds(node.children ?? [], depth + 1, collapseFromDepth)) ids.add(id)
  }
  return ids
}

// ── Count descendants ────────────────────────────────────────────────────────

function countDescendants(node) {
  return (node.children ?? []).reduce((sum, c) => sum + 1 + countDescendants(c), 0)
}

// ── Text tree view ────────────────────────────────────────────────────────────

function TextTreeNode({ node, depth, collapsedIds, onNodeClick, selectedId }) {
  const cfg = STATUS_CONFIG[node.status] ?? STATUS_CONFIG.NO_NOTES
  const hasChildren = !!(node.children?.length)
  const isCollapsed = collapsedIds.has(node.id)
  const isSelected = node.id === selectedId
  const visibleChildren = isCollapsed ? [] : (node.children ?? [])

  return (
    <>
      <div
        onClick={() => onNodeClick(node)}
        style={{
          display: 'flex', alignItems: 'center', gap: 5,
          padding: '2px 6px 2px ' + (6 + depth * 18) + 'px',
          cursor: 'pointer', borderRadius: 3, userSelect: 'none',
          background: isSelected ? 'var(--surface2)' : 'transparent',
        }}
        onMouseEnter={e => { if (!isSelected) e.currentTarget.style.background = 'var(--surface2)' }}
        onMouseLeave={e => { if (!isSelected) e.currentTarget.style.background = 'transparent' }}
      >
        <span style={{ fontSize: 8, color: 'var(--muted)', width: 10, textAlign: 'center', flexShrink: 0 }}>
          {hasChildren ? (isCollapsed ? '▶' : '▾') : ''}
        </span>
        <span style={{ width: 7, height: 7, borderRadius: '50%', background: cfg.color, flexShrink: 0 }} />
        <span style={{
          fontSize: depth === 0 ? 13 : 12,
          fontWeight: depth === 0 ? 700 : hasChildren ? 600 : 400,
          color: 'var(--text)', lineHeight: 1.7, flex: 1,
        }}>
          {node.label}
        </span>
        {node.weakAreaCount > 0 && (
          <span style={{ fontSize: 10, color: '#ef4444', flexShrink: 0 }}>⚠ {node.weakAreaCount}</span>
        )}
        {node.revisionDueCount > 0 && (
          <span style={{ fontSize: 10, color: '#f59e0b', flexShrink: 0 }}>↻ {node.revisionDueCount}</span>
        )}
        {isCollapsed && hasChildren && (
          <span style={{ fontSize: 10, color: 'var(--muted)', flexShrink: 0 }}>+{countDescendants(node)}</span>
        )}
      </div>
      {visibleChildren.map(child => (
        <TextTreeNode
          key={child.id}
          node={child}
          depth={depth + 1}
          collapsedIds={collapsedIds}
          onNodeClick={onNodeClick}
          selectedId={selectedId}
        />
      ))}
    </>
  )
}

function TextTreeView({ forest, collapsedIds, onNodeClick, selectedId }) {
  if (!forest?.length) return null
  return (
    <div style={{ padding: '6px 0', overflowY: 'auto', height: '100%' }}>
      {forest.map(root => (
        <TextTreeNode
          key={root.id}
          node={root}
          depth={0}
          collapsedIds={collapsedIds}
          onNodeClick={onNodeClick}
          selectedId={selectedId}
        />
      ))}
    </div>
  )
}

// ── Progress bar ──────────────────────────────────────────────────────────────

function ProgressBar({ value }) {
  return (
    <div style={{ height: 4, background: 'var(--border)', borderRadius: 2, overflow: 'hidden' }}>
      <div style={{
        height: '100%', width: `${Math.min(100, value)}%`,
        background: value >= 80 ? '#10b981' : value >= 40 ? '#6366f1' : '#f59e0b',
        borderRadius: 2,
      }} />
    </div>
  )
}

// ── Detail panel ─────────────────────────────────────────────────────────────

function NodeDetailPanel({ nodeId, onClose }) {
  const { data, isLoading, error } = useQuery({
    queryKey: ['tree-node', nodeId],
    queryFn: () => treeApi.getNodeDetail(nodeId),
    enabled: !!nodeId,
  })

  if (isLoading) return <div style={{ padding: 24 }}><LoadingSpinner /></div>
  if (error)     return <div style={{ padding: 16 }}><ErrorMessage message={error.message} /></div>
  if (!data)     return null

  const cfg = STATUS_CONFIG[data.status] ?? STATUS_CONFIG.NO_NOTES

  return (
    <div style={{ padding: 14 }}>
      <div style={{ display: 'flex', alignItems: 'flex-start', justifyContent: 'space-between', marginBottom: 12 }}>
        <div style={{ flex: 1, minWidth: 0 }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: 6, marginBottom: 4 }}>
            <span style={{ width: 8, height: 8, borderRadius: '50%', background: cfg.color, flexShrink: 0 }} />
            <span className="badge" style={{ background: cfg.color + '20', color: cfg.color, fontSize: 10 }}>
              {cfg.label}
            </span>
          </div>
          <h2 style={{ fontSize: 14, fontWeight: 700, lineHeight: 1.3, wordBreak: 'break-word' }}>{data.label}</h2>
          <p style={{ fontSize: 11, color: 'var(--muted)', marginTop: 2 }}>{data.nodeType}</p>
        </div>
        <button
          className="btn btn-secondary"
          style={{ fontSize: 12, padding: '2px 7px', marginLeft: 8, flexShrink: 0 }}
          onClick={onClose}>✕</button>
      </div>

      <div style={{ marginBottom: 12 }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 4 }}>
          <span style={{ fontSize: 11, color: 'var(--muted)' }}>Progress</span>
          <span style={{ fontSize: 11, fontWeight: 600 }}>{Math.round(data.progressPercent)}%</span>
        </div>
        <ProgressBar value={data.progressPercent} />
      </div>

      {(data.weakAreaCount > 0 || data.revisionDueCount > 0) && (
        <div style={{ display: 'flex', gap: 6, flexWrap: 'wrap', marginBottom: 12 }}>
          {data.weakAreaCount > 0 && (
            <span className="badge" style={{ background: '#ef444420', color: '#ef4444', fontSize: 10 }}>
              {data.weakAreaCount} weak{data.weakAreaCount > 1 ? ' areas' : ' area'}
            </span>
          )}
          {data.revisionDueCount > 0 && (
            <span className="badge" style={{ background: '#f59e0b20', color: '#f59e0b', fontSize: 10 }}>
              {data.revisionDueCount} revision{data.revisionDueCount > 1 ? 's' : ''} due
            </span>
          )}
        </div>
      )}

      {data.topicId && (
        <div className="card" style={{ padding: '10px 12px', marginBottom: 8 }}>
          <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: 5 }}>
            <p style={{ fontSize: 10, color: 'var(--muted)', textTransform: 'uppercase', fontWeight: 600, letterSpacing: '0.05em' }}>Topic</p>
            <Link to={`/topics/${data.topicId}`} style={{ fontSize: 10, color: 'var(--accent)' }}>Open →</Link>
          </div>
          <p style={{ fontSize: 13, fontWeight: 700, marginBottom: 5 }}>{data.topicName}</p>
          <div style={{ display: 'flex', gap: 5, flexWrap: 'wrap', marginBottom: 5 }}>
            <span className="badge" style={{ fontSize: 10 }}>{data.topicStatus?.replace('_', ' ')}</span>
            <span className="badge" style={{ fontSize: 10 }}>Mastery {data.masteryScore}/100</span>
          </div>
          {data.nextRevisionDue && (
            <p style={{ fontSize: 11, color: 'var(--muted)' }}>
              Revision: <strong style={{ color: 'var(--text)' }}>{data.nextRevisionDue}</strong>
            </p>
          )}
          {data.weakAreaSeverity && (
            <p style={{ fontSize: 11, color: '#ef4444', marginTop: 3 }}>Severity: {data.weakAreaSeverity}</p>
          )}
        </div>
      )}

      {data.lectureId && (
        <div className="card" style={{ padding: '10px 12px' }}>
          <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: 5 }}>
            <p style={{ fontSize: 10, color: 'var(--muted)', textTransform: 'uppercase', fontWeight: 600, letterSpacing: '0.05em' }}>Lecture</p>
            <Link to={`/lectures/${data.lectureId}`} style={{ fontSize: 10, color: 'var(--accent)' }}>Open →</Link>
          </div>
          <p style={{ fontSize: 13, fontWeight: 700, marginBottom: 5 }}>{data.lectureTitle}</p>
          <div style={{ display: 'flex', gap: 5, flexWrap: 'wrap' }}>
            <span className="badge" style={{ fontSize: 10 }}>{data.contentStatus?.replace('_', ' ')}</span>
            {data.estimatedMinutes && (
              <span className="badge" style={{ fontSize: 10 }}>{data.estimatedMinutes} min</span>
            )}
          </div>
        </div>
      )}
    </div>
  )
}

// ── Page ─────────────────────────────────────────────────────────────────────

export default function LearningTreePage() {
  const [search, setSearch]             = useState('')
  const [statusFilter, setStatusFilter] = useState(null)
  const [selectedNode, setSelectedNode] = useState(null)
  const [collapsedIds, setCollapsedIds] = useState(new Set())
  const [viewMode, setViewMode]         = useState('text')
  const flowRef = useRef(null)
  const qc = useQueryClient()

  const { data: tree = [], isLoading, error } = useQuery({
    queryKey: ['learning-tree'],
    queryFn: treeApi.getFullTree,
  })

  const reset = useMutation({
    mutationFn: treeApi.reset,
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['learning-tree'] })
      setSelectedNode(null)
      setCollapsedIds(new Set())
    },
  })

  const topicTree = useMemo(() => pruneShells(tree), [tree])

  // Start with everything below depth 1 collapsed — root's direct children visible, rest hidden
  useEffect(() => {
    if (topicTree.length) setCollapsedIds(collectDeepIds(topicTree, 0, 1))
  }, [topicTree])

  const filteredTree = useMemo(() => {
    if (!search && !statusFilter) return topicTree
    return filterTree(topicTree, search, statusFilter)
  }, [topicTree, search, statusFilter])

  const handleNodeClick = useCallback((raw) => {
    setSelectedNode(raw)
    if (raw.children?.length) {
      setCollapsedIds(prev => {
        const next = new Set(prev)
        next.has(raw.id) ? next.delete(raw.id) : next.add(raw.id)
        return next
      })
    }
  }, [])

  const { nodes: rfNodes, edges: rfEdges } = useMemo(
    () => buildLayout(filteredTree, collapsedIds),
    [filteredTree, collapsedIds]
  )

  // Re-fit whenever visible node count changes (expand/collapse/filter)
  useEffect(() => {
    if (flowRef.current && rfNodes.length) {
      flowRef.current.fitView({ padding: 0.15, duration: 350 })
    }
  }, [rfNodes.length])

  if (isLoading) return <LoadingSpinner />
  if (error)     return <ErrorMessage message={error.message} />

  return (
    <div style={{ height: 'calc(100vh - 60px)', display: 'flex', flexDirection: 'column', overflow: 'hidden' }}>

      {/* ── Toolbar ── */}
      <div style={{
        padding: '7px 12px', borderBottom: '1px solid var(--border)', flexShrink: 0,
        display: 'flex', gap: 8, alignItems: 'center', flexWrap: 'wrap',
      }}>
        <h1 style={{ fontSize: 15, fontWeight: 700, marginRight: 4, flexShrink: 0 }}>Knowledge Tree</h1>

        <input
          type="text" className="input tree-search"
          placeholder="Search..."
          value={search}
          onChange={e => setSearch(e.target.value)}
          style={{ width: 150, margin: 0, flexShrink: 0 }}
        />

        <div style={{ display: 'flex', gap: 3, flexWrap: 'wrap' }}>
          <button
            onClick={() => setStatusFilter(null)}
            className={!statusFilter ? 'btn btn-primary' : 'btn btn-secondary'}
            style={{ fontSize: 10, padding: '3px 7px' }}>All</button>
          {Object.entries(STATUS_CONFIG).map(([key, cfg]) => (
            <button
              key={key}
              onClick={() => setStatusFilter(f => f === key ? null : key)}
              className={statusFilter === key ? 'btn btn-primary' : 'btn btn-secondary'}
              style={{ fontSize: 10, padding: '3px 7px', display: 'flex', alignItems: 'center', gap: 3 }}>
              <span style={{ width: 5, height: 5, borderRadius: '50%', background: cfg.color, flexShrink: 0 }} />
              {cfg.label}
            </button>
          ))}
        </div>

        <div style={{ marginLeft: 'auto', display: 'flex', gap: 6, alignItems: 'center', flexShrink: 0 }}>
          <span style={{ fontSize: 11, color: 'var(--muted)' }}>{rfNodes.length} nodes</span>

          <div style={{ display: 'flex', background: 'var(--surface2)', borderRadius: 5, padding: 2, gap: 2 }}>
            <button
              className={viewMode === 'canvas' ? 'btn btn-primary' : 'btn btn-secondary'}
              style={{ fontSize: 10, padding: '2px 8px' }}
              onClick={() => setViewMode('canvas')}
              title="Canvas view">⬜ Canvas</button>
            <button
              className={viewMode === 'text' ? 'btn btn-primary' : 'btn btn-secondary'}
              style={{ fontSize: 10, padding: '2px 8px' }}
              onClick={() => setViewMode('text')}
              title="Compact text view">☰ Text</button>
          </div>

          <button
            className="btn btn-secondary"
            style={{ fontSize: 11, padding: '3px 8px' }}
            disabled={topicTree.length === 0}
            onClick={() => {
              const date = new Date().toISOString().slice(0, 10)
              const blob = new Blob([JSON.stringify(topicTree, null, 2)], { type: 'application/json' })
              const url = URL.createObjectURL(blob)
              const a = document.createElement('a')
              a.href = url
              a.download = `knowledge-tree-${date}.json`
              a.click()
              URL.revokeObjectURL(url)
            }}
            title="Download tree as JSON">
            ↓ Export
          </button>
          <button
            className="btn btn-secondary"
            style={{ fontSize: 11, padding: '3px 8px' }}
            disabled={reset.isPending}
            onClick={() => reset.mutate()}
            title="Rebuild tree from notes">
            {reset.isPending ? '…' : '↻ Rebuild'}
          </button>
        </div>
      </div>

      {/* ── Main area ── */}
      <div style={{ flex: 1, position: 'relative', overflow: 'hidden', minHeight: 0, display: 'flex' }}>

        {/* Canvas or Text view */}
        <div style={{ flex: 1, position: 'relative', overflow: 'hidden', minHeight: 0 }}>
          {rfNodes.length === 0 ? (
            <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center', height: '100%', color: 'var(--muted)' }}>
              {topicTree.length === 0 ? (
                <>
                  <p style={{ fontSize: 14, marginBottom: 12 }}>Your tree is empty. Build it from your notes.</p>
                  <button className="btn btn-primary" disabled={reset.isPending} onClick={() => reset.mutate()}>
                    {reset.isPending ? 'Building…' : 'Build tree from notes'}
                  </button>
                  {reset.error && (
                    <p style={{ fontSize: 12, color: 'var(--danger)', marginTop: 8 }}>{reset.error.message}</p>
                  )}
                </>
              ) : (
                <p style={{ fontSize: 14 }}>No nodes match your search or filter.</p>
              )}
            </div>
          ) : viewMode === 'canvas' ? (
            <ReactFlow
              nodes={rfNodes}
              edges={rfEdges}
              nodeTypes={nodeTypes}
              onNodeClick={(_, node) => handleNodeClick(node.data.raw)}
              onInit={inst => { flowRef.current = inst; inst.fitView({ padding: 0.15 }) }}
              minZoom={0.05}
              maxZoom={2}
              nodesDraggable={false}
              nodesConnectable={false}
              elementsSelectable={false}
              panOnDrag
              zoomOnScroll
              zoomOnPinch
              style={{ background: 'var(--bg)' }}
            >
              <Controls showInteractive={false} />
              <MiniMap
                nodeColor={n => (STATUS_CONFIG[n.data?.raw?.status] ?? STATUS_CONFIG.NO_NOTES).color}
                maskColor="rgba(15,17,23,0.7)"
                style={{ background: 'var(--surface)', border: '1px solid var(--border)' }}
              />
              <Background variant={BackgroundVariant.Dots} color="var(--border)" gap={20} size={1} />
            </ReactFlow>
          ) : (
            <TextTreeView
              forest={filteredTree}
              collapsedIds={collapsedIds}
              onNodeClick={handleNodeClick}
              selectedId={selectedNode?.id}
            />
          )}
        </div>

        {/* ── Detail panel ── */}
        {selectedNode && (
          <div className="tree-detail-panel" style={{
            width: 268, flexShrink: 0,
            background: 'var(--surface)', borderLeft: '1px solid var(--border)',
            overflowY: 'auto',
            boxShadow: viewMode === 'canvas' ? '-6px 0 24px rgba(0,0,0,0.18)' : 'none',
          }}>
            <NodeDetailPanel nodeId={selectedNode.id} onClose={() => setSelectedNode(null)} />
          </div>
        )}
      </div>
    </div>
  )
}
