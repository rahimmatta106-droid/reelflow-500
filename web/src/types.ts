export type JobStatus =
  | 'DRAFT'
  | 'AI_GENERATING'
  | 'READY_FOR_REVIEW'
  | 'APPROVED'
  | 'REJECTED'
  | 'SCHEDULED'
  | 'PUBLISHING'
  | 'PUBLISHED'
  | 'FAILED'

export interface FacebookPage {
  id: string
  name: string
  pageId: string
  niche: string
  followers: number
  isEnabled: boolean
  tokenHealth: 'ACTIVE' | 'EXPIRING_SOON' | 'EXPIRED'
  hasToken: boolean
  masterPrompt: string
}

export interface VideoJob {
  id: string
  targetPageId: string
  targetPageName: string
  prompt: string
  veoModel: string
  status: JobStatus
  title: string
  caption: string
  hashtags: string[]
  thumbnailUrl: string | null
  scheduledDateTime: string | null
  scheduledTimezone: string
  postUrl: string | null
  createdAt: number
}

export type Tab =
  | 'dashboard'
  | 'library'
  | 'create'
  | 'review'
  | 'schedule'
  | 'pages'
  | 'analytics'
  | 'settings'
