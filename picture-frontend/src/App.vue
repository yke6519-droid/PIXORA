<template>
  <div id="app">
    <router-view/>
  </div>
</template>

<script setup>
import BasicLayout from './layout/BasicLayout.vue';
import { healthUsingGet } from './api/mainController';
import { onMounted } from 'vue';
import { getCurrentUserUsingGet } from './api/userController';

onMounted(async()=>{
})

</script>



<style>
html, body {
  background: #fff !important;
  color: #000;
  margin: 0;
  min-height: 100vh;
}
</style>
