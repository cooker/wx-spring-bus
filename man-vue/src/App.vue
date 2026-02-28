<template>
  <a-config-provider :locale="zhCN">
    <div id="app">
      <router-view />
    </div>
  </a-config-provider>
</template>

<script setup>
import zhCN from 'ant-design-vue/es/locale/zh_CN'
</script>

<style>
#app {
  font-family: system-ui, sans-serif;
  min-height: 100vh;
}
</style>
