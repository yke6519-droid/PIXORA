import { message } from "ant-design-vue";
import axios from "axios";

// 前后端分离部署时通过环境变量指定后端地址，本地开发保留原端口。
const apiBaseUrl = import.meta.env.VITE_API_BASE_URL || "http://localhost:8123";

const myAxios = axios.create({
  baseURL: apiBaseUrl,
  timeout: 60000,
  withCredentials: true,
});

// 请求拦截器
myAxios.interceptors.request.use(
  function (config) {
    return config;
  },
  function (error) {
    return Promise.reject(error);
  }
);

// 响应拦截器
myAxios.interceptors.response.use(
  function (response) {
    const data = response.data;
    if (data?.code === 40100) {
      const requestUrl = response.config?.url || "";
      const isCurrentUserRequest = requestUrl.includes("/user/getCurrentUser");
      const isPrototypeLogin = window.location.pathname.includes("/prototype/user/login");
      const isLegacyLogin = window.location.pathname.includes("/user/login");
      if (isCurrentUserRequest && !isPrototypeLogin && !isLegacyLogin) {
        message.warning("请先登录");
        const loginPath = window.location.pathname.startsWith("/prototype")
          ? "/prototype/user/login"
          : "/user/login";
        const redirect = `${window.location.pathname}${window.location.search}`;
        window.location.href = `${loginPath}?redirect=${encodeURIComponent(redirect)}`;
      }
    }
    // 返回整个 response，保持原有行为
    return response;
  },
  function (error) {
    return Promise.reject(error);
  }
);

export default myAxios;
