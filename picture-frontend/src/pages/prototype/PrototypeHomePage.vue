<template>
  <div class="prototype-home">
    <section class="proto-hero">
      <div class="proto-hero-copy">
        <span class="proto-eyebrow">智能云图库 / 功能原型</span>
        <h1 class="proto-title">让每一张图片，都有一条清晰的工作路径。</h1>
        <p class="proto-copy">这一版不是重新发明业务，而是把现有图库项目已经具备的浏览、上传、审核、空间和用户管理能力，整理成一套可以继续接入真实接口的视觉工作台。</p>
        <div class="proto-hero-actions">
          <a-button class="proto-button acid-button" type="primary" @click="router.push('/prototype/gallery')">进入公共图库</a-button>
          <a-button class="proto-button ghost-button" @click="router.push('/prototype/gallery/manage')">查看图片管理</a-button>
        </div>
      </div>
      <div class="proto-hero-media">
        <div class="hero-note hero-note-top">pictureCheck / 0 → 1</div>
        <div class="hero-note hero-note-bottom">spaceId / 27</div>
        <img data-hero-image src="https://picsum.photos/seed/editorial-archive/1200/900" alt="图库原型视觉封面" />
        <div class="hero-media-overlay"></div>
      </div>
    </section>

    <section class="proto-section">
      <div class="proto-section-heading">
        <div>
          <span class="proto-eyebrow">当前业务地图</span>
          <h2 class="proto-subtitle">从发现，到收纳，再到审核。</h2>
        </div>
        <p class="proto-copy">每个入口都对应现有接口，页面先用静态数据演示状态，最终接入时不改变业务边界。</p>
      </div>

      <div class="proto-bento home-bento">
        <RouterLink to="/prototype/gallery" class="proto-bento-card dark span-2 bento-link">
          <h3>公共图库</h3>
          <p>按 searchText、category、tags 查询审核通过的图片，浏览缩略图并进入详情。</p>
          <span class="bento-corner">queryPicturePageCache</span>
        </RouterLink>
        <RouterLink to="/prototype/gallery/upload" class="proto-bento-card acid bento-link">
          <h3>上传图片</h3>
          <p>文件或 URL，补充名称、分类、标签、简介和空间。</p>
          <span class="bento-corner">uploadPic</span>
        </RouterLink>
        <RouterLink to="/prototype/space" class="proto-bento-card orange bento-link">
          <h3>个人空间</h3>
          <p>实时呈现 usedSize / maxSize 与 usedCount / maxCount。</p>
          <span class="bento-corner">querySpaceById</span>
        </RouterLink>
        <RouterLink to="/prototype/admin/pictures/review" class="proto-bento-card blue span-2 bento-link">
          <h3>图片审核工作台</h3>
          <p>按 pictureCheck 筛选，支持单张和批量通过、拒绝以及审核原因记录。</p>
          <span class="bento-corner">adminCheckPictureBatch</span>
        </RouterLink>
      </div>
    </section>

    <section class="proto-section proto-marquee-section">
      <div class="proto-marquee" aria-label="当前原型覆盖模块">
        <span>PUBLIC GALLERY</span><b>×</b><span>UPLOAD</span><b>×</b><span>PERSONAL SPACE</span><b>×</b><span>REVIEW QUEUE</span><b>×</b><span>USER CENTER</span><b>×</b>
        <span>PUBLIC GALLERY</span><b>×</b><span>UPLOAD</span><b>×</b><span>PERSONAL SPACE</span><b>×</b><span>REVIEW QUEUE</span><b>×</b>
      </div>
    </section>

    <section class="proto-section home-experience">
      <div class="home-experience-copy">
        <span class="proto-eyebrow">工作流预览</span>
        <h2 class="proto-subtitle">同一张图片，在不同角色眼里有不同状态。</h2>
        <p class="proto-copy">普通用户看到自己的上传记录与审核结果，管理员看到待审核队列、审核人和拒绝原因。下面的卡片是原型中真实会反复出现的状态结构。</p>
        <a-button class="proto-button proto-button-primary" type="primary" @click="router.push('/prototype/admin/pictures/review')">打开审核队列</a-button>
      </div>
      <div class="stack-stage" ref="stackStage">
        <article v-for="(card, index) in stackCards" :key="card.title" class="stack-card" :style="{ zIndex: stackCards.length - index }">
          <div class="stack-card-index">0{{ index + 1 }}</div>
          <div class="stack-card-image proto-image-wrap"><img :src="card.image" :alt="card.title" /></div>
          <div class="stack-card-copy">
            <div>
              <strong>{{ card.title }}</strong>
              <p>{{ card.description }}</p>
            </div>
            <a-tag class="proto-status" :class="card.statusClass">{{ card.status }}</a-tag>
          </div>
        </article>
      </div>
    </section>

    <section class="proto-section home-accordion-section">
      <div class="proto-section-heading">
        <div>
          <span class="proto-eyebrow">接口边界</span>
          <h2 class="proto-subtitle">原型不替后端发明新事实。</h2>
        </div>
      </div>
      <div class="horizontal-accordion">
        <article v-for="item in accordionItems" :key="item.title" class="accordion-item">
          <span class="accordion-number">{{ item.number }}</span>
          <div class="accordion-content"><strong>{{ item.title }}</strong><p>{{ item.description }}</p></div>
          <span class="accordion-arrow">↗</span>
        </article>
      </div>
    </section>

    <section class="proto-section home-quote-section">
      <a-carousel autoplay arrows class="quote-carousel">
        <div v-for="quote in quotes" :key="quote.name" class="quote-slide">
          <span class="quote-mark">“</span>
          <p>{{ quote.text }}</p>
          <div class="quote-author"><img :src="quote.avatar" :alt="quote.name" /><span><strong>{{ quote.name }}</strong><small>{{ quote.role }}</small></span></div>
        </div>
      </a-carousel>
    </section>

    <section class="proto-section home-action">
      <div>
        <span class="proto-eyebrow">准备开始</span>
        <h2>从一个真实的接口，长出一整个清晰的页面。</h2>
      </div>
      <a-button class="proto-button acid-button" type="primary" @click="router.push('/prototype/user/login')">从登录开始浏览</a-button>
    </section>
  </div>
