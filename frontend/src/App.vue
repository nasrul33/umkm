<template>
  <router-view />
</template>

<script setup>
// SRS-A-05: root ini hanya merender router-view. Pemisahan publik/privat
// dilakukan di router/index.js, bukan di sini — jangan taruh logic auth global di App.vue.
</script>
