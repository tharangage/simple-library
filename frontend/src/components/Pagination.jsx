/** Previous / Next controls. `page` is 1-based here because this is what people read. */
export function Pagination({ page, totalPages, onPageChange, disabled }) {
  if (totalPages <= 1) return null
  return (
    <nav className="pagination" aria-label="Pagination">
      <button type="button" onClick={() => onPageChange(page - 1)} disabled={disabled || page <= 1}>
        ← Previous
      </button>
      <span aria-current="page">
        Page {page} of {totalPages}
      </span>
      <button type="button" onClick={() => onPageChange(page + 1)} disabled={disabled || page >= totalPages}>
        Next →
      </button>
    </nav>
  )
}