</template>

<script setup lang="ts">
import { onMounted, onBeforeUnmount, ref } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()
const stackStage = ref<HTMLElement | null>(null)

const stackCards = [
  { title: 'Blue architecture', description: '新上传图片进入审核队列，普通用户默认是待审核。', status: '待审核', statusClass: 'wait', image: 'https://picsum.photos/seed/blue-architecture/720/480' },
  { title: 'Quiet forest', description: '审核通过后进入公共图库，可被搜索、分类和标签筛选。', status: '审核通过', statusClass: 'pass', image: 'https://picsum.photos/seed/quiet-forest/720/480' },
  { title: 'Red object', description: '审核拒绝会留下 checkMessage，用户可在图片管理中查看。', status: '审核拒绝', statusClass: 'refuse', image: 'https://picsum.photos/seed/red-object/720/480' },
]

const accordionItems = [
  { number: 'A', title: '字段先行', description: '页面状态沿用 PictureVO、User、SpaceVO 的真实字段。' },
  { number: 'B', title: '权限清楚', description: '管理员操作与普通用户操作在导航和按钮层面分开。' },
  { number: 'C', title: '接口可替换', description: '静态数据结构与生成 API 返回结构保持一致。' },
]

const quotes = [
  { name: '林默', role: '普通用户 / 图片上传者', text: '我不需要猜图片现在在哪里，上传、审核、拒绝原因都应该在同一个工作流里被看见。', avatar: 'https://i.pravatar.cc/100?img=12' },
  { name: '管理员', role: '平台运营', text: '审核页不是另一个图库，它应该优先展示状态、原因和批量决策。', avatar: 'https://i.pravatar.cc/100?img=68' },
]

