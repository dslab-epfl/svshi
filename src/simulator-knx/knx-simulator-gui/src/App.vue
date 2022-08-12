<script setup lang="ts">
import { RouterLink, RouterView } from 'vue-router'
import HelloWorld from '@/components/HelloWorld.vue'
</script>

<template>
    <div class="wrapper">
      <h1>KNX Simulator</h1>
<!-- 
      <nav>
        <RouterLink to="/">Home</RouterLink>
        <RouterLink to="/about">About</RouterLink>
      </nav> -->
    </div>

  <RouterView />
</template>

<style>
@import '@/assets/base.css';

</style>
