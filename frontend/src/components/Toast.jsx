import { useCallback, useMemo, useRef, useState } from 'react'
import { ToastContext } from './useToast.js'

const DISMISS_AFTER_MS = 6000

/**
 * Short messages that appear in the corner and go away by themselves ("Dune is yours").
 * Success messages are announced politely (role="status"), errors immediately (role="alert").
 * Wrap the app once; call `useToast().success(text)` or `.error(text)` (see useToast.js) from any component.
 */
export function ToastProvider({ children }) {
  const [toasts, setToasts] = useState([])
  const nextId = useRef(1)

  const dismiss = useCallback((id) => setToasts((current) => current.filter((t) => t.id !== id)), [])

  const show = useCallback(
    (variant, message) => {
      const id = nextId.current++
      setToasts((current) => [...current, { id, variant, message }])
      setTimeout(() => dismiss(id), DISMISS_AFTER_MS)
    },
    [dismiss],
  )

  const api = useMemo(
    () => ({ success: (message) => show('success', message), error: (message) => show('error', message) }),
    [show],
  )

  return (
    <ToastContext.Provider value={api}>
      {children}
      <div className="toasts">
        {toasts.map((toast) => (
          <div
            key={toast.id}
            role={toast.variant === 'error' ? 'alert' : 'status'}
            className={`toast toast--${toast.variant}`}
          >
            <span>{toast.message}</span>
            <button type="button" className="toast__close" aria-label="Dismiss message" onClick={() => dismiss(toast.id)}>
              ×
            </button>
          </div>
        ))}
      </div>
    </ToastContext.Provider>
  )
}
