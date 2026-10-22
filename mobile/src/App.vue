<script setup lang="ts">
import { onLaunch, onShow, onHide } from "@dcloudio/uni-app";
onLaunch(() => {
  console.log("App Launch");
});
onShow(() => {
  console.log("App Show");
});
onHide(() => {
  console.log("App Hide");
});
</script>
<style lang="scss">
/* 全局基础样式 */
page {
  background-color: #f4f7f9;
  font-size: 28rpx;
  color: #1f2d3d;
}
</style>
