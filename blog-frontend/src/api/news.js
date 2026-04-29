import request from './request'

/**
 * 分页查询新闻列表。
 */
export const getNews = (params = {}) => request.get('/news', { params })

/**
 * 查询新闻详情。
 */
export const getNewsDetail = id => request.get(`/news/${id}`)

/**
 * 查询新闻分类统计。
 */
export const getNewsCategories = params => request.get('/news/categories', { params })

/**
 * 查询指定日期的 Top 新闻。
 */
export const getTopNews = params => request.get('/news/top', { params })

/**
 * 手动触发新闻抓取。
 */
export const fetchNewsNow = params => request.post('/admin/news/fetch', null, { params })

/**
 * 查询新闻抓取任务日志。
 */
export const getNewsJobLogs = (page = 1, size = 10) =>
  request.get('/admin/news/jobs', { params: { page, size } })

/**
 * 查询分类规则。
 */
export const getNewsRules = () => request.get('/admin/news/rules')

/**
 * 更新分类规则。
 */
export const updateNewsRule = (id, data) => request.put(`/admin/news/rules/${id}`, data)
