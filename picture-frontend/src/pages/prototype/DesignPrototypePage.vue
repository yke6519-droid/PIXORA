<template>
  <a-config-provider :theme="themeConfig">
    <main ref="root" class="prototype-shell">
      <nav class="prototype-nav">
        <router-link to="/prototype" class="brand-lockup">
          <span class="brand-mark">云</span>
          <span class="brand-name">智能云图库</span>
        </router-link>

        <div class="nav-links">
          <a href="#archive">浏览图库</a>
          <a href="#rhythm">设计理念</a>
          <a href="#contact">关于项目</a>
        </div>

        <div class="nav-actions">
          <a-button class="nav-login" type="text">登录</a-button>
          <a-button class="nav-upload" type="primary" @click="scrollToArchive">
            上传图片
            <ArrowUpOutlined />
          </a-button>
          <a-avatar class="nav-avatar" :size="38">C</a-avatar>
        </div>
      </nav>

      <section class="hero-section">
        <div class="hero-copy">
          <p class="eyebrow">智能云图库 · 视觉资产工作台</p>
          <h1>
            让每一张图像，<span class="inline-image inline-image-one" aria-hidden="true"></span>
            都拥有自己的秩序。
          </h1>
          <p class="hero-description">
            从灵感采集到团队协作，把分散的图像资产整理成一套清晰、可检索、可持续生长的视觉系统。
          </p>
          <div class="hero-actions">
            <a-button type="primary" size="large" class="hero-primary" @click="scrollToArchive">
              进入图库
              <ArrowRightOutlined />
            </a-button>
            <a-button size="large" class="hero-secondary" @click="scrollToRhythm">
              看看它如何工作
            </a-button>
          </div>
        </div>

        <div class="hero-media" aria-label="图库精选图片预览">
          <div class="hero-image hero-image-main"></div>
          <div class="hero-image hero-image-float"></div>
          <div class="hero-caption">
            <span>精选视觉资产</span>
            <span>2026 / 06</span>
          </div>
        </div>
      </section>

      <section id="archive" class="archive-section">
        <div class="section-intro">
          <div>
            <p class="eyebrow">正在被重新发现</p>
            <h2>一个更有呼吸感的图库入口</h2>
          </div>
          <div class="archive-tools">
            <a-input-search
              v-model:value="searchText"
              class="archive-search"
              size="large"
              placeholder="搜索图像、标签或分类"
              enter-button="搜索"
            />
            <a-button class="filter-button" size="large">筛选 <span>＋</span></a-button>
          </div>
        </div>

        <div class="marquee-band" aria-label="图库分类">
          <div class="marquee-track">
            <span v-for="item in marqueeItems" :key="item + '-a'">{{ item }}</span>
            <span v-for="item in marqueeItems" :key="item + '-b'">{{ item }}</span>
          </div>
        </div>

        <div class="bento-grid">
          <article
            v-for="(item, index) in galleryItems"
            :key="item.title"
            class="gallery-card"
            :class="`gallery-card-${index + 1}`"
          >
            <div class="gallery-image" :style="{ backgroundImage: `url(${item.image})` }"></div>
            <div class="gallery-shade"></div>
            <div class="gallery-card-content">
              <span class="gallery-kind">{{ item.kind }}</span>
              <h3>{{ item.title }}</h3>
              <span class="gallery-arrow"><ArrowUpOutlined /></span>
            </div>
          </article>
        </div>
      </section>

      <section id="rhythm" class="desire-section">
        <div class="desire-copy">
          <p class="eyebrow">让搜索变成一种节奏</p>
          <h2>从收藏，到真正找到。</h2>
          <p class="reveal-copy">
            <span v-for="(word, index) in revealWords" :key="`${word}-${index}`" class="reveal-word">{{ word }}</span>
          </p>
          <a-button type="primary" size="large" class="dark-button" @click="scrollToArchive">
            探索全部图像
            <ArrowRightOutlined />
          </a-button>
        </div>

        <div class="accordion-stack">
          <article v-for="item in accordionItems" :key="item.title" class="accordion-item">
            <div class="accordion-image" :style="{ backgroundImage: `url(${item.image})` }"></div>
            <div class="accordion-overlay"></div>
            <div class="accordion-content">
              <span>{{ item.index }}</span>
              <h3>{{ item.title }}</h3>
              <p>{{ item.description }}</p>
            </div>
            <ArrowRightOutlined class="accordion-arrow" />
          </article>
        </div>
      </section>

      <section class="feedback-section">
        <div class="feedback-image" :style="{ backgroundImage: `url(${quotes[quoteIndex].image})` }"></div>
        <div class="feedback-content">
          <p class="eyebrow">团队协作的另一种语言</p>
          <blockquote>“{{ quotes[quoteIndex].quote }}”</blockquote>
          <div class="feedback-person">
            <div>
              <strong>{{ quotes[quoteIndex].name }}</strong>
              <span>{{ quotes[quoteIndex].role }}</span>
            </div>
            <div class="feedback-controls">
              <button aria-label="上一条反馈" @click="previousQuote">←</button>
              <button aria-label="下一条反馈" @click="nextQuote">→</button>
            </div>
          </div>
        </div>
      </section>

      <section id="contact" class="action-section">
        <div>
          <p class="eyebrow">准备好整理你的下一组图像了吗</p>
          <h2>把灵感留在<br />一个值得回来的地方。</h2>
        </div>
        <a-button type="primary" size="large" class="action-button" @click="scrollToArchive">
          开始整理图库
          <ArrowRightOutlined />
        </a-button>
      </section>

      <footer class="prototype-footer">
        <span>智能云图库 · 视觉资产工作台</span>
        <span>静态设计原型</span>
        <span>Vue 3 / Ant Design Vue</span>
      </footer>
    </main>
  </a-config-provider>
