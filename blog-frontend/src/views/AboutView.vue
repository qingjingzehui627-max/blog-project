<template>
  <div class="space-y-8">
    <section class="overflow-hidden rounded-[28px] border border-[#d9dcff] bg-[linear-gradient(180deg,#faf8ff_0%,#f5f7ff_100%)] p-6 shadow-[0_18px_44px_rgba(99,102,241,0.10)] sm:p-8 lg:p-10">
      <div class="grid grid-cols-2 items-center gap-2">
        <!-- 左侧：头像 -->
        <div class="flex items-center justify-center">
          <div class="h-72 w-72 overflow-hidden rounded-[28px] shadow-[0_20px_48px_rgba(99,102,241,0.35)]">
            <img :src="avatarSrc" alt="avatar" class="h-full w-full object-cover" />
          </div>
        </div>

        <!-- 右侧：简介 -->
        <div class="min-w-0">
          <p class="text-sm font-semibold tracking-[0.15em] text-[#7c3aed]">👋 你好，我是</p>
          <h1 class="mt-1 bg-gradient-to-r from-[#4f46e5] via-[#6366f1] to-[#8b5cf6] bg-clip-text text-4xl font-black tracking-tight text-transparent sm:text-5xl">
            风中有诗
          </h1>
          <p class="mt-2 text-base font-medium text-[#5b57d9]">
            全栈开发 / 部署运维 / 博客作者
          </p>
          <p class="mt-3 break-words text-[15px] leading-7 text-slate-600">
            我是一名热爱技术与 AI 的全栈开发者。4 年+ 的工程经验让我深信：好的产品需要同时具备技术深度和设计温度。我喜欢用文字记录思考，与你分享那些在代码和像素之间发现的有趣洞见。
          </p>
          <div class="mt-5 flex flex-wrap gap-3">
            <a
              href="mailto:qingjingzehui627@gmail.com"
              class="inline-flex items-center rounded-2xl bg-[linear-gradient(90deg,#4f46e5_0%,#8b5cf6_100%)] px-6 py-3 text-sm font-semibold text-white shadow-[0_14px_28px_rgba(99,102,241,0.18)] transition hover:opacity-90"
            >
              邮箱：qingjingzehui627@gmail.com
            </a>
          </div>
        </div>
      </div>
    </section>

    <div class="grid grid-cols-2 items-start gap-8">
      <!-- 技术栈 -->
      <section class="space-y-3">
        <h2 class="bg-gradient-to-r from-[#312e81] via-[#6366f1] to-[#8b5cf6] bg-clip-text text-xl font-black tracking-tight text-transparent">
          技术栈
        </h2>
        <div class="grid gap-3">
          <div
            v-for="group in stackGroups"
            :key="group.title"
            class="rounded-[16px] border border-[#d9dcff] bg-white/95 p-4 shadow-[0_8px_24px_rgba(99,102,241,0.07)]"
          >
            <h3 class="text-xs font-bold text-[#5b57d9]">{{ group.title }}</h3>
            <div class="mt-2 flex flex-wrap gap-1.5">
              <span
                v-for="item in group.items"
                :key="item"
                class="inline-flex items-center rounded-full border border-[#cdc8ff] bg-[linear-gradient(180deg,#f6f2ff_0%,#eef2ff_100%)] px-2.5 py-1 text-xs font-semibold text-[#5b57d9]"
              >
                {{ item }}
              </span>
            </div>
          </div>
        </div>
      </section>

      <!-- 成长历程 -->
      <section class="space-y-3">
        <h2 class="bg-gradient-to-r from-[#312e81] via-[#6366f1] to-[#8b5cf6] bg-clip-text text-xl font-black tracking-tight text-transparent">
          成长历程
        </h2>
        <div class="relative">
          <!-- 竖线 -->
          <div class="absolute left-[11px] top-2 bottom-2 w-px bg-[#d9dcff]"></div>
          <div class="space-y-3">
            <div
              v-for="item in milestones"
              :key="item.year"
              class="relative flex gap-4"
            >
              <!-- 时间轴圆点 -->
              <div class="relative z-10 mt-3 flex h-6 w-6 flex-shrink-0 items-center justify-center rounded-full border-2 border-[#6366f1] bg-white">
                <div class="h-2 w-2 rounded-full bg-[#6366f1]"></div>
              </div>
              <!-- 内容卡片 -->
              <div class="flex-1 rounded-[12px] border border-[#d9dcff] bg-white/95 p-3 shadow-[0_6px_18px_rgba(99,102,241,0.07)]">
                <div class="flex items-center gap-2">
                  <span class="rounded-full bg-[#eef2ff] px-2.5 py-0.5 text-xs font-bold text-[#6366f1]">{{ item.year }}</span>
                  <span class="text-sm font-bold text-slate-900">{{ item.title }}</span>
                </div>
                <p class="mt-1.5 text-xs leading-5 text-slate-500">{{ item.desc }}</p>
              </div>
            </div>
          </div>
        </div>
      </section>
    </div>
  </div>
</template>

<script setup>
const avatarSrc = '/uploads/image/f96385ad-b645-41b6-9c9d-3567c715ee0a.png'

const stackGroups = [
  {
    title: '前端技术',
    items: ['Vue 3', 'JavaScript', 'TailwindCSS', 'Vite', 'Pinia']
  },
  {
    title: '后端技术',
    items: ['Spring Boot', 'MySQL', 'JWT', 'MyBatis-Plus', 'REST API']
  },
  {
    title: '部署运维',
    items: ['Linux', 'Docker', 'Nginx', 'GitHub Actions', '服务部署']
  },
  {
    title: 'AI 与效率',
    items: ['Prompt 设计', 'AI 工具协作', '自动化提效', '内容整理', '界面优化']
  }
]

const milestones = [
  {
    year: '2024',
    title: '全栈与 AI 深入实践',
    desc: '开始把 AI 能力融入开发流程，持续打磨从前端到部署的一体化项目能力。'
  },
  {
    year: '2023',
    title: '部署与运维能力拓展',
    desc: '从功能开发延伸到上线部署、服务维护与稳定性优化。'
  },
  {
    year: '2022',
    title: '全栈能力持续进阶',
    desc: '开始系统串联前端、后端和数据库设计，形成完整工程视角。'
  },
  {
    year: '2020',
    title: '开启代码与写作记录',
    desc: '在学习与实践中持续沉淀博客内容，记录每一次思考与成长。'
  }
]
</script>
