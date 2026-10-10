import { describe, expect, it } from 'vitest'
import { validateRegistration } from './validateRegistration.js'

const valid = {
  firstName: 'Ann',
  lastName: 'Lee',
  email: 'ann@example.com',
  mobile: '+14155550100',
  password: 'secret-pass',
  confirmPassword: 'secret-pass',
}

describe('validateRegistration', () => {
  it('accepts a complete, valid form', () => {
    expect(validateRegistration(valid)).toEqual({})
  })

  it('requires every field', () => {
    const empty = { firstName: '', lastName: ' ', email: '', mobile: '', password: '', confirmPassword: '' }
    expect(Object.keys(validateRegistration(empty))).toEqual([
      'firstName',
      'lastName',
      'email',
      'mobile',
      'password',
      'confirmPassword',
    ])
  })

  it.each(['ann', 'ann@example', 'ann@@example.com', 'a nn@example.com'])('rejects the email %s', (email) => {
    expect(validateRegistration({ ...valid, email }).email).toBe('Email must be valid')
  })

  it.each(['0415550100', '+0415550100', '+1415', '+1415555010012345', '+1 415 555 0100', '+1415abc0100'])(
    'rejects the mobile number %s',
    (mobile) => {
      expect(validateRegistration({ ...valid, mobile }).mobile).toMatch(/international format/)
    },
  )

  it.each(['+14155550100', '+61412345678', '+12345678'])('accepts the mobile number %s', (mobile) => {
    expect(validateRegistration({ ...valid, mobile }).mobile).toBeUndefined()
  })

  it('rejects a password shorter than 8 characters', () => {
    expect(validateRegistration({ ...valid, password: 'short', confirmPassword: 'short' }).password).toBe(
      'Password must be 8 to 128 characters',
    )
  })

  it('rejects a confirmation that differs', () => {
    expect(validateRegistration({ ...valid, confirmPassword: 'other-pass' }).confirmPassword).toBe(
      'Passwords do not match',
    )
  })

  it('rejects names longer than 64 characters', () => {
    const long = 'x'.repeat(65)
    const errors = validateRegistration({ ...valid, firstName: long, lastName: long })
    expect(errors.firstName).toMatch(/at most 64/)
    expect(errors.lastName).toMatch(/at most 64/)
  })

  it('ignores surrounding spaces in names, email and mobile', () => {
    expect(validateRegistration({ ...valid, firstName: ' Ann ', email: ' ann@example.com ', mobile: ' +14155550100 ' })).toEqual({})
  })
})
