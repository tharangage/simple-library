import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it, vi } from 'vitest'
import { ConfirmDialog } from './ConfirmDialog.jsx'

const renderDialog = (props = {}) => {
  const onConfirm = vi.fn()
  const onCancel = vi.fn()
  render(
    <>
      <button type="button">Opener</button>
      <ConfirmDialog open title="Borrow this book?" confirmLabel="Borrow" onConfirm={onConfirm} onCancel={onCancel} {...props}>
        <p>Dune</p>
      </ConfirmDialog>
    </>,
  )
  return { onConfirm, onCancel }
}

describe('ConfirmDialog', () => {
  it('renders nothing while closed', () => {
    renderDialog({ open: false })
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument()
  })

  it('is a labelled modal dialog and starts with focus on Cancel', () => {
    renderDialog()
    expect(screen.getByRole('dialog', { name: 'Borrow this book?' })).toHaveAttribute('aria-modal', 'true')
    expect(screen.getByRole('button', { name: 'Cancel' })).toHaveFocus()
    expect(screen.getByText('Dune')).toBeInTheDocument()
  })

  it('confirms and cancels with the buttons', async () => {
    const { onConfirm, onCancel } = renderDialog()
    const user = userEvent.setup()

    await user.click(screen.getByRole('button', { name: 'Borrow' }))
    await user.click(screen.getByRole('button', { name: 'Cancel' }))

    expect(onConfirm).toHaveBeenCalledTimes(1)
    expect(onCancel).toHaveBeenCalledTimes(1)
  })

  it('cancels with Escape', async () => {
    const { onCancel } = renderDialog()

    await userEvent.setup().keyboard('{Escape}')

    expect(onCancel).toHaveBeenCalledTimes(1)
  })

  it('keeps Tab inside the dialog', async () => {
    renderDialog()
    const user = userEvent.setup()

    await user.tab()
    expect(screen.getByRole('button', { name: 'Borrow' })).toHaveFocus()
    await user.tab()
    expect(screen.getByRole('button', { name: 'Cancel' })).toHaveFocus()
    await user.tab({ shift: true })
    expect(screen.getByRole('button', { name: 'Borrow' })).toHaveFocus()
  })

  it('disables both buttons while busy', () => {
    renderDialog({ busy: true })
    expect(screen.getByRole('button', { name: 'Cancel' })).toBeDisabled()
    expect(screen.getByRole('button', { name: 'Borrow' })).toBeDisabled()
  })
})
