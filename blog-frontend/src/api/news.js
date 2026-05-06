import request from './request'

/**
 * 分页查询新闻列表。
 *
 * @param {object} params 查询参数
 * @returns {Promise<any>}
 */
export const getNews = (params = {}) => request.get('/news', { params })

/**
 * 查询新闻详情。
 *
 * @param {number|string} id 新闻主键
 * @returns {Promise<any>}
 */
export const getNewsDetail = id => request.get(`/news/${id}`)

/**
 * 查询新闻分类统计。
 *
 * @param {object} params 查询参数
 * @returns {Promise<any>}
 */
export const getNewsCategories = params => request.get('/news/categories', { params })

/**
 * 查询指定日期的 Top 新闻。
 *
 * @param {object} params 查询参数
 * @returns {Promise<any>}
 */
export const getTopNews = params => request.get('/news/top', { params })

/**
 * 手动触发新闻抓取。
 *
 * @param {object} params 查询参数
 * @returns {Promise<any>}
 */
export const fetchNewsNow = params => request.post('/admin/news/fetch', null, { params })

/**
 * 查询新闻抓取任务日志。
 *
 * @param {number} page 页码
 * @param {number} size 每页数量
 * @returns {Promise<any>}
 */
export const getNewsJobLogs = (page = 1, size = 10) =>
  request.get('/admin/news/jobs', { params: { page, size } })

/**
 * 查询新闻数据源状态。
 *
 * @returns {Promise<any>}
 */
export const getNewsProviders = () => request.get('/admin/news/providers')

/**
 * 查询重复新闻结果。
 *
 * @param {number} page 页码
 * @param {number} size 每页数量
 * @param {object} params 额外参数
 * @returns {Promise<any>}
 */
export const getNewsDuplicates = (page = 1, size = 10, params = {}) =>
  request.get('/admin/news/duplicates', { params: { page, size, ...params } })

/**
 * 查询分类规则。
 *
 * @returns {Promise<any>}
 */
export const getNewsRules = () => request.get('/admin/news/rules')

/**
 * 更新分类规则。
 *
 * @param {number|string} id 规则主键
 * @param {object} data 提交数据
 * @returns {Promise<any>}
 */
export const updateNewsRule = (id, data) => request.put(`/admin/news/rules/${id}`, data)

/**
 * 生成指定日期前十条热度新闻摘要。
 *
 * @param {object} params 查询参数
 * @returns {Promise<any>}
 */
export const generateTopNewsSummaries = params =>
  request.post('/admin/news/summary/generate/top', null, { params })

/**
 * 生成单条新闻摘要。
 *
 * @param {number|string} id 新闻主键
 * @param {object} params 查询参数
 * @returns {Promise<any>}
 */
export const generateNewsSummary = (id, params = {}) =>
  request.post(`/admin/news/${id}/summary`, null, { params })

/**
 * 查询新闻摘要额度。
 *
 * @param {object} params 查询参数
 * @returns {Promise<any>}
 */
export const getNewsSummaryQuota = params =>
  request.get('/admin/news/summary/quota', { params })
