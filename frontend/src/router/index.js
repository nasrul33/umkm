import { createRouter, createWebHistory } from "vue-router";
import { useAuthStore } from "../stores/auth";

/**
 * SRS-A-05: pemisahan tegas antara halaman publik (Modul A, tidak butuh login)
 * dan aplikasi akuntansi (Modul B1-B6, wajib login). Setiap route halaman
 * aplikasi HARUS punya meta.requiresAuth = true.
 *
 * PENTING: path halaman SPA TIDAK BOLEH berawalan /app — prefix itu milik
 * REST API backend (SecurityConfig + proxy Vite/Nginx). Kalau sama, full-page
 * load halaman ikut terlempar ke backend dan dibalas 401.
 */
const routes = [
  { path: "/", name: "public-landing", component: () => import("../views/public/ProfileLanding.vue") },
  { path: "/login", name: "login", component: () => import("../views/app/Login.vue") },
  {
    path: "/dashboard",
    name: "app-dashboard",
    component: () => import("../views/app/Dashboard.vue"),
    meta: { requiresAuth: true }
  },
  {
    path: "/transaksi/baru",
    name: "transaction-wizard",
    component: () => import("../views/app/TransactionWizard.vue"),
    meta: { requiresAuth: true }
  }
];

const router = createRouter({
  history: createWebHistory(),
  routes
});

router.beforeEach((to) => {
  const auth = useAuthStore();
  if (to.meta.requiresAuth && !auth.isAuthenticated) {
    return { name: "login" };
  }
});

export default router;
