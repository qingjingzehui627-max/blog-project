<template>
  <footer :class="footerClass">
    <div class="mx-auto w-full max-w-[1240px] px-3 py-6 sm:px-4 lg:px-5">
      <div :class="panelClass">
        <p :class="brandLineClass">
          <span class="font-black italic text-[#4f46e5]">{{ siteConfig.brandName }}</span>
          <span class="hidden sm:inline">|</span>
          <span>{{ siteConfig.footerNote }}</span>
        </p>

        <div :class="recordLineClass">
          <a
            v-if="icpNumber"
            :href="siteConfig.filing.icp.href"
            target="_blank"
            rel="noreferrer"
            :class="recordLinkClass"
          >
            ICP备案：{{ icpNumber }}
          </a>

          <span v-if="icpNumber && publicSecurityNumber" :class="dividerClass">|</span>

          <a
            v-if="publicSecurityNumber"
            :href="siteConfig.filing.publicSecurity.href"
            target="_blank"
            rel="noreferrer"
            :class="recordLinkClass"
          >
            公安备案：{{ publicSecurityText }}
          </a>
        </div>

        <p :class="metaLineClass">
          <span>(c) {{ currentYear }} {{ siteConfig.brandName }}</span>
          <span class="hidden sm:inline">|</span>
          <span>备案信息仅用于网站合规展示</span>
        </p>
      </div>
    </div>
  </footer>
</template>

<script setup>
import { computed } from 'vue'
import { useTheme } from '../composables/useTheme'
import { siteConfig } from '../config/site'

const { isDark } = useTheme()
const currentYear = new Date().getFullYear()

const icpNumber = computed(() => siteConfig.filing.icp.number.trim())
const publicSecurityCity = computed(() => (siteConfig.filing.publicSecurity.city || '').trim())
const publicSecurityNumber = computed(() => siteConfig.filing.publicSecurity.number.trim())

const publicSecurityText = computed(() =>
  publicSecurityCity.value
    ? `${publicSecurityCity.value} ${publicSecurityNumber.value}`
    : publicSecurityNumber.value
)

const footerClass = computed(() =>
  isDark.value
    ? 'mt-auto border-t border-white/10 bg-[linear-gradient(180deg,rgba(12,12,18,0.96)_0%,rgba(8,8,12,1)_100%)]'
    : 'mt-auto border-t border-[#d9dcff] bg-[linear-gradient(180deg,rgba(248,250,255,0.82)_0%,rgba(240,244,255,0.96)_100%)]'
)

const panelClass = computed(() =>
  isDark.value
    ? 'rounded-[24px] border border-white/10 bg-white/[0.03] px-5 py-5 text-center shadow-[0_18px_50px_rgba(0,0,0,0.24)]'
    : 'rounded-[24px] border border-[#d9dcff] bg-white/85 px-5 py-5 text-center shadow-[0_18px_50px_rgba(99,102,241,0.10)]'
)

const brandLineClass = computed(() =>
  isDark.value
    ? 'flex flex-wrap items-center justify-center gap-x-2 gap-y-1 text-sm text-zinc-400'
    : 'flex flex-wrap items-center justify-center gap-x-2 gap-y-1 text-sm text-slate-500'
)

const recordLineClass = computed(() =>
  isDark.value
    ? 'mt-3 flex flex-wrap items-center justify-center gap-x-3 gap-y-2 text-sm text-zinc-200'
    : 'mt-3 flex flex-wrap items-center justify-center gap-x-3 gap-y-2 text-sm text-slate-700'
)

const recordLinkClass = computed(() =>
  isDark.value
    ? 'transition hover:text-white'
    : 'transition hover:text-[#4f46e5]'
)

const dividerClass = computed(() =>
  isDark.value ? 'text-zinc-600' : 'text-slate-300'
)

const metaLineClass = computed(() =>
  isDark.value
    ? 'mt-3 flex flex-wrap items-center justify-center gap-x-2 gap-y-1 text-xs text-zinc-500'
    : 'mt-3 flex flex-wrap items-center justify-center gap-x-2 gap-y-1 text-xs text-slate-400'
)
</script>
