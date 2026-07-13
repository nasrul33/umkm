import { createRouter, createWebHistory } from "vue-router";
import { useAuthStore } from "../stores/auth";

/**
 * SRS-A-05: pemisahan tegas antara halaman publik (Modul A, tidak butuh login)
 * dan aplikasi akuntansi (Modul B1-B6, wajib login — di dalam AppShell).
 *
 * PENTING: path halaman SPA TIDAK BOLEH berawalan /app — prefix itu milik
 * REST API backend (SecurityConfig + proxy Vite/Nginx). Kalau sama, full-page
 * load halaman ikut terlempar ke backend dan dibalas 401.
 *
 * meta.requiresRole hanya UX-gating; sumber kebenaran otorisasi = backend (403).
 */
const routes = [
  { path: "/", name: "public-landing", component: () => import("../views/public/ProfileLanding.vue") },
  { path: "/login", name: "login", component: () => import("../views/app/Login.vue") },
  {
    path: "/",
    component: () => import("../layouts/AppShell.vue"),
    meta: { requiresAuth: true },
    children: [
      {
        path: "dashboard",
        name: "app-dashboard",
        component: () => import("../views/app/Dashboard.vue")
      },
      {
        path: "transaksi/baru",
        name: "transaction-wizard",
        component: () => import("../views/app/TransactionWizard.vue")
      },
      {
        path: "laporan",
        name: "app-reports",
        component: () => import("../views/app/Reports.vue"),
        meta: { requiresRole: "OWNER" }
      },
      {
        path: "pajak",
        name: "app-tax",
        component: () => import("../views/app/Tax.vue"),
        meta: { requiresRole: "OWNER" }
      }
    ]
  }
];

const router = createRouter({
  history: createWebHistory(),
  routes
});

router.beforeEach((to) => {
  const auth = useAuthStore();
  if (to.matched.some((r) => r.meta.requiresAuth) && !auth.isAuthenticated) {
    return { name: "login" };
  }
  if (to.meta.requiresRole && auth.role !== to.meta.requiresRole) {
    return { name: "app-dashboard" };
  }
});

export default router;
