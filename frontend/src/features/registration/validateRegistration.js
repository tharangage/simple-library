/**
 * Client-side checks for the registration form. They mirror the backend rules (RegistrationRequest),
 * so the user sees problems before sending; the backend still validates everything again.
 * Returns {field: message} for invalid fields only (empty object = valid).
 */
const EMAIL = /^[^@\s]+@[^@\s]+\.[^@\s]+$/
const MOBILE_E164 = /^\+[1-9]\d{7,14}$/

export function validateRegistration({ firstName, lastName, email, mobile, password, confirmPassword }) {
  const errors = {}

  const first = firstName.trim()
  if (!first) errors.firstName = 'First name is required'
  else if (first.length > 64) errors.firstName = 'First name must be at most 64 characters'

  const last = lastName.trim()
  if (!last) errors.lastName = 'Last name is required'
  else if (last.length > 64) errors.lastName = 'Last name must be at most 64 characters'

  const mail = email.trim()
  if (!mail) errors.email = 'Email is required'
  else if (mail.length > 254 || !EMAIL.test(mail)) errors.email = 'Email must be valid'

  const phone = mobile.trim()
  if (!phone) errors.mobile = 'Mobile number is required'
  else if (!MOBILE_E164.test(phone)) {
    errors.mobile = 'Mobile number must be in international format, e.g. +14155550100'
  }

  if (!password) errors.password = 'Password is required'
  else if (password.length < 8 || password.length > 128) errors.password = 'Password must be 8 to 128 characters'

  if (!confirmPassword) errors.confirmPassword = 'Please repeat your password'
  else if (confirmPassword !== password) errors.confirmPassword = 'Passwords do not match'

  return errors
}
