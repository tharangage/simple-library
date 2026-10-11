import { useMutation, useQueryClient } from '@tanstack/react-query'
import { useEffect, useRef, useState } from 'react'
import { useAuth } from 'react-oidc-context'
import { Link } from 'react-router'
import { createBook } from '../../api/books.js'
import { TextField } from '../../components/TextField.jsx'
import { validateBook } from './validateBook.js'

const EMPTY = { isbn: '', title: '', author: '' }
const FIELD_ORDER = ['isbn', 'title', 'author']

/**
 * Admin form to register one copy of a book (US-03.4). Reach it through <RequireAdmin>; the API refuses
 * everybody else with 403 anyway. After a book is added the same values stay in the form, so "Add another
 * copy" is one click.
 */
export function AddBookPage() {
  const auth = useAuth()
  const token = auth.user?.access_token
  const queryClient = useQueryClient()
  const formRef = useRef(null)
  const anotherCopyRef = useRef(null)
  const sending = useRef(false) // set at once, unlike isPending which changes only after the next render
  const copiesInARow = useRef({ isbn: null, copies: 0 })
  const [values, setValues] = useState(EMPTY)
  const [touched, setTouched] = useState({})
  const [submitted, setSubmitted] = useState(false)
  const [serverErrors, setServerErrors] = useState({}) // field errors from a 400 answer
  const [added, setAdded] = useState(null) // { title, copies } of the last success, for the status line

  const create = useMutation({
    mutationFn: (book) => createBook({ token, ...book }),
    onSuccess: (_created, book) => {
      const previous = copiesInARow.current
      const copies = previous.isbn === book.isbn ? previous.copies + 1 : 1
      copiesInARow.current = { isbn: book.isbn, copies }
      setAdded({ title: book.title, copies })
      queryClient.invalidateQueries({ queryKey: ['books'] })
    },
    onError: (error) => {
      const fieldErrors = error.fieldErrors ?? {}
      setServerErrors(fieldErrors)
      // Take a keyboard or screen-reader user to the first field the server complained about.
      const firstInvalid = FIELD_ORDER.find((name) => fieldErrors[name])
      if (firstInvalid) formRef.current?.elements[firstInvalid]?.focus()
    },
    onSettled: () => {
      sending.current = false
    },
  })

  // After a success the next likely step is another copy, so focus its button (it appears with the status line).
  useEffect(() => {
    if (added) anotherCopyRef.current?.focus()
  }, [added])

  const clientErrors = validateBook(values)
  const errors = { ...serverErrors, ...clientErrors }
  const visible = (name) => (submitted || touched[name] || serverErrors[name] ? errors[name] : undefined)

  const change = (name) => (event) => {
    setValues((current) => ({ ...current, [name]: event.target.value }))
    setServerErrors((current) => ({ ...current, [name]: undefined }))
    setAdded(null)
  }
  const blur = (name) => () => setTouched((current) => ({ ...current, [name]: true }))

  const send = (book) => {
    if (sending.current) return // a second click or Enter in the same moment would create a second copy
    sending.current = true
    setServerErrors({})
    setAdded(null) // a failed attempt must not sit next to the old "was added" line
    create.reset()
    create.mutate(book)
  }

  const submit = (event) => {
    event.preventDefault()
    setSubmitted(true)
    const firstInvalid = FIELD_ORDER.find((name) => clientErrors[name])
    if (firstInvalid) {
      formRef.current.elements[firstInvalid].focus()
      return
    }
    send(trimmed(values))
  }

  const addAnotherCopy = () => send(trimmed(values))

  const startOver = () => {
    setValues(EMPTY)
    setTouched({})
    setSubmitted(false)
    setAdded(null)
    copiesInARow.current = { isbn: null, copies: 0 }
    create.reset()
    formRef.current.elements.isbn.focus()
  }

  const field = (name, label, props = {}) => (
    <TextField
      label={label}
      name={name}
      value={values[name]}
      onChange={change(name)}
      onBlur={blur(name)}
      error={visible(name)}
      required
      {...props}
    />
  )

  const error = create.error
  const failureMessage = error && Object.keys(error.fieldErrors ?? {}).length === 0 ? describeFailure(error) : null

  return (
    <section className="auth-form" aria-labelledby="add-book-title">
      <h1 id="add-book-title">Add a book</h1>
      <p className="auth-subtitle">Register one copy. Use the same ISBN, title and author for more copies.</p>

      {/* Always in the page, so screen readers announce the text when it appears. Only text goes inside. */}
      <div role="status" className="status-line">
        {added && (
          <p className="message message--success">
            “{added.title}” was added{added.copies > 1 ? ` (${added.copies} copies in a row)` : ''}.
          </p>
        )}
      </div>
      {added && (
        <p className="form__actions">
          <button type="button" ref={anotherCopyRef} onClick={addAnotherCopy}>
            Add another copy
          </button>
          <button type="button" onClick={startOver}>
            Add a different book
          </button>
          <Link to="/books">Back to books</Link>
        </p>
      )}
      {failureMessage && (
        <p role="alert" className="message message--error">
          {failureMessage}
        </p>
      )}

      <form ref={formRef} onSubmit={submit} noValidate className="form">
        {field('isbn', 'ISBN', { placeholder: '978-3-16-148410-0', hint: 'ISBN-10 or ISBN-13, hyphens allowed' })}
        {field('title', 'Title', { placeholder: 'The title of the book' })}
        {field('author', 'Author', { placeholder: 'Full name' })}
        {/* aria-disabled instead of disabled: the button keeps keyboard focus while the book is being sent. */}
        <button type="submit" className="button--primary auth-button" aria-disabled={create.isPending}>
          {create.isPending ? 'Adding…' : 'Add book'}
        </button>
      </form>
    </section>
  )
}

function trimmed({ isbn, title, author }) {
  return { isbn: isbn.trim(), title: title.trim(), author: author.trim() }
}

function describeFailure(error) {
  if (error.status === 0) return error.message // no answer at all; the client already words it
  if (error.status === 401) return 'Your session has expired. Please log in again.'
  if (error.status === 403) return 'Only library administrators can add books.'
  // 409: this ISBN already exists with another title or author. The API explains it.
  if (error.status === 409) return error.message
  if (error.status >= 500) return 'Something went wrong on our side. Please try again later.'
  return error.message
}
