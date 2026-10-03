<template>
  <main class="cs-page">
    <section class="photo-panel">
      <img class="cs-photo" src="/cs-side.jpg" alt="CS original" />
    </section>
    <section class="photo-panel">
      <img class="cs-photo" src="/cs-photo.jpg" alt="CS" />
    </section>
    <section class="photo-panel">
      <img class="cs-photo cs-photo--mirrored" src="/cs-side.jpg" alt="CS mirrored" />
    </section>
  </main>
</template>

<style scoped>
.cs-page {
  background: #101010;
  display: flex;
  height: 100%;
  width: 100%;
}

.photo-panel {
  align-items: center;
  display: flex;
  flex: 1 1 0;
  justify-content: center;
  min-width: 0;
  overflow: hidden;
}

.cs-photo {
  display: block;
  height: 100%;
  max-width: 100%;
  object-fit: contain;
  width: 100%;
}

.cs-photo--mirrored {
  transform: scaleX(-1);
  transform-origin: center;
}

@media (max-width: 768px) {
  .cs-page {
    flex-direction: column;
    overflow-y: auto;
  }

  .photo-panel {
    flex: 0 0 100%;
    min-height: 100%;
  }
}
</style>
