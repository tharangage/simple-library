import { act, render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { ToastProvider } from './Toast.jsx'
import { useToast } from './useToast.js'

function Buttons() {
  const toast = useToast()
  return (
    <>
      <button type="button" onClick={() => toast.success('Saved')}>
        ok
      </button>
      <button type="button" onClick={() => toast.error('Failed')}>
        bad
      </button>
    </>
  )
}

const renderToasts = () =>
  render(
    <ToastProvider>
      <Buttons />
    </ToastProvider>,
  )

afterEach(() => vi.useRealTimers())

describe('Toast', () => {
  it('announces a success politely and an error as an alert', async () => {
    const user = userEvent.setup()
    renderToasts()

    await user.click(screen.getByRole('button', { name: 'ok' }))
    await user.click(screen.getByRole('button', { name: 'bad' }))

    expect(screen.getByText('Saved').closest('[aria-live]')).toHaveAttribute('aria-live', 'polite')
    expect(screen.getByText('Failed').closest('[aria-live]')).toHaveAttribute('aria-live', 'assertive')
  })

  it('keeps both live regions in the page before any message, so the first message is announced', () => {
    const { container } = renderToasts()

    expect(container.ownerDocument.querySelector('[aria-live="polite"]')).toBeEmptyDOMElement()
    expect(container.ownerDocument.querySelector('[aria-live="assertive"]')).toBeEmptyDOMElement()
  })

  it('can be dismissed by hand', async () => {
    const user = userEvent.setup()
    renderToasts()
    await user.click(screen.getByRole('button', { name: 'ok' }))

    await user.click(screen.getByRole('button', { name: 'Dismiss message' }))

    expect(screen.queryByText('Saved')).not.toBeInTheDocument()
  })

  it('goes away by itself after a few seconds', () => {
    vi.useFakeTimers()
    renderToasts()
    act(() => screen.getByRole('button', { name: 'ok' }).click())
    expect(screen.getByText('Saved')).toBeInTheDocument()

    act(() => vi.advanceTimersByTime(6100))

    expect(screen.queryByText('Saved')).not.toBeInTheDocument()
  })

  it('refuses to be used without a provider', () => {
    const quiet = vi.spyOn(console, 'error').mockImplementation(() => {})
    expect(() => render(<Buttons />)).toThrow(/ToastProvider/)
    quiet.mockRestore()
  })
})