</template>

<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref } from 'vue'
import { ArrowRightOutlined, ArrowUpOutlined } from '@ant-design/icons-vue'

declare global {
  interface Window {
    gsap?: any
    ScrollTrigger?: any
  }
}

const root = ref<HTMLElement | null>(null)
const searchText = ref('')
const quoteIndex = ref(0)
let animationContext: any

const themeConfig = {
  token: {
    colorPrimary: '#d9ff4a',
    colorTextLightSolid: '#11130f',
    borderRadius: 2,
    fontFamily: 'Geist, Avenir Next, PingFang SC, Microsoft YaHei, sans-serif',
  },
}

const marqueeItems = ['城市光线', '品牌影像', '自然纹理', '产品细节', '团队灵感', '空间记录']

const galleryItems = [
  { title: '静默的建筑', kind: '城市光线', image: 'https://picsum.photos/seed/quiet-architecture/1600/1200' },
  { title: '柔软边界', kind: '自然纹理', image: 'https://picsum.photos/seed/soft-boundary/1000/1200' },
  { title: '夜色样本', kind: '品牌影像', image: 'https://picsum.photos/seed/night-sample/1000/1200' },
  { title: '触感与留白', kind: '产品细节', image: 'https://picsum.photos/seed/tactile-space/1200/1000' },
  { title: '温度的切面', kind: '空间记录', image: 'https://picsum.photos/seed/warm-slice/1200/1000' },
  { title: '可见的节奏', kind: '团队灵感', image: 'https://picsum.photos/seed/visible-rhythm/1200/1000' },
]

const accordionItems = [
  { index: 'A', title: '采集', description: '把零散的灵感，放进同一个可以被理解的空间。', image: 'https://picsum.photos/seed/collecting-light/1400/1600' },
  { index: 'B', title: '整理', description: '用分类、标签与空间，让每一次搜索都更接近答案。', image: 'https://picsum.photos/seed/organize-studio/1400/1600' },
  { index: 'C', title: '共创', description: '让团队在同一张视觉地图上工作，留下清晰的来路。', image: 'https://picsum.photos/seed/collaborate-table/1400/1600' },
]

