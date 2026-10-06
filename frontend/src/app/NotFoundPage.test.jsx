import { render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router'
import { describe, expect, it } from 'vitest'
import { NotFoundPage } from './NotFoundPage.jsx'

describe('NotFoundPage', () => {
  it('links back to the start page', () => {
    render(
      <MemoryRouter>
        <NotFoundPage />
      </MemoryRouter>,
    )
    expect(screen.getByRole('link', { name: 'Back to the start page' })).toHaveAttribute('href', '/')
  })
})
