import nodemailer from 'nodemailer'

interface ContactPayload {
  name?: string
  email?: string
  subject?: string
  message?: string
  hCaptchaResponse?: string
}

interface HcaptchaVerifyResponse {
  success: boolean
  score?: number
  'error-codes'?: string[]
}

const EMAIL_PATTERN =
  /^(?:[a-zA-Z0-9_'^&/+-])+(?:\.(?:[a-zA-Z0-9_'^&/+-])+)*@(?:[a-zA-Z0-9-]+\.)+[a-zA-Z]{2,}$/u

const clean = (value?: string): string => String(value ?? '').trim()

const validatePayload = (payload: ContactPayload) => {
  const name = clean(payload.name)
  const email = clean(payload.email)
  const subject = clean(payload.subject)
  const message = clean(payload.message)
  const hCaptchaResponse = clean(payload.hCaptchaResponse)

  if (name.length < 2 || name.length > 120) {
    throw createError({ statusCode: 400, statusMessage: 'Invalid name.' })
  }

  if (!EMAIL_PATTERN.test(email)) {
    throw createError({ statusCode: 400, statusMessage: 'Invalid email address.' })
  }

  if (subject.length < 2 || subject.length > 180) {
    throw createError({ statusCode: 400, statusMessage: 'Invalid subject.' })
  }

  if (message.length < 10 || message.length > 4000) {
    throw createError({ statusCode: 400, statusMessage: 'Invalid message length.' })
  }

  if (!hCaptchaResponse) {
    throw createError({ statusCode: 400, statusMessage: 'Missing hCaptcha token.' })
  }

  return { name, email, subject, message, hCaptchaResponse }
}

const verifyHcaptcha = async (token: string, remoteip: string | undefined) => {
  const config = useRuntimeConfig()

  if (!config.hcaptcha.secret) {
    throw createError({
      statusCode: 500,
      statusMessage: 'HCAPTCHA_SECRET is not configured.',
    })
  }

  const payload = new URLSearchParams({
    response: token,
    secret: config.hcaptcha.secret,
  })

  if (remoteip) {
    payload.append('remoteip', remoteip)
  }

  const result = await $fetch<HcaptchaVerifyResponse>(config.hcaptcha.verifyUrl, {
    method: 'POST',
    body: payload,
    headers: {
      'content-type': 'application/x-www-form-urlencoded',
    },
  })

  if (!result.success) {
    throw createError({
      statusCode: 400,
      statusMessage: `hCaptcha validation failed: ${(result['error-codes'] ?? []).join(', ') || 'unknown error'}.`,
    })
  }

  if (typeof result.score === 'number' && result.score < config.hcaptcha.minScore) {
    throw createError({
      statusCode: 400,
      statusMessage: 'hCaptcha score is below the configured minimum threshold.',
    })
  }
}

const sendMail = async (payload: {
  name: string
  email: string
  subject: string
  message: string
}) => {
  const config = useRuntimeConfig()

  const missing = [
    !config.smtp.host && 'SMTP_HOST',
    !config.smtp.user && 'SMTP_USER',
    !config.smtp.pass && 'SMTP_PASS',
    !config.smtp.from && 'SMTP_FROM',
    !config.smtp.to && 'CONTACT_TO',
  ].filter(Boolean)

  if (missing.length > 0) {
    throw createError({
      statusCode: 500,
      statusMessage: `Missing SMTP configuration: ${missing.join(', ')}.`,
    })
  }

  const transporter = nodemailer.createTransport({
    host: config.smtp.host,
    port: config.smtp.port,
    secure: config.smtp.secure,
    auth: {
      user: config.smtp.user,
      pass: config.smtp.pass,
    },
  })

  await transporter.sendMail({
    from: config.smtp.from,
    to: config.smtp.to,
    replyTo: payload.email,
    subject: `[spered-conseil.fr] ${payload.subject}`,
    text: [
      `Name: ${payload.name}`,
      `Email: ${payload.email}`,
      '',
      payload.message,
    ].join('\n'),
  })
}

export default defineEventHandler(async event => {
  const body = await readBody<ContactPayload>(event)
  const remoteIp = getRequestIP(event, { xForwardedFor: true })

  const sanitizedPayload = validatePayload(body)

  await verifyHcaptcha(sanitizedPayload.hCaptchaResponse, remoteIp)
  await sendMail(sanitizedPayload)

  return { ok: true }
})
