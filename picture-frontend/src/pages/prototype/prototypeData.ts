export type PictureCheck = 0 | 1 | 2

export interface PrototypePicture {
  id: number
  url: string
  thumbnailUrl: string
  name: string
  introduction: string
  category: string
  tags: string[]
  picsize: number
  picwidth: number
  picheight: number
  picformat: string
  pictureCheck: PictureCheck
  checkMessage?: string
  spaceId: number
  userId: number
  createdUser: { id: number; username: string; avatarurl: string }
  createtime: string
}

export const prototypePictures: PrototypePicture[] = [
  {
    id: 101,
    url: 'https://picsum.photos/seed/quiet-forest/1600/1100',
    thumbnailUrl: 'https://picsum.photos/seed/quiet-forest/720/480',
    name: 'Quiet forest',
    introduction: '一组低饱和度森林影像，适合做空间和自然主题的视觉素材。',
    category: '风景',
    tags: ['森林', '自然', '静谧'],
    picsize: 1843200,
    picwidth: 1600,
    picheight: 1100,
    picformat: 'JPG',
    pictureCheck: 1,
    spaceId: 0,
    userId: 12,
    createdUser: { id: 12, username: '林默', avatarurl: 'https://i.pravatar.cc/80?img=12' },
    createtime: '2026-07-15 09:24:00',
  },
  {
    id: 102,
    url: 'https://picsum.photos/seed/blue-architecture/1600/1100',
    thumbnailUrl: 'https://picsum.photos/seed/blue-architecture/720/480',
    name: 'Blue architecture',
    introduction: '城市建筑的几何构成，强调结构、色块与光线。',
    category: '建筑',
    tags: ['建筑', '几何', '城市'],
    picsize: 2365400,
    picwidth: 1600,
    picheight: 1100,
    picformat: 'PNG',
    pictureCheck: 0,
    spaceId: 27,
    userId: 18,
    createdUser: { id: 18, username: '周野', avatarurl: 'https://i.pravatar.cc/80?img=18' },
    createtime: '2026-07-14 16:08:00',
  },
  {
    id: 103,
    url: 'https://picsum.photos/seed/soft-still-life/1600/1100',
    thumbnailUrl: 'https://picsum.photos/seed/soft-still-life/720/480',
    name: 'Soft still life',
    introduction: '日常物件的静物记录，采用暖色和留白构图。',
    category: '生活',
    tags: ['静物', '生活', '暖色'],
    picsize: 1126400,
    picwidth: 1600,
    picheight: 1100,
    picformat: 'JPG',
    pictureCheck: 1,
    spaceId: 0,
    userId: 7,
    createdUser: { id: 7, username: '苏禾', avatarurl: 'https://i.pravatar.cc/80?img=7' },
    createtime: '2026-07-13 11:42:00',
  },
  {
    id: 104,
    url: 'https://picsum.photos/seed/red-object/1600/1100',
    thumbnailUrl: 'https://picsum.photos/seed/red-object/720/480',
    name: 'Red object',
    introduction: '高对比度的红色物件，测试公共图库的视觉焦点。',
    category: '抽象',
    tags: ['抽象', '红色', '实验'],
    picsize: 3289600,
    picwidth: 1600,
    picheight: 1100,
    picformat: 'WEBP',
    pictureCheck: 2,
    checkMessage: '请补充图片来源说明。',
    spaceId: 27,
    userId: 18,
    createdUser: { id: 18, username: '周野', avatarurl: 'https://i.pravatar.cc/80?img=18' },
    createtime: '2026-07-11 21:10:00',
  },
  {
    id: 105,
    url: 'https://picsum.photos/seed/night-road/1600/1100',
    thumbnailUrl: 'https://picsum.photos/seed/night-road/720/480',
    name: 'Night road',
    introduction: '夜间道路和灯光留下的长曝光轨迹。',
    category: '城市',
    tags: ['夜景', '道路', '光线'],
    picsize: 1572864,
    picwidth: 1600,
    picheight: 1100,
    picformat: 'JPG',
    pictureCheck: 1,
    spaceId: 0,
    userId: 21,
    createdUser: { id: 21, username: '顾言', avatarurl: 'https://i.pravatar.cc/80?img=21' },
    createtime: '2026-07-10 19:36:00',
  },
  {
    id: 106,
    url: 'https://picsum.photos/seed/paper-texture/1600/1100',
    thumbnailUrl: 'https://picsum.photos/seed/paper-texture/720/480',
    name: 'Paper texture',
    introduction: '适合做背景的纸张纹理样本。',
    category: '纹理',
    tags: ['纹理', '纸张', '背景'],
    picsize: 942080,
    picwidth: 1600,
    picheight: 1100,
    picformat: 'JPG',
    pictureCheck: 0,
    spaceId: 28,
    userId: 12,
    createdUser: { id: 12, username: '林默', avatarurl: 'https://i.pravatar.cc/80?img=12' },
    createtime: '2026-07-09 14:12:00',
  },
]