const quotes = [
  { quote: '好的图库不是把图片堆在一起，而是让团队更快地找到共同的方向。', name: '陈晓', role: '视觉设计师', image: 'https://picsum.photos/seed/creative-director/1000/1200' },
  { quote: '当搜索不再是负担，灵感才有机会继续向前发生。', name: '林夏', role: '品牌负责人', image: 'https://picsum.photos/seed/brand-studio/1000/1200' },
]

const revealWords = '把灵感留在一个值得回来的地方。每一次整理，都是在为下一次创作留下线索。'.split('')

function scrollToArchive() {
  document.querySelector('#archive')?.scrollIntoView({ behavior: 'smooth' })
}

function scrollToRhythm() {
  document.querySelector('#rhythm')?.scrollIntoView({ behavior: 'smooth' })
}

function nextQuote() {
  quoteIndex.value = (quoteIndex.value + 1) % quotes.length
}

function previousQuote() {
  quoteIndex.value = (quoteIndex.value - 1 + quotes.length) % quotes.length
}

onMounted(() => {
  const gsap = window.gsap
  const ScrollTrigger = window.ScrollTrigger
  if (!gsap || !ScrollTrigger || !root.value) return

  gsap.registerPlugin(ScrollTrigger)
  animationContext = gsap.context(() => {
    gsap.from('.hero-copy > *', { y: 34, opacity: 0, duration: 1, stagger: 0.08, ease: 'power3.out' })
    gsap.from('.hero-image-main', { scale: 0.84, opacity: 0.25, duration: 1.2, ease: 'power3.out' })

    gsap.to('.desire-copy', {
      scrollTrigger: { trigger: '.desire-section', start: 'top top+=80', end: 'bottom bottom-=120', pin: true, scrub: 1 },
    })

    gsap.fromTo('.reveal-word', { opacity: 0.18 }, {
      opacity: 1,
      stagger: 0.06,
      scrollTrigger: { trigger: '.reveal-copy', start: 'top 72%', end: 'bottom 42%', scrub: 1 },
    })

    gsap.utils.toArray('.gallery-card').forEach((card: HTMLElement) => {
      gsap.fromTo(card, { y: 44, opacity: 0.35 }, {
        y: 0,
        opacity: 1,
        duration: 1,
        ease: 'power3.out',
        scrollTrigger: { trigger: card, start: 'top 88%' },
      })
    })
  }, root.value)
})

onBeforeUnmount(() => {
  animationContext?.revert()
})
</script>

