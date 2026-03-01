<script setup lang="ts">
import { ref, onMounted, nextTick } from 'vue'
import {
  login, register, accessToken, authError,
  needsVerification, verificationEmail, resendVerificationEmail,
  userId, refreshProfile
} from '../composables/useAuth'
import { useWalletState } from '../composables/useWalletState'
import {
  Mail, Lock, AlertTriangle, Loader, CheckCircle,
  Bot, ArrowLeftRight, ListOrdered, ShieldCheck, TrendingUp,
  ChevronRight, Pencil
} from 'lucide-vue-next'

const props = withDefaults(defineProps<{ mode?: 'login' | 'register' }>(), { mode: 'login' })

const { refreshWalletState } = useWalletState()

const isLogin = ref(props.mode === 'login')
const loginStep = ref<'email' | 'password'>('email')

const loginUsername = ref('')
const loginPassword = ref('')
const passwordInputRef = ref<HTMLInputElement>()
const regEmail = ref('')
const regPassword = ref('')
const subscribeToNewsletter = ref(true)

const submitting = ref(false)
const regSuccess = ref(false)
const regSuccessEmail = ref('')
const localError = ref<string | null>(null)
const resendingVerification = ref(false)
const resendSuccess = ref(false)

function navigateTo(path: string) {
  history.pushState({}, '', path)
  window.dispatchEvent(new PopStateEvent('popstate'))
}

function switchMode(newMode: 'login' | 'register') {
  isLogin.value = newMode === 'login'
  loginStep.value = 'email'
  localError.value = null
  authError.value = null
  needsVerification.value = false
  navigateTo(newMode === 'login' ? '/login' : '/register')
}

async function onLoginEmailContinue() {
  loginStep.value = 'password'
  await nextTick()
  passwordInputRef.value?.focus()
}

function onLoginBack() {
  loginStep.value = 'email'
  loginPassword.value = ''
  localError.value = null
  authError.value = null
  needsVerification.value = false
}

async function onLogin() {
  localError.value = null
  authError.value = null
  submitting.value = true
  try {
    await login(loginUsername.value, loginPassword.value)
    loginPassword.value = ''
    if (accessToken.value) {
      await refreshProfile()
      if (userId.value) await refreshWalletState(userId.value)
      navigateTo('/app')
    }
  } catch {
    localError.value = authError.value ?? 'Login failed'
  } finally {
    submitting.value = false
  }
}

async function onRegister() {
  localError.value = null
  authError.value = null
  submitting.value = true
  regSuccess.value = false
  try {
    await register(regEmail.value, regPassword.value, undefined, subscribeToNewsletter.value)
    regSuccessEmail.value = regEmail.value
    regSuccess.value = true
  } catch {
    localError.value = authError.value ?? 'Registration failed'
  } finally {
    submitting.value = false
  }
}

async function handleResendVerification() {
  if (!verificationEmail.value) return
  resendingVerification.value = true
  resendSuccess.value = false
  try {
    const result = await resendVerificationEmail(verificationEmail.value)
    if (result.success) {
      resendSuccess.value = true
      setTimeout(() => { resendSuccess.value = false }, 5000)
    }
  } finally {
    resendingVerification.value = false
  }
}

// Stars only for the left cosmic zone
const stars = Array.from({ length: 110 }, () => ({
  left: `${Math.random() * 100}%`,
  top: `${Math.random() * 100}%`,
  delay: `${(Math.random() * 4).toFixed(2)}s`,
  size: Math.random() > 0.7 ? '2px' : '1.5px',
  opacity: (0.25 + Math.random() * 0.55).toFixed(2),
}))

const features = [
  { icon: Bot, title: 'AI trading in plain language', desc: 'Just type what you want — the agent handles the rest' },
  { icon: ArrowLeftRight, title: 'Instant token swaps', desc: 'Trade any token pair via Ston.fi DEX at best rates' },
  { icon: ListOrdered, title: 'Automated limit orders', desc: 'Set price targets and let orders execute automatically' },
  { icon: ShieldCheck, title: 'Non-custodial security', desc: 'Your keys, encrypted and stored only on your device' },
  { icon: TrendingUp, title: 'Real-time market data', desc: 'Live TON ecosystem prices and portfolio tracking' },
]