export const prototypeUsers = [
  { id: 1, username: '管理员', useraccount: 'admin', gender: 0, phone: '138****0001', email: 'admin@cloudpic.local', userstatus: 'admin', profile: '负责用户、图片审核和空间运营。', createtime: '2026-05-01 09:00:00', avatarurl: 'https://i.pravatar.cc/80?img=68', spaceId: 0 },
  { id: 12, username: '林默', useraccount: 'linmo', gender: 1, phone: '139****1024', email: 'linmo@cloudpic.local', userstatus: 'user', profile: '记录自然、材质和城市光线。', createtime: '2026-05-14 11:24:00', avatarurl: 'https://i.pravatar.cc/80?img=12', spaceId: 28 },
  { id: 18, username: '周野', useraccount: 'zhouye', gender: 0, phone: '186****2077', email: 'zhouye@cloudpic.local', userstatus: 'user', profile: '关注建筑、几何与公共空间。', createtime: '2026-06-02 16:08:00', avatarurl: 'https://i.pravatar.cc/80?img=18', spaceId: 27 },
  { id: 21, username: '顾言', useraccount: 'guyan', gender: 0, phone: '158****5511', email: 'guyan@cloudpic.local', userstatus: 'vip', profile: '城市摄影与视觉研究。', createtime: '2026-06-20 08:45:00', avatarurl: 'https://i.pravatar.cc/80?img=21', spaceId: 0 },
]

export const prototypeCategories = ['全部', '风景', '建筑', '生活', '抽象', '城市', '纹理']
export const prototypeTags = ['森林', '自然', '建筑', '几何', '城市', '静物', '抽象', '夜景', '纹理']

export const prototypeSpaces = [
  { id: 27, spaceName: '周野的私人空间', spaceLevel: 1, maxSize: 5242880, usedSize: 3925400, maxCount: 100, usedCount: 42, userId: 18, createdUser: '周野', createTime: '2026-06-02 16:10:00', updateTime: '2026-07-14 16:08:00' },
  { id: 28, spaceName: '林默的私人空间', spaceLevel: 0, maxSize: 1048576, usedSize: 680400, maxCount: 50, usedCount: 16, userId: 12, createdUser: '林默', createTime: '2026-05-14 11:28:00', updateTime: '2026-07-09 14:12:00' },
]

export function pictureStatusText(status?: number) {
  return status === 1 ? '审核通过' : status === 2 ? '审核拒绝' : '待审核'
}

export function formatSize(bytes: number) {
  if (bytes >= 1024 * 1024) return `${(bytes / 1024 / 1024).toFixed(1)} MB`
  return `${Math.round(bytes / 1024)} KB`
}

export function formatSpaceLevel(level: number) {
  return level === 2 ? '专家空间' : level === 1 ? '专业空间' : '基础空间'
}