<style scoped>
:global(html) { scroll-behavior: smooth; }
:global(body) { background: #11130f; }
:global(#app) { width: 100%; max-width: none; border: 0; text-align: left; }

.prototype-shell {
  --ink: #11130f;
  --paper: #f1f0e9;
  --acid: #d9ff4a;
  --muted: #9c9f91;
  --line: rgba(241, 240, 233, 0.18);
  width: 100%;
  overflow-x: hidden;
  color: var(--paper);
  background: radial-gradient(circle at 83% 2%, rgba(217, 255, 74, 0.12), transparent 25rem), var(--ink);
  font-family: Geist, 'Avenir Next', 'PingFang SC', 'Microsoft YaHei', sans-serif;
}

.prototype-nav {
  position: sticky;
  top: 18px;
  z-index: 20;
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: min(1360px, calc(100% - 48px));
  min-height: 66px;
  margin: 18px auto 0;
  padding: 10px 12px 10px 20px;
  border: 1px solid rgba(241, 240, 233, 0.18);
  border-radius: 999px;
  background: rgba(17, 19, 15, 0.72);
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.24);
  backdrop-filter: blur(18px);
}

.brand-lockup, .nav-links a { color: var(--paper); text-decoration: none; }
.brand-lockup { display: inline-flex; align-items: center; gap: 12px; font-size: 14px; font-weight: 700; letter-spacing: -0.02em; }
.brand-mark { display: grid; place-items: center; width: 36px; height: 36px; color: var(--ink); background: var(--acid); border-radius: 50%; font-size: 16px; }
.nav-links { display: flex; align-items: center; gap: 34px; margin-left: 90px; font-size: 13px; }
.nav-links a { opacity: 0.68; transition: opacity 0.3s ease; }
.nav-links a:hover { opacity: 1; }
.nav-actions { display: flex; align-items: center; gap: 8px; }
.nav-login :deep(span) { color: var(--paper); }
.nav-upload, .hero-primary, .dark-button, .action-button { color: var(--ink) !important; background: var(--acid) !important; border-color: var(--acid) !important; box-shadow: none !important; }
.nav-upload { height: 42px; padding-inline: 18px; border-radius: 999px; }
.nav-avatar { color: var(--ink); background: #d7d5cc; }

.hero-section, .archive-section, .desire-section, .feedback-section, .action-section, .prototype-footer { width: min(1360px, calc(100% - 48px)); margin-inline: auto; }
.hero-section { display: grid; grid-template-columns: minmax(0, 0.98fr) minmax(420px, 1.02fr); gap: clamp(40px, 7vw, 140px); align-items: center; min-height: 900px; padding: 150px 0 180px; }
.eyebrow { margin: 0 0 28px; color: var(--acid); font-size: 11px; font-weight: 700; letter-spacing: 0.16em; line-height: 1.5; text-transform: uppercase; }
.hero-copy h1, .section-intro h2, .desire-copy h2, .action-section h2 { margin: 0; color: var(--paper); font-weight: 500; letter-spacing: -0.075em; }
.hero-copy h1 { max-width: 72rem; font-size: clamp(3rem, 6vw, 6rem); line-height: 0.98; }
.inline-image { display: inline-block; width: 1.2em; height: 0.62em; margin: 0 0.08em; vertical-align: 0.04em; border-radius: 999px; background: center / cover no-repeat url('https://picsum.photos/seed/inline-art/800/400'); filter: grayscale(0.2) contrast(1.15); }
.hero-description { max-width: 460px; margin-top: 34px; color: var(--muted); font-size: 17px; line-height: 1.75; }
.hero-actions { display: flex; flex-wrap: wrap; gap: 10px; margin-top: 42px; }
.hero-primary, .hero-secondary { height: 52px; padding-inline: 24px; border-radius: 999px; }
.hero-secondary { color: var(--paper) !important; border-color: rgba(241, 240, 233, 0.32) !important; background: transparent !important; }
.hero-media { position: relative; min-height: 620px; }
.hero-image { position: absolute; background-position: center; background-size: cover; filter: contrast(1.06) saturate(0.82); }
.hero-image-main { inset: 0 3% 8% 12%; background-image: url('https://picsum.photos/seed/monumental-light/1400/1800'); clip-path: polygon(14% 0, 100% 7%, 86% 100%, 0 91%); }
.hero-image-float { right: -2%; bottom: 3%; width: 37%; height: 37%; background-image: url('https://picsum.photos/seed/material-study/800/1000'); border: 10px solid var(--ink); clip-path: polygon(0 9%, 100% 0, 91% 100%, 8% 90%); }
.hero-caption { position: absolute; right: 2%; bottom: -1%; display: flex; justify-content: space-between; width: 39%; color: var(--muted); font-size: 10px; letter-spacing: 0.1em; text-transform: uppercase; }

.archive-section { padding: 170px 0 220px; }
.section-intro { display: flex; justify-content: space-between; gap: 40px; align-items: end; margin-bottom: 58px; }
.section-intro h2, .desire-copy h2 { max-width: 720px; font-size: clamp(2.5rem, 5vw, 5.2rem); line-height: 0.98; }
.archive-tools { display: flex; gap: 10px; width: min(460px, 100%); }
.archive-search { flex: 1; }
.archive-search :deep(.ant-input-group .ant-input), .archive-search :deep(.ant-input-search-button), .filter-button { height: 48px; border-color: rgba(241, 240, 233, 0.24); color: var(--paper); background: rgba(241, 240, 233, 0.06); }
.archive-search :deep(.ant-input) { color: var(--paper); background: rgba(241, 240, 233, 0.06); }
.archive-search :deep(.ant-input::placeholder) { color: rgba(241, 240, 233, 0.42); }
.archive-search :deep(.ant-input-search-button) { color: var(--ink); background: var(--acid); border-color: var(--acid); }
.filter-button { min-width: 86px; }
.filter-button span { margin-left: 8px; color: var(--acid); }

.marquee-band { position: relative; overflow: hidden; margin: 0 0 24px; border-block: 1px solid var(--line); }
.marquee-track { display: flex; width: max-content; padding: 16px 0; animation: marquee 26s linear infinite; }
.marquee-track span { display: inline-flex; align-items: center; gap: 32px; margin-right: 32px; color: rgba(241, 240, 233, 0.62); font-size: 13px; letter-spacing: 0.06em; white-space: nowrap; }
.marquee-track span::after { content: '·'; color: var(--acid); }
@keyframes marquee { to { transform: translateX(-50%); } }

.bento-grid { display: grid; grid-auto-flow: dense; grid-auto-rows: 180px; grid-template-columns: repeat(12, minmax(0, 1fr)); gap: 14px; }
.gallery-card { position: relative; min-height: 180px; overflow: hidden; cursor: pointer; border-radius: 4px; background: #242720; isolation: isolate; }
.gallery-card-1 { grid-column: span 6; grid-row: span 2; }
.gallery-card-2, .gallery-card-3 { grid-column: span 3; grid-row: span 1; }
.gallery-card-4, .gallery-card-5, .gallery-card-6 { grid-column: span 4; grid-row: span 1; }
.gallery-image, .gallery-shade { position: absolute; inset: 0; }
.gallery-image { background-position: center; background-size: cover; filter: grayscale(0.12) contrast(1.08); transition: transform 0.8s cubic-bezier(0.2, 0.75, 0.25, 1), filter 0.8s ease; }
.gallery-shade { z-index: 1; background: linear-gradient(180deg, transparent 35%, rgba(17, 19, 15, 0.86) 100%); }
.gallery-card:hover .gallery-image { transform: scale(1.06); filter: grayscale(0) contrast(1.08); }
.gallery-card-content { position: absolute; z-index: 2; right: 20px; bottom: 18px; left: 20px; }
.gallery-kind { color: var(--acid); font-size: 10px; letter-spacing: 0.12em; text-transform: uppercase; }
.gallery-card h3 { margin: 7px 0 0; color: var(--paper); font-size: clamp(1.15rem, 2vw, 1.8rem); font-weight: 500; letter-spacing: -0.04em; }
.gallery-arrow { position: absolute; right: 0; bottom: 3px; display: grid; place-items: center; width: 34px; height: 34px; color: var(--ink); background: var(--acid); border-radius: 50%; transform: rotate(45deg); }

.desire-section { display: grid; grid-template-columns: minmax(280px, 0.8fr) minmax(0, 1.2fr); gap: clamp(50px, 9vw, 180px); padding: 200px 0 230px; border-top: 1px solid var(--line); }
.desire-copy { align-self: start; max-width: 460px; }
.desire-copy h2 { font-size: clamp(2.5rem, 4.5vw, 4.8rem); }
.reveal-copy { max-width: 440px; margin: 44px 0 38px; color: var(--muted); font-size: 21px; line-height: 1.75; }
.reveal-word { display: inline; opacity: 0.2; }
.dark-button { height: 52px; padding-inline: 24px; border-radius: 999px; }
.accordion-stack { display: flex; flex-direction: column; gap: 12px; }
.accordion-item { position: relative; min-height: 178px; overflow: hidden; cursor: pointer; border: 1px solid var(--line); background: #1a1d17; transition: min-height 0.7s cubic-bezier(0.2, 0.75, 0.25, 1); }
.accordion-item:hover { min-height: 252px; }
.accordion-image, .accordion-overlay { position: absolute; inset: 0; }
.accordion-image { background-position: center; background-size: cover; opacity: 0.7; transition: transform 0.8s ease; }
.accordion-item:hover .accordion-image { transform: scale(1.06); }
.accordion-overlay { background: linear-gradient(90deg, rgba(17, 19, 15, 0.94), rgba(17, 19, 15, 0.24)); }
.accordion-content { position: relative; z-index: 1; padding: 28px; }
.accordion-content > span { color: var(--acid); font-size: 12px; font-weight: 700; }
.accordion-content h3 { margin: 22px 0 8px; color: var(--paper); font-size: 28px; font-weight: 500; letter-spacing: -0.05em; }
.accordion-content p { max-width: 340px; margin: 0; color: rgba(241, 240, 233, 0.72); line-height: 1.65; }
.accordion-arrow { position: absolute; z-index: 1; right: 28px; bottom: 28px; color: var(--acid); font-size: 20px; }

.feedback-section { display: grid; grid-template-columns: minmax(280px, 0.72fr) minmax(0, 1.28fr); min-height: 640px; border-top: 1px solid var(--line); border-bottom: 1px solid var(--line); }
.feedback-image { min-height: 520px; background-position: center; background-size: cover; filter: grayscale(0.35) contrast(1.08); }
.feedback-content { display: flex; flex-direction: column; justify-content: center; padding: 80px clamp(32px, 8vw, 140px); }
.feedback-content blockquote { max-width: 720px; margin: 0; color: var(--paper); font-size: clamp(2.1rem, 4.2vw, 4.6rem); font-weight: 500; letter-spacing: -0.07em; line-height: 1.02; }
.feedback-person { display: flex; align-items: end; justify-content: space-between; max-width: 720px; margin-top: 70px; }
.feedback-person strong, .feedback-person span { display: block; }
.feedback-person strong { color: var(--paper); font-size: 15px; }
.feedback-person span { margin-top: 7px; color: var(--muted); font-size: 12px; }
.feedback-controls { display: flex; gap: 8px; }
.feedback-controls button { display: grid; place-items: center; width: 44px; height: 44px; color: var(--paper); border: 1px solid var(--line); background: transparent; cursor: pointer; transition: color 0.3s ease, background 0.3s ease; }
.feedback-controls button:hover { color: var(--ink); background: var(--acid); }

.action-section { display: flex; align-items: end; justify-content: space-between; gap: 40px; padding: 220px 0 180px; }
.action-section h2 { font-size: clamp(3rem, 6vw, 6.2rem); line-height: 0.96; }
.action-button { flex: 0 0 auto; height: 58px; padding-inline: 28px; border-radius: 999px; }
.prototype-footer { display: flex; justify-content: space-between; gap: 24px; padding: 22px 0 34px; color: rgba(241, 240, 233, 0.48); border-top: 1px solid var(--line); font-size: 11px; letter-spacing: 0.08em; }

@media (max-width: 900px) {
  .nav-links { display: none; }
  .hero-section, .desire-section, .feedback-section { grid-template-columns: 1fr; }
  .hero-section { min-height: auto; padding-top: 120px; }
  .hero-media { min-height: 580px; }
  .desire-copy { position: static !important; }
  .feedback-image { min-height: 380px; }
}

@media (max-width: 620px) {
  .prototype-nav, .hero-section, .archive-section, .desire-section, .feedback-section, .action-section, .prototype-footer { width: min(100% - 28px, 1360px); }
  .prototype-nav { top: 10px; margin-top: 10px; }
  .brand-name, .nav-login { display: none; }
  .hero-section { gap: 50px; padding: 110px 0 120px; }
  .hero-copy h1 { font-size: clamp(3rem, 14vw, 5rem); }
  .hero-description { font-size: 15px; }
  .hero-media { min-height: 470px; }
  .archive-section, .desire-section { padding-block: 120px; }
  .section-intro, .action-section, .prototype-footer { display: block; }
  .archive-tools { width: 100%; margin-top: 34px; }
  .bento-grid { grid-auto-rows: 150px; gap: 8px; }
  .gallery-card-1 { grid-column: span 12; grid-row: span 2; }
  .gallery-card-2, .gallery-card-3, .gallery-card-4, .gallery-card-5, .gallery-card-6 { grid-column: span 6; }
  .action-button { margin-top: 34px; }
  .prototype-footer span { display: block; margin-top: 12px; }
}
</style>