onMounted(() => {
  if (accessToken.value) navigateTo('/app')
})
</script>

<style scoped>
/* Input fields styling for dark panel */
.auth-input {
  width: 100%;
  border-radius: 0.75rem;
  background: rgba(255, 255, 255, 0.06);
  border: 1px solid rgba(255, 255, 255, 0.12);
  padding: 0.75rem 1rem;
  font-size: 0.875rem;
  color: #fff;
  outline: none;
  transition: border-color 0.15s, background 0.15s;
}
.auth-input::placeholder { color: rgba(255,255,255,0.25); }
.auth-input:focus {
  border-color: rgba(99, 102, 241, 0.6);
  background: rgba(255, 255, 255, 0.09);
  box-shadow: 0 0 0 3px rgba(99, 102, 241, 0.12);
}

/* Subtle divider glow between zones */
.zone-divider {
  position: absolute;
  top: 0; right: 0; bottom: 0;
  width: 1px;
  background: linear-gradient(
    to bottom,
    transparent 0%,
    rgba(99, 102, 241, 0.15) 20%,
    rgba(139, 92, 246, 0.25) 50%,
    rgba(99, 102, 241, 0.15) 80%,
    transparent 100%
  );
}
</style>

<template>
  <div class="min-h-screen flex flex-col lg:flex-row overflow-hidden">

    <!-- ══════════════════════════════════════════
         LEFT ZONE — Cosmic gradient + form
    ══════════════════════════════════════════ -->
    <div
      class="relative flex flex-col items-center justify-center px-6 py-12 lg:py-0 flex-1 lg:w-[52%] lg:flex-none"
      style="background: linear-gradient(145deg, #08061e 0%, #0d0b2a 45%, #150829 100%);"
    >
      <!-- Nebula glows -->
      <div class="absolute inset-0 pointer-events-none" style="background-image: radial-gradient(ellipse at 25% 35%, rgba(99,102,241,0.22) 0%, transparent 55%), radial-gradient(ellipse at 75% 70%, rgba(139,92,246,0.16) 0%, transparent 50%), radial-gradient(ellipse at 50% 5%, rgba(59,130,246,0.1) 0%, transparent 40%);"></div>

      <!-- Stars -->
      <div class="absolute inset-0 pointer-events-none overflow-hidden">
        <div
          v-for="(star, i) in stars" :key="i"
          class="absolute rounded-full bg-white animate-pulse"
          :style="{ left: star.left, top: star.top, width: star.size, height: star.size, opacity: star.opacity, animationDelay: star.delay, animationDuration: '3.5s' }"
        />
      </div>

      <!-- Zone divider line (desktop only) -->
      <div class="zone-divider hidden lg:block"></div>

      <!-- Content -->
      <div class="relative z-10 w-full max-w-md">

        <!-- Logo -->
        <a href="/" class="flex items-center gap-3 mb-10 group w-fit">
          <svg width="40" height="40" viewBox="0 0 96 96" fill="none" xmlns="http://www.w3.org/2000/svg" class="transition-transform group-hover:scale-105">
            <path d="M0 38.4C0 24.9587 0 18.2381 2.61584 13.1042C4.9168 8.58834 8.58834 4.9168 13.1042 2.61584C18.2381 0 24.9587 0 38.4 0H57.6C71.0413 0 77.7619 0 82.8958 2.61584C87.4117 4.9168 91.0832 8.58834 93.3842 13.1042C96 18.2381 96 24.9587 96 38.4V57.6C96 71.0413 96 77.7619 93.3842 82.8958C91.0832 87.4117 87.4117 91.0832 82.8958 93.3842C77.7619 96 71.0413 96 57.6 96H38.4C24.9587 96 18.2381 96 13.1042 93.3842C8.58834 91.0832 4.9168 87.4117 2.61584 82.8958C0 77.7619 0 71.0413 0 57.6V38.4Z" fill="#6366f1"/>
            <path d="M71.6404 45H24.3596C20.9765 45 19.3231 40.8734 21.7703 38.5375L45.4107 15.9716C46.8598 14.5884 49.1402 14.5884 50.5893 15.9716L74.2297 38.5374C76.6768 40.8734 75.0235 45 71.6404 45Z" fill="white"/>
            <path fill-rule="evenodd" clip-rule="evenodd" d="M28.1159 52.8038C27.4167 52.1278 26.4821 51.75 25.5096 51.75L24.3595 51.75C20.9764 51.75 19.323 55.8766 21.7702 58.2126L45.4106 80.7784C46.8597 82.1616 49.1401 82.1616 50.5891 80.7784L74.2295 58.2126C76.6767 55.8766 75.0233 51.75 71.6403 51.75L70.4901 51.75C69.5175 51.75 68.583 52.1278 67.8837 52.8038L50.6062 69.5055C49.1526 70.9105 46.847 70.9105 45.3935 69.5054L28.1159 52.8038Z" fill="white"/>
          </svg>
          <div>
            <span class="text-xl font-bold text-white tracking-tight">Esprito</span>
            <span class="text-xl font-bold bg-gradient-to-r from-indigo-400 to-purple-400 bg-clip-text text-transparent"> AI</span>
          </div>
        </a>

        <!-- Form card -->
        <div class="bg-white/[0.04] backdrop-blur-md border border-white/[0.08] rounded-2xl p-7 shadow-2xl">

          <!-- Header -->
          <div class="mb-6">
            <h2 class="text-xl font-bold text-white mb-1">
              <template v-if="!isLogin">Create your account</template>
              <template v-else-if="loginStep === 'email'">Welcome back</template>
              <template v-else>Enter your password</template>
            </h2>
            <!-- Step 1 or register: subtitle text -->
            <p v-if="!isLogin" class="text-sm text-white/40">Join Esprito AI — free to start</p>
            <p v-else-if="loginStep === 'email'" class="text-sm text-white/40">Sign in to continue to Esprito AI</p>
            <!-- Step 2: show email with edit link -->
            <div v-else class="flex items-center gap-2 mt-1">
              <span class="text-sm text-white/55 font-medium truncate">{{ loginUsername }}</span>
              <button type="button" @click="onLoginBack"
                class="flex items-center gap-1 text-xs text-indigo-400 hover:text-indigo-300 transition flex-shrink-0">
                <Pencil :size="11" />
                <span>Edit</span>
              </button>
            </div>
          </div>

          <!-- ── LOGIN: STEP 1 — Email ── -->
          <form v-if="isLogin && loginStep === 'email'" class="space-y-4" @submit.prevent="onLoginEmailContinue">
            <div>
              <label class="text-xs font-medium text-white/45 mb-1.5 flex items-center gap-1.5">
                <Mail :size="11" />Email or username
              </label>
              <input v-model="loginUsername" type="text" placeholder="your@email.com" required autocomplete="username" class="auth-input" />
            </div>

            <button type="submit" :disabled="!loginUsername"
              class="w-full cosmic-button rounded-xl py-3 text-sm font-bold text-white disabled:opacity-40 disabled:cursor-not-allowed flex items-center justify-center gap-2 mt-1">
              <span class="flex items-center gap-2">Continue <ChevronRight :size="15" /></span>
            </button>

            <!-- Switch to register -->
            <div class="flex items-center gap-3 mt-5">
              <div class="flex-1 h-px bg-white/[0.07]"></div>
            </div>
            <p class="text-center text-xs text-white/35 mt-3">
              Don't have an account?
              <button type="button" @click="switchMode('register')"
                class="text-indigo-400 hover:text-indigo-300 font-semibold transition ml-1">
                Sign up for free
              </button>
            </p>
          </form>

          <!-- ── LOGIN: STEP 2 — Password ── -->
          <form v-else-if="isLogin && loginStep === 'password'" class="space-y-4" @submit.prevent="onLogin">
            <div>
              <label class="text-xs font-medium text-white/45 mb-1.5 flex items-center gap-1.5">
                <Lock :size="11" />Password
              </label>
              <input ref="passwordInputRef" v-model="loginPassword" type="password" placeholder="••••••••" required autocomplete="current-password" class="auth-input" />
            </div>

            <!-- Email verification needed -->
            <div v-if="needsVerification" class="rounded-xl bg-amber-500/10 border border-amber-500/25 p-4 space-y-3">
              <div class="flex items-start gap-2.5">
                <Mail :size="15" class="text-amber-400 mt-0.5 flex-shrink-0" />
                <div>
                  <div class="text-xs font-semibold text-amber-200 mb-0.5">Email verification required</div>
                  <div class="text-xs text-amber-300/70">Check your inbox and click the link to activate your account.</div>
                </div>
              </div>
              <div v-if="resendSuccess" class="flex items-center gap-2 p-2 rounded-lg bg-green-500/15 border border-green-500/25">
                <CheckCircle :size="13" class="text-green-400" />
                <span class="text-xs text-green-300">Verification email sent!</span>
              </div>
              <button type="button" @click="handleResendVerification" :disabled="resendingVerification"
                class="w-full rounded-lg bg-amber-500/15 hover:bg-amber-500/25 border border-amber-500/25 px-3 py-2 text-xs font-semibold text-amber-200 transition flex items-center justify-center gap-2">
                <Loader v-if="resendingVerification" :size="13" class="animate-spin" />
                <Mail v-else :size="13" />
                {{ resendingVerification ? 'Sending...' : 'Resend Verification Email' }}
              </button>
            </div>

            <!-- Error -->
            <div v-else-if="localError" class="flex items-start gap-2.5 p-3 rounded-xl bg-red-500/10 border border-red-500/25">
              <AlertTriangle :size="15" class="text-red-400 flex-shrink-0 mt-0.5" />
              <span class="text-xs text-red-300">{{ localError }}</span>
            </div>

            <button type="submit" :disabled="submitting || !loginPassword"
              class="w-full cosmic-button rounded-xl py-3 text-sm font-bold text-white disabled:opacity-40 disabled:cursor-not-allowed flex items-center justify-center gap-2 mt-1">
              <Loader v-if="submitting" :size="15" class="animate-spin" />
              <span v-else class="flex items-center gap-2">Continue <ChevronRight :size="15" /></span>
            </button>
          </form>

          <!-- ── REGISTER FORM ── -->
          <div v-else>
            <!-- Success state -->
            <div v-if="regSuccess" class="py-4 flex flex-col items-center text-center gap-4">
              <div class="w-14 h-14 rounded-full bg-green-500/15 border border-green-500/25 flex items-center justify-center">
                <CheckCircle :size="28" class="text-green-400" />
              </div>
              <div>
                <h4 class="text-base font-bold text-white mb-1.5">Check your inbox!</h4>
                <p class="text-sm text-white/45">We sent a verification link to <strong class="text-white">{{ regSuccessEmail }}</strong></p>
                <p class="text-xs text-white/30 mt-2">Don't forget to check your spam folder.</p>
              </div>
              <button type="button" @click="switchMode('login')" class="cosmic-button rounded-xl px-6 py-2.5 text-sm font-bold text-white">
                Go to Sign In
              </button>
            </div>

            <form v-else class="space-y-4" @submit.prevent="onRegister">
              <div>
                <label class="text-xs font-medium text-white/45 mb-1.5 flex items-center gap-1.5"><Mail :size="11" />Email</label>
                <input v-model="regEmail" type="email" placeholder="your@email.com" required autocomplete="email" class="auth-input" />
              </div>
              <div>
                <label class="text-xs font-medium text-white/45 mb-1.5 flex items-center gap-1.5"><Lock :size="11" />Password</label>
                <input v-model="regPassword" type="password" placeholder="••••••••" required autocomplete="new-password" class="auth-input" />
              </div>
              <!-- Newsletter opt-in — subtle footnote style -->
              <label class="flex items-center gap-2 cursor-pointer group py-0.5 select-none">
                <input v-model="subscribeToNewsletter" type="checkbox"
                  class="w-3 h-3 rounded-sm border-white/20 text-indigo-500 focus:ring-0 focus:ring-offset-0 cursor-pointer flex-shrink-0 bg-white/8 checked:bg-indigo-600 checked:border-indigo-600 transition-colors" />
                <span class="text-[11px] text-white/30 group-hover:text-white/45 transition-colors leading-none">
                  Send me product updates and news
                </span>
              </label>

              <!-- Error -->
              <div v-if="localError" class="flex items-start gap-2.5 p-3 rounded-xl bg-red-500/10 border border-red-500/25">
                <AlertTriangle :size="15" class="text-red-400 flex-shrink-0 mt-0.5" />
                <span class="text-xs text-red-300">{{ localError }}</span>
              </div>

              <button type="submit" :disabled="submitting || !regEmail || !regPassword"
                class="w-full cosmic-button rounded-xl py-3 text-sm font-bold text-white disabled:opacity-40 disabled:cursor-not-allowed flex items-center justify-center gap-2">
                <Loader v-if="submitting" :size="15" class="animate-spin" />
                <span v-else class="flex items-center gap-2">Continue <ChevronRight :size="15" /></span>
              </button>

              <!-- Switch to login -->
              <div class="flex items-center gap-3 mt-5">
                <div class="flex-1 h-px bg-white/[0.07]"></div>
              </div>
              <p class="text-center text-xs text-white/35 mt-3">
                Already have an account?
                <button type="button" @click="switchMode('login')"
                  class="text-indigo-400 hover:text-indigo-300 font-semibold transition ml-1">
                  Sign in
                </button>
              </p>
            </form>
          </div>

        </div>

        <!-- Below form: legal notice -->
        <p class="text-center text-xs text-white/50 leading-relaxed mt-5">
          By continuing, you agree to our
          <a href="/terms" target="_blank" rel="noopener noreferrer" class="text-white/70 hover:text-white underline underline-offset-2 transition">Terms of Service</a>
          and
          <a href="/privacy" target="_blank" rel="noopener noreferrer" class="text-white/70 hover:text-white underline underline-offset-2 transition">Privacy Policy</a>.
        </p>

      </div>
    </div>

    <!-- ══════════════════════════════════════════
         RIGHT ZONE — Dark solid panel + features
    ══════════════════════════════════════════ -->
    <div
      class="hidden lg:flex flex-col justify-center px-14 xl:px-20 py-16 flex-1"
      style="background: #06060f;"
    >
      <!-- Subtle top-right glow -->
      <div class="absolute top-0 right-0 w-96 h-96 pointer-events-none" style="background: radial-gradient(circle at top right, rgba(99,102,241,0.07) 0%, transparent 60%);"></div>

      <div class="relative max-w-md">
        <div class="mb-10">
          <h2 class="text-3xl xl:text-4xl font-bold text-white leading-snug mb-3">
            Trade smarter<br>
            <span class="bg-gradient-to-r from-indigo-400 via-purple-400 to-pink-400 bg-clip-text text-transparent">on TON blockchain</span>
          </h2>
          <p class="text-sm text-white/35 leading-relaxed">
            Your AI-powered agent — swap tokens, set limit orders, and manage your wallet with simple text commands.
          </p>
        </div>

        <div class="space-y-6">
          <div v-for="f in features" :key="f.title" class="flex items-start gap-4 group">
            <div class="w-9 h-9 rounded-lg bg-indigo-500/10 border border-indigo-500/20 flex items-center justify-center flex-shrink-0 group-hover:bg-indigo-500/15 group-hover:border-indigo-500/35 transition duration-200">
              <component :is="f.icon" :size="16" class="text-indigo-400" />
            </div>
            <div>
              <div class="text-sm font-semibold text-white/80 mb-0.5">{{ f.title }}</div>
              <div class="text-xs text-white/30 leading-relaxed">{{ f.desc }}</div>
            </div>
          </div>
        </div>
      </div>
    </div>

  </div>
</template>
