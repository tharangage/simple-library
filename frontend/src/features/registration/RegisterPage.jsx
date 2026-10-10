import { useMutation } from '@tanstack/react-query'
import { useRef, useState } from 'react'
import { useAuth } from 'react-oidc-context'
import { Navigate } from 'react-router'
import { registerUser } from '../../api/registrations.js'
import { TextField } from '../../components/TextField.jsx'
import { validateRegistration } from './validateRegistration.js'

const EMPTY = { firstName: '', lastName: '', email: '', mobile: '', password: '', confirmPassword: '' }
const FIELD_ORDER = ['firstName', 'lastName', 'email', 'mobile', 'password', 'confirmPassword']

export function RegisterPage() {
  const auth = useAuth()
  const formRef = useRef(null)
  const [values, setValues] = useState(EMPTY)
  const [touched, setTouched] = useState({}) // fields the user has left, so we don't nag while typing
  const [submitted, setSubmitted] = useState(false)
  const [serverErrors, setServerErrors] = useState({}) // field errors from a 400 answer

  const registration = useMutation({
    mutationFn: registerUser,
    onSuccess: () => setValues(EMPTY), // do not keep the password in memory longer than needed
    onError: (error) => setServerErrors(error.fieldErrors ?? {}),
  })

  if (auth.isAuthenticated) return <Navigate to="/books" replace />

  if (registration.isSuccess) {
    return (
      <section className="auth-card" aria-labelledby="registered-title">
        <h1 id="registered-title">Account created</h1>
        <p role="status">Your account is ready. Log in to start borrowing books.</p>
        <button type="button" className="button--primary" onClick={() => auth.signinRedirect()}>
          Log in
        </button>
      </section>
    )
  }

  const clientErrors = validateRegistration(values)
  // Client rules win; server errors fill in what the browser could not know.
  const errors = { ...serverErrors, ...clientErrors }
  const visible = (name) => (submitted || touched[name] || serverErrors[name] ? errors[name] : undefined)

  const change = (name) => (event) => {
    setValues((current) => ({ ...current, [name]: event.target.value }))
    setServerErrors((current) => ({ ...current, [name]: undefined }))
  }
  const blur = (name) => () => setTouched((current) => ({ ...current, [name]: true }))

  const submit = (event) => {
    event.preventDefault()
    setSubmitted(true)
    const firstInvalid = FIELD_ORDER.find((name) => clientErrors[name])
    if (firstInvalid) {
      formRef.current.elements[firstInvalid].focus()
      return
    }
    setServerErrors({})
    registration.mutate(values)
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

  const error = registration.error
  const emailTaken = error?.status === 409
  // Field-level 400 errors are shown next to their fields; everything else gets one message here.
  const failureMessage = error && !emailTaken && !hasFieldErrors(error) ? describeFailure(error) : null

  return (
    <section className="auth-card" aria-labelledby="register-title">
      <h1 id="register-title">Create your account</h1>
      <p className="muted">Register as a library borrower.</p>

      {emailTaken && (
        <p role="alert" className="message message--error">
          This email is already registered.{' '}
          <button type="button" className="link-button" onClick={() => auth.signinRedirect()}>
            Log in
          </button>
        </p>
      )}
      {failureMessage && (
        <p role="alert" className="message message--error">
          {failureMessage}
        </p>
      )}

      <form ref={formRef} onSubmit={submit} noValidate className="form">
        <div className="form__row">
          {field('firstName', 'First name', { autoComplete: 'given-name' })}
          {field('lastName', 'Last name', { autoComplete: 'family-name' })}
        </div>
        {field('email', 'Email', { type: 'email', autoComplete: 'email' })}
        {field('mobile', 'Mobile number', {
          type: 'tel',
          autoComplete: 'tel',
          placeholder: '+14155550100',
          hint: 'Start with + and your country code, e.g. +14155550100',
        })}
        {field('password', 'Password', {
          type: 'password',
          autoComplete: 'new-password',
          hint: 'At least 8 characters',
        })}
        {field('confirmPassword', 'Confirm password', { type: 'password', autoComplete: 'new-password' })}
        <button type="submit" className="button--primary" disabled={registration.isPending}>
          {registration.isPending ? 'Creating account…' : 'Create account'}
        </button>
      </form>

      <p className="muted">
        Already have an account?{' '}
        <button type="button" className="link-button" onClick={() => auth.signinRedirect()}>
          Log in
        </button>
      </p>
    </section>
  )
}

function hasFieldErrors(error) {
  return Object.keys(error.fieldErrors ?? {}).length > 0
}

function describeFailure(error) {
  // status 0: no answer at all; the client already words that message for us.
  if (error.status === 0) return error.message
  if (error.status >= 500) return 'Something went wrong on our side. Please try again later.'
  return error.message
}
