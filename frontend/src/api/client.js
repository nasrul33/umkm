import axios from "axios";
import { useAuthStore } from "../stores/auth";

const client = axios.create({ baseURL: "/" });

client.interceptors.request.use((config) => {
  const auth = useAuthStore();
  if (auth.token) {
    config.headers.Authorization = `Bearer ${auth.token}`;
  }
  return config;
});

// 401: sesi habis -> keluar; 403: tidak berhak -> kembali ke beranda.
// Redirect via location (bukan import router) utk menghindari siklus impor.
client.interceptors.response.use(
  (response) => response,
  (error) => {
    const status = error.response?.status;
    if (status === 401 && window.location.pathname !== "/login") {
      useAuthStore().logout();
      window.location.href = "/login";
    } else if (status === 403 && window.location.pathname !== "/dashboard") {
      window.location.href = "/dashboard";
    }
    return Promise.reject(error);
  }
);

export default client;
