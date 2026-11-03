import { useState } from 'react';
import { useForm } from 'react-hook-form';
import { Link, useNavigate } from 'react-router';
import { ApiError } from '@/data/apiError';
import { errorMessages, formatWait, messageFor } from '@/shared/errors';
import { applyServerErrors, errorCode } from '@/shared/forms/serverErrors';
import { useCountdown } from '@/shared/hooks/useCountdown';
import { vi } from '@/shared/i18n/vi';
import {
  ArrowRightIcon,
  Button,
  ErrorBanner,
  LockIcon,
  MailIcon,
  PinIcon,
  StoreIcon,
  TextField,
  UserIcon,
  UserPlusIcon,
} from '@/shared/ui';
import { useAccountApi } from './accountApi';
import { AuthCardHead, AuthLayout, MainArt, RouteArt } from './AuthLayout';
import { emailRules, passwordRules, requiredRule } from './rules';

interface RegisterForm {
  storeName: string;
  fullName: string;
  email: string;
  password: string;
}

const FIELDS = ['storeName', 'fullName', 'email', 'password'] as const;
const LABELS = {
  storeName: vi.form.storeName,
  fullName: vi.form.fullName,
  email: vi.auth.email,
  password: vi.auth.password,
};
const STEP_ICONS = [StoreIcon, PinIcon, UserPlusIcon];

/** Prototype "Đăng ký cửa hàng" (SCREENS §1): creates the store and its OWNER, then the code arrives by email. */
export function RegisterPage() {
  const accountApi = useAccountApi();
  const navigate = useNavigate();
  const lock = useCountdown();
  const [failure, setFailure] = useState<unknown>(null);
  const [emailExisted, setEmailExisted] = useState(false);
  const {
    register,
    handleSubmit,
    setError,
    formState: { errors, isSubmitting },
  } = useForm<RegisterForm>({ defaultValues: { storeName: '', fullName: '', email: '', password: '' } });

  const onSubmit = handleSubmit(async (form) => {
    setFailure(null);
    setEmailExisted(false);
    const email = form.email.trim();
    try {
      await accountApi.register({
        storeName: form.storeName.trim(),
        fullName: form.fullName.trim() || undefined,
        email,
        password: form.password,
      });
      navigate(`/verify-email?email=${encodeURIComponent(email)}`, { state: { codeSent: true } });
    } catch (error) {
      if (errorCode(error) === 'EMAIL_EXISTED') {
        setEmailExisted(true);
        return;
      }
      if (applyServerErrors(error, setError, FIELDS, LABELS)) return;
      if (error instanceof ApiError && error.code === 'RATE_LIMITED' && error.retryAfter !== null) {
        lock.start(error.retryAfter);
      }
      setFailure(error);
    }
  });

  const emailError = emailExisted ? (
    <>
      {errorMessages.EMAIL_EXISTED} <Link to="/login">{vi.form.toLogin}</Link> {vi.form.emailExistedLogin}
    </>
  ) : (
    errors.email?.message
  );

  return (
    <AuthLayout
      panel="wide"
      logoToLogin
      art={<RouteArt variant="register" />}
      mainArt={<MainArt variant="register" />}
      pitch={
        <div className="pw-auth-pitch pw-auth-pitch-steps">
          <div className="pw-auth-pitch-text">
            <p className="pw-auth-headline">
              {vi.register.brandHeadline[0]}
              <span>{vi.register.brandHeadline[1]}</span>
              {vi.register.brandHeadline[2]}
            </p>
            <p className="pw-auth-lead">{vi.register.brandLead}</p>
          </div>
          <ol className="pw-auth-steps-inline">
            {vi.register.steps.map((step, index) => {
              const StepIcon = STEP_ICONS[index] ?? StoreIcon;
              return (
                <li key={step}>
                  <StepIcon />
                  <span>{step}</span>
                </li>
              );
            })}
          </ol>
        </div>
      }
    >
      <form className="pw-auth-card pw-auth-card-md" aria-labelledby="reg-title" onSubmit={onSubmit} noValidate>
        <AuthCardHead id="reg-title" title={vi.register.title} lead={vi.register.lead} />
        {failure ? <ErrorBanner title={messageFor(failure)} /> : null}

        <TextField
          label={vi.form.storeName}
          required
          size="lg"
          maxLength={150}
          placeholder={vi.register.storePlaceholder}
          startIcon={<StoreIcon />}
          error={errors.storeName?.message}
          {...register('storeName', requiredRule('STORE_NAME_REQUIRED', vi.form.storeName))}
        />
        <TextField
          label={vi.form.yourName}
          size="lg"
          maxLength={100}
          autoComplete="name"
          startIcon={<UserIcon />}
          error={errors.fullName?.message}
          {...register('fullName')}
        />
        <TextField
          label={vi.auth.email}
          type="email"
          required
          size="lg"
          autoComplete="email"
          inputMode="email"
          placeholder="ten@example.com"
          startIcon={<MailIcon />}
          error={emailError}
          {...register('email', emailRules(vi.auth.email))}
        />
        <TextField
          label={vi.auth.password}
          type="password"
          required
          size="lg"
          maxLength={72}
          autoComplete="new-password"
          hint={vi.form.passwordHint}
          startIcon={<LockIcon />}
          error={errors.password?.message}
          {...register('password', passwordRules(vi.auth.password))}
        />

        <Button type="submit" variant="primary" size="lg" block busy={isSubmitting} disabled={lock.remaining > 0}>
          {lock.remaining > 0 ? (
            vi.auth.retryIn(formatWait(lock.remaining))
          ) : (
            <>
              {vi.register.submit}
              <ArrowRightIcon />
            </>
          )}
        </Button>
        <p className="pw-auth-center">
          {vi.form.haveAccount} <Link to="/login">{vi.form.toLogin}</Link>
        </p>
      </form>
    </AuthLayout>
  );
}
