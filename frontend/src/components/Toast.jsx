import { useCallback, useEffect, useMemo, useRef, useState } from 'react'
import { ToastContext } from './useToast.js'

const DISMISS_AFTER_MS = 6000

/**
 * Short messages that appear in the corner and go away by themselves ("Dune is yours").
 *
 * Screen readers only announce a live region that already exists when its content changes, so the two
 * regions below are always in the page (even when empty): successes go into the polite one, errors into
 * the assertive one. Wrap the app once; call `useToast().success(text)` or `.error(text)` (see useToast.js).
 */
export function ToastProvider({ children }) {
  const [toasts, setToasts] = useState([])
  const nextId = useRef(1)
  const timers = useRef(new Set())

  // Forget pending dismiss timers when the provider goes away.
  useEffect(() => {
    const pending = timers.current
    return () => pending.forEach(clearTimeout)
  }, [])

  const dismiss = useCallback((id) => setToasts((current) => current.filter((t) => t.id !== id)), [])

  const show = useCallback(
    (variant, message) => {
      const id = nextId.current++
      setToasts((current) => [...current, { id, variant, message }])
      const timer = setTimeout(() => {
        timers.current.delete(timer)
        dismiss(id)
      }, DISMISS_AFTER_MS)
      timers.current.add(timer)
    },
    [dismiss],
  )

  const api = useMemo(
    () => ({ success: (message) => show('success', message), error: (message) => show('error', message) }),
    [show],
  )

  const renderToast = (toast) => (
    <div key={toast.id} className={`toast toast--${toast.variant}`}>
      <span>{toast.message}</span>
      <button type="button" className="toast__close" aria-label="Dismiss message" onClick={() => dismiss(toast.id)}>
        ×
      </button>
    </div>
  )

  return (
    <ToastContext.Provider value={api}>
      {children}
      <div className="toasts">
        <div aria-live="polite">{toasts.filter((t) => t.variant === 'success').map(renderToast)}</div>
        <div aria-live="assertive">{toasts.filter((t) => t.variant === 'error').map(renderToast)}</div>
      </div>
    </ToastContext.Provider>
  )
}
