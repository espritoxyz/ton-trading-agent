<script setup lang="ts">
import AuthCard from '../components/AuthCard.vue'
import BalanceCard from '../components/BalanceCard.vue'
import ChatPanel from '../components/ChatPanel.vue'
</script>

<template>
  <div class="grid gap-4 lg:grid-cols-[1.2fr_0.8fr]">
    <div class="h-[70vh] lg:h-[calc(100vh-4rem)]">
      <ChatPanel />
    </div>
    <div class="flex h-[70vh] flex-col gap-4 lg:h-[calc(100vh-4rem)]">
      <AuthCard />
      <BalanceCard />
    </div>
  </div>
</template>
