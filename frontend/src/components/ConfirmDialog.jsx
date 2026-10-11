import { useEffect, useId, useRef } from 'react'

/**
 * A modal "are you sure?" box. Shown while `open` is true.
 * Keyboard: focus moves into the dialog, Tab stays inside it, Escape cancels, and focus returns to the
 * button that opened it when it closes.
 */
export function ConfirmDialog({ open, title, children, confirmLabel, onConfirm, onCancel, busy = false }) {
  const titleId = useId()
  const dialogRef = useRef(null)
  const cancelRef = useRef(null)

  // Keep the latest onCancel without restarting the effect below on every render.
  const onCancelRef = useRef(onCancel)
  useEffect(() => {
    onCancelRef.current = onCancel
  })

  useEffect(() => {
    if (!open) return undefined
    const opener = document.activeElement
    cancelRef.current?.focus()

    const onKeyDown = (event) => {
      if (event.key === 'Escape') {
        event.stopPropagation()
        onCancelRef.current()
        return
      }
      if (event.key !== 'Tab' || !dialogRef.current) return
      const focusable = dialogRef.current.querySelectorAll('button:not(:disabled)')
      const first = focusable[0]
      const last = focusable[focusable.length - 1]
      if (event.shiftKey && document.activeElement === first) {
        event.preventDefault()
        last.focus()
      } else if (!event.shiftKey && document.activeElement === last) {
        event.preventDefault()
        first.focus()
      }
    }
    document.addEventListener('keydown', onKeyDown)
    return () => {
      document.removeEventListener('keydown', onKeyDown)
      opener?.focus?.()
    }
  }, [open])

  if (!open) return null

  return (
    <div className="dialog-backdrop">
      <div
        ref={dialogRef}
        role="dialog"
        aria-modal="true"
        aria-labelledby={titleId}
        className="dialog"
      >
        <h2 id={titleId}>{title}</h2>
        <div className="dialog__body">{children}</div>
        <div className="dialog__actions">
          <button type="button" ref={cancelRef} onClick={onCancel} disabled={busy}>
            Cancel
          </button>
          <button type="button" className="button--primary" onClick={onConfirm} disabled={busy}>
            {confirmLabel}
          </button>
        </div>
      </div>
    </div>
  )
}
