<script setup lang="ts">
import AuthCard from '../components/AuthCard.vue'
import BalanceCard from '../components/BalanceCard.vue'
import ChatPanel from '../components/ChatPanel.vue'
</script>

<template>
  <div class="grid h-full gap-4 lg:grid-cols-[1.2fr_0.8fr]">
    <div class="flex h-full">
      <ChatPanel />
    </div>
    <div class="flex h-full flex-col gap-4">
      <AuthCard />
      <BalanceCard />
    </div>
  </div>
</template>
