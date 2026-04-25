<template>
  <component
    :is="as"
    v-bind="$attrs"
    :class="[
      'rounded-2xl border border-slate-200 bg-white shadow-[0_8px_24px_rgba(15,23,42,0.08)]',
      hover ? 'transition duration-200 hover:-translate-y-0.5 hover:shadow-[0_12px_32px_rgba(99,102,241,0.16)]' : '',
      cardClass
    ]"
  >
    <slot />
  </component>
</template>

<script setup>
defineOptions({ inheritAttrs: false })

defineProps({
  as: {
    type: [String, Object],
    default: 'section'
  },
  hover: {
    type: Boolean,
    default: false
  },
  cardClass: {
    type: String,
    default: ''
  }
})
</script>