let gsapContext: any

onMounted(() => {
  const gsapApi = (window as any).gsap
  const scrollTrigger = (window as any).ScrollTrigger
  if (!gsapApi) return
  if (scrollTrigger) gsapApi.registerPlugin(scrollTrigger)
  gsapContext = gsapApi.context(() => {
    gsapApi.fromTo('[data-hero-image]', { scale: .86, opacity: .25 }, { scale: 1, opacity: 1, duration: 1.4, ease: 'power3.out' })
    if (stackStage.value && scrollTrigger) {
      gsapApi.to('.stack-card', {
        y: (index: number) => index * -54,
        rotate: (index: number) => index === 1 ? -4 : index === 2 ? 4 : 0,
        scale: (index: number) => 1 - index * .035,
        scrollTrigger: { trigger: stackStage.value, start: 'top 75%', end: 'bottom 30%', scrub: true },
      })
    }
  })
})

onBeforeUnmount(() => gsapContext?.revert?.())
</script>

<style scoped>
.proto-hero { min-height: 500px; padding-top: clamp(36px, 6vw, 78px); display: grid; grid-template-columns: minmax(0, 1fr) minmax(330px, .8fr); gap: clamp(28px, 5vw, 86px); align-items: center; }
.proto-hero-copy { position: relative; z-index: 2; }
.proto-hero .proto-title { font-size: clamp(48px, 6.9vw, 104px); max-width: 770px; }
.proto-hero-actions { display: flex; gap: 10px; margin-top: 29px; flex-wrap: wrap; }
.proto-hero-media { min-height: 410px; position: relative; transform: rotate(3deg); }
.proto-hero-media img { width: 100%; height: 100%; min-height: 410px; object-fit: cover; filter: grayscale(.35) contrast(1.1); }
.hero-media-overlay { position: absolute; inset: 0; background: linear-gradient(135deg, rgba(17,20,22,.1), rgba(17,20,22,.5)); pointer-events: none; }
.hero-note { position: absolute; z-index: 2; padding: 9px 12px; background: var(--proto-acid); color: var(--proto-ink); font-family: 'DM Mono', monospace; font-size: 10px; box-shadow: 8px 8px 0 rgba(17,20,22,.7); }
.hero-note-top { top: 32px; left: -32px; transform: rotate(-8deg); }
.hero-note-bottom { right: -25px; bottom: 36px; transform: rotate(7deg); background: var(--proto-orange); }
.proto-section-heading { display: flex; align-items: flex-end; justify-content: space-between; gap: 20px; margin-bottom: 18px; }
.proto-section-heading .proto-copy { margin: 0 0 3px; }
.bento-link { display: block; color: inherit; text-decoration: none; }
.bento-link:hover { color: inherit; }
.proto-marquee-section { padding-top: 38px; overflow: hidden; }
.proto-marquee { display: flex; align-items: center; gap: 18px; min-width: max-content; animation: proto-marquee 28s linear infinite; color: var(--proto-muted); font-family: 'DM Mono', monospace; font-size: 11px; letter-spacing: .06em; }
.proto-marquee b { color: var(--proto-orange); font-size: 18px; font-weight: 400; }
@keyframes proto-marquee { to { transform: translateX(-45%); } }
.home-experience { display: grid; grid-template-columns: minmax(230px, .7fr) minmax(380px, 1fr); gap: clamp(24px, 6vw, 90px); align-items: start; }
.home-experience-copy { position: sticky; top: 110px; }
.home-experience-copy .proto-subtitle { margin: 13px 0 17px; }
.home-experience-copy .proto-button { margin-top: 25px; }
.stack-stage { min-height: 470px; position: relative; }
.stack-card { width: min(100%, 470px); padding: 11px; position: absolute; inset: 0 auto auto 50%; transform: translateX(-50%); background: #fff; border: 1px solid var(--proto-line); box-shadow: var(--proto-shadow); }
.stack-card:nth-child(1) { top: 0; }
.stack-card:nth-child(2) { top: 33px; }
.stack-card:nth-child(3) { top: 66px; }
.stack-card-index { position: absolute; top: 22px; left: 23px; z-index: 2; color: white; font-family: 'DM Mono', monospace; font-size: 10px; }
.stack-card-image { height: 330px; }
.stack-card-copy { display: flex; align-items: flex-start; justify-content: space-between; gap: 15px; padding: 16px 6px 5px; }
.stack-card-copy strong { font-size: 18px; letter-spacing: -.04em; }
.stack-card-copy p { max-width: 290px; margin: 7px 0 0; color: var(--proto-muted); font-size: 11px; line-height: 1.6; }
.home-accordion-section { padding-bottom: 42px; }
.horizontal-accordion { display: flex; gap: 7px; min-height: 205px; }
.accordion-item { flex: 1; min-width: 0; padding: 24px 19px; display: flex; flex-direction: column; justify-content: space-between; background: var(--proto-ink); color: var(--proto-paper); overflow: hidden; transition: flex .45s cubic-bezier(.2,.7,.2,1), background .45s ease; }
.accordion-item:hover { flex: 2.15; background: var(--proto-orange); color: var(--proto-ink); }
.accordion-number { font-family: 'DM Mono', monospace; font-size: 11px; opacity: .5; }
.accordion-content strong { display: block; font-size: 21px; letter-spacing: -.05em; white-space: nowrap; }
.accordion-content p { max-width: 265px; margin: 11px 0 0; font-size: 12px; line-height: 1.7; opacity: .68; }
.accordion-arrow { align-self: flex-end; font-size: 24px; }
.home-quote-section { padding-bottom: 42px; }
.quote-carousel { max-width: 760px; min-height: 240px; margin: 0 auto; background: var(--proto-acid); }
.quote-carousel :deep(.slick-slide) { min-height: 240px; }
.quote-slide { min-height: 240px; padding: 32px 42px 28px; }
.quote-mark { display: block; font-size: 60px; line-height: .6; }
.quote-slide p { max-width: 610px; margin: 16px 0 20px; font-size: clamp(20px, 3vw, 32px); line-height: 1.18; letter-spacing: -.05em; }
.quote-author { display: flex; align-items: center; gap: 10px; }
.quote-author img { width: 35px; height: 35px; border-radius: 50%; object-fit: cover; }
.quote-author strong, .quote-author small { display: block; }
.quote-author strong { font-size: 12px; }
.quote-author small { margin-top: 2px; font-size: 10px; opacity: .62; }
.home-action { min-height: 190px; padding: 30px; display: flex; align-items: flex-end; justify-content: space-between; gap: 25px; background: var(--proto-ink); color: var(--proto-paper); }
.home-action h2 { max-width: 680px; margin: 15px 0 0; font-size: clamp(27px, 4vw, 53px); line-height: .98; letter-spacing: -.07em; }
.home-action .proto-button { flex: 0 0 auto; }

@media (max-width: 800px) {
  .proto-hero { grid-template-columns: 1fr; padding-top: 42px; }
  .proto-hero-media { min-height: 380px; margin: 15px 16px 0 0; }
  .proto-hero-media img { min-height: 380px; }
  .home-experience { grid-template-columns: 1fr; }
  .home-experience-copy { position: static; }
  .stack-stage { min-height: 470px; }
  .stack-card-image { height: 290px; }
  .home-action { align-items: flex-start; flex-direction: column; padding: 29px; }
}

@media (max-width: 520px) {
  .proto-section-heading { align-items: flex-start; flex-direction: column; }
  .horizontal-accordion { min-height: 0; flex-direction: column; }
  .accordion-item { min-height: 120px; }
  .accordion-item:hover { flex: 1; }
  .quote-slide { padding: 32px 25px; }
}
</style>
