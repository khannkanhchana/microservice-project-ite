import { useEffect, useMemo, useRef, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { accountApi } from '../../api/account'
import { useAuth } from '../../auth/AuthContext'
import { applyLanguage } from '../../i18n'
import { Alert, Avatar, Badge, Button, Card, Field, Input, PageHeader, Select, useToast } from '../../components/ui'
import { CameraIcon } from '../../components/icons'
import { errorMessage, formatDate, formatDateTime } from '../../lib/format'

const MAX_PICTURE_BYTES = 2 * 1024 * 1024
const PICTURE_TYPES = ['image/png', 'image/jpeg', 'image/webp', 'image/gif']

const toForm = (user) => ({
  givenName: user.givenName || '',
  familyName: user.familyName || '',
  nickname: user.nickname || '',
  username: user.username || '',
  email: user.email || '',
  phoneNumber: user.phoneNumber || '',
  birthdate: user.birthdate || '',
  gender: user.gender || '',
  locale: user.locale === 'km' ? 'km' : 'en',
})

function PictureCard({ user, onChange }) {
  const { t } = useTranslation()
  const toast = useToast()
  const input = useRef(null)
  const [busy, setBusy] = useState(null)
  const [error, setError] = useState(null)

  const upload = async (event) => {
    const file = event.target.files?.[0]
    event.target.value = ''
    if (!file) return
    setError(null)
    if (!PICTURE_TYPES.includes(file.type)) return setError(t('profile.pictureType'))
    if (file.size > MAX_PICTURE_BYTES) return setError(t('profile.pictureTooLarge'))
    setBusy('upload')
    try {
      const { pictureUrl } = await accountApi.uploadPicture(file)
      onChange({ ...user, pictureUrl })
      toast(t('profile.pictureUpdated'))
    } catch (err) {
      setError(errorMessage(err, t))
    } finally {
      setBusy(null)
    }
  }

  const remove = async () => {
    setBusy('remove')
    setError(null)
    try {
      await accountApi.removePicture()
      onChange({ ...user, pictureUrl: null })
      toast(t('profile.pictureRemoved'))
    } catch (err) {
      setError(errorMessage(err, t))
    } finally {
      setBusy(null)
    }
  }

  return (
    <Card title={t('profile.pictureTitle')} description={t('profile.pictureDescription')}>
      <div className="flex flex-col items-start gap-5 sm:flex-row sm:items-center">
        <div className="relative">
          <Avatar src={user.pictureUrl} name={user.name || user.username} size="xl" />
          <button
            type="button"
            onClick={() => input.current?.click()}
            className="absolute -bottom-1 -right-1 rounded-full border-2 border-surface bg-primary p-1.5 text-on-primary hover:bg-primary-hover"
            aria-label={t('profile.change')}
          >
            <CameraIcon className="size-4" />
          </button>
        </div>
        <div className="flex flex-col gap-3">
          <div className="flex flex-wrap gap-2">
            <Button variant="secondary" onClick={() => input.current?.click()} loading={busy === 'upload'}>
              {user.pictureUrl ? t('profile.change') : t('profile.upload')}
            </Button>
            {user.pictureUrl && (
              <Button variant="danger-outline" onClick={remove} loading={busy === 'remove'}>
                {t('profile.remove')}
              </Button>
            )}
          </div>
          {error && <p className="text-sm text-danger">{error}</p>}
        </div>
        <input ref={input} type="file" accept={PICTURE_TYPES.join(',')} className="hidden" onChange={upload} />
      </div>
    </Card>
  )
}

export default function Profile() {
  const { t, i18n } = useTranslation()
  const toast = useToast()
  const { user, setUser } = useAuth()
  const [form, setForm] = useState(() => toForm(user))
  const [errors, setErrors] = useState({})
  const [error, setError] = useState(null)
  const [busy, setBusy] = useState(false)

  useEffect(() => setForm(toForm(user)), [user])

  const original = useMemo(() => toForm(user), [user])
  const changes = useMemo(
    () => Object.fromEntries(Object.entries(form).filter(([key, value]) => value.trim() !== original[key])),
    [form, original],
  )
  const dirty = Object.keys(changes).length > 0
  const set = (key) => (event) => setForm((current) => ({ ...current, [key]: event.target.value }))

  const submit = async (event) => {
    event.preventDefault()
    if (!dirty) return toast(t('profile.noChanges'))
    const missing = Object.fromEntries(
      ['givenName', 'familyName', 'username', 'email']
        .filter((key) => !form[key].trim())
        .map((key) => [key, t('profile.required')]),
    )
    setErrors(missing)
    if (Object.keys(missing).length) return
    setBusy(true)
    setError(null)
    try {
      const payload = Object.fromEntries(Object.entries(changes).map(([key, value]) => [key, value.trim()]))
      // Blank optional fields are left unchanged by the API, except the phone number which a blank value clears.
      for (const key of ['nickname', 'birthdate', 'gender']) if (payload[key] === '') delete payload[key]
      const updated = await accountApi.updateMe(payload)
      setUser(updated)
      if (payload.locale && payload.locale !== i18n.language) applyLanguage(payload.locale)
      toast(t('profile.saved'))
    } catch (err) {
      setErrors(err.fieldErrors || {})
      setError(errorMessage(err, t))
    } finally {
      setBusy(false)
    }
  }

  return (
    <>
      <PageHeader title={t('profile.title')} description={t('profile.description')} />
      <div className="flex flex-col gap-6">
        <PictureCard user={user} onChange={setUser} />

        <Card title={t('profile.personalTitle')} description={t('profile.personalDescription')}>
          <form onSubmit={submit} noValidate>
            {error && (
              <div className="mb-5">
                <Alert tone="danger" onDismiss={() => setError(null)}>
                  {error}
                </Alert>
              </div>
            )}
            <div className="grid grid-cols-1 gap-5 md:grid-cols-2">
              <Field label={t('profile.givenName')} error={errors.givenName}>
                {(p) => <Input {...p} autoComplete="given-name" value={form.givenName} onChange={set('givenName')} />}
              </Field>
              <Field label={t('profile.familyName')} error={errors.familyName}>
                {(p) => <Input {...p} autoComplete="family-name" value={form.familyName} onChange={set('familyName')} />}
              </Field>
              <Field label={t('profile.username')} hint={t('profile.usernameHint')} error={errors.username}>
                {(p) => <Input {...p} autoComplete="username" value={form.username} onChange={set('username')} />}
              </Field>
              <Field label={t('profile.nickname')} error={errors.nickname}>
                {(p) => <Input {...p} autoComplete="nickname" value={form.nickname} onChange={set('nickname')} />}
              </Field>
              <Field
                label={
                  <span className="flex items-center gap-2">
                    {t('profile.email')}
                    <Badge tone={user.emailVerified ? 'success' : 'warning'}>
                      {user.emailVerified ? t('common.verified') : t('common.notVerified')}
                    </Badge>
                  </span>
                }
                hint={changes.email ? t('profile.emailChangeHint') : undefined}
                error={errors.email}
              >
                {(p) => <Input {...p} type="email" autoComplete="email" value={form.email} onChange={set('email')} />}
              </Field>
              <Field label={t('profile.phone')} hint={t('profile.phoneHint')} error={errors.phoneNumber}>
                {(p) => (
                  <Input {...p} type="tel" autoComplete="tel" placeholder="+855 12 345 678" value={form.phoneNumber} onChange={set('phoneNumber')} />
                )}
              </Field>
              <Field label={t('profile.birthdate')} error={errors.birthdate}>
                {(p) => <Input {...p} type="date" autoComplete="bday" value={form.birthdate} onChange={set('birthdate')} />}
              </Field>
              <Field label={t('profile.gender')} error={errors.gender}>
                {(p) => (
                  <Select {...p} value={form.gender} onChange={set('gender')}>
                    <option value="">{t('profile.genderUnset')}</option>
                    <option value="female">{t('profile.genderFemale')}</option>
                    <option value="male">{t('profile.genderMale')}</option>
                    <option value="other">{t('profile.genderOther')}</option>
                  </Select>
                )}
              </Field>
              <Field label={t('profile.locale')} error={errors.locale}>
                {(p) => (
                  <Select {...p} value={form.locale} onChange={set('locale')}>
                    <option value="en">English</option>
                    <option value="km">ភាសាខ្មែរ</option>
                  </Select>
                )}
              </Field>
            </div>
            <div className="mt-6 flex justify-end gap-2 border-t border-line pt-5">
              <Button variant="secondary" disabled={!dirty || busy} onClick={() => setForm(original)}>
                {t('common.cancel')}
              </Button>
              <Button type="submit" loading={busy} disabled={!dirty}>
                {t('common.save')}
              </Button>
            </div>
          </form>
        </Card>

        <Card title={t('profile.detailsTitle')}>
          <dl className="grid grid-cols-1 gap-x-6 gap-y-4 text-sm sm:grid-cols-2">
            <div>
              <dt className="text-muted">{t('profile.memberSince')}</dt>
              <dd className="mt-0.5 font-medium text-ink">{formatDate(user.createdAt, i18n.language)}</dd>
            </div>
            <div>
              <dt className="text-muted">{t('profile.lastLogin')}</dt>
              <dd className="mt-0.5 font-medium text-ink">
                {user.lastLoginAt ? formatDateTime(user.lastLoginAt, i18n.language) : t('common.never')}
              </dd>
            </div>
            <div>
              <dt className="text-muted">{t('profile.roles')}</dt>
              <dd className="mt-1 flex flex-wrap gap-1.5">
                {user.roles?.length ? (
                  user.roles.map((role) => (
                    <Badge key={role.uuid} tone="primary">
                      {role.name}
                    </Badge>
                  ))
                ) : (
                  <span className="text-subtle">{t('profile.noRoles')}</span>
                )}
              </dd>
            </div>
            <div>
              <dt className="text-muted">{t('profile.accountId')}</dt>
              <dd className="mt-0.5 break-all font-mono text-xs text-ink">{user.uuid}</dd>
            </div>
          </dl>
        </Card>
      </div>
    </>
  )
}
