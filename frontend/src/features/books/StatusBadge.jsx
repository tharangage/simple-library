import { STATUS_LABELS } from './bookStatus.js'

export function StatusBadge({ status }) {
  return <span className={`badge badge--${status.toLowerCase()}`}>{STATUS_LABELS[status]}</span>
}
