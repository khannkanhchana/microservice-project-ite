import { Navigate, Route, Routes } from 'react-router-dom'
import { AuthProvider } from './auth/AuthContext'
import RequireAuth from './auth/RequireAuth'
import AccountLayout from './components/AccountLayout'
import { ToastProvider } from './components/ui'
import Callback from './pages/Callback'
import ForgotPassword from './pages/ForgotPassword'
import Landing from './pages/Landing'
import NotFound from './pages/NotFound'
import ResetPassword from './pages/ResetPassword'
import Activity from './pages/account/Activity'
import Deactivate from './pages/account/Deactivate'
import LinkedAccounts from './pages/account/LinkedAccounts'
import LoginHistory from './pages/account/LoginHistory'
import Profile from './pages/account/Profile'
import Security from './pages/account/Security'
import Sessions from './pages/account/Sessions'
import RequirePermission from './auth/RequirePermission'
import AdminOverview from './pages/admin/Overview'
import AdminUsers from './pages/admin/Users'
import AdminUserDetail from './pages/admin/UserDetail'
import AdminRoles from './pages/admin/Roles'
import AdminPermissions from './pages/admin/Permissions'
import AdminClients from './pages/admin/Clients'
import AdminLoginMonitor from './pages/admin/LoginMonitor'
import AdminAuditLogs from './pages/admin/AuditLogs'
import AdminSettings from './pages/admin/Settings'

const guard = (permission, element) => <RequirePermission permission={permission}>{element}</RequirePermission>

export default function App() {
  return (
    <ToastProvider>
      <AuthProvider>
        <Routes>
          <Route path="/" element={<Landing />} />
          <Route path="/callback" element={<Callback />} />
          <Route path="/forgot-password" element={<ForgotPassword />} />
          <Route path="/reset-password" element={<ResetPassword />} />
          <Route
            path="/account"
            element={
              <RequireAuth>
                <AccountLayout />
              </RequireAuth>
            }
          >
            <Route index element={<Navigate to="profile" replace />} />
            <Route path="profile" element={<Profile />} />
            <Route path="security" element={<Security />} />
            <Route path="linked-accounts" element={<LinkedAccounts />} />
            <Route path="sessions" element={<Sessions />} />
            <Route path="login-history" element={<LoginHistory />} />
            <Route path="activity" element={<Activity />} />
            <Route path="deactivate" element={<Deactivate />} />
          </Route>
          <Route
            path="/admin"
            element={
              <RequireAuth>
                <AccountLayout />
              </RequireAuth>
            }
          >
            <Route index element={guard('dashboard:read', <AdminOverview />)} />
            <Route path="users" element={guard('user:read', <AdminUsers />)} />
            <Route path="users/:uuid" element={guard('user:read', <AdminUserDetail />)} />
            <Route path="roles" element={guard('role:read', <AdminRoles />)} />
            <Route path="permissions" element={guard('permission:read', <AdminPermissions />)} />
            <Route path="clients" element={guard('client:read', <AdminClients />)} />
            <Route path="login-monitor" element={guard('session:read', <AdminLoginMonitor />)} />
            <Route path="audit-logs" element={guard('audit:read', <AdminAuditLogs />)} />
            <Route path="settings" element={guard('settings:read', <AdminSettings />)} />
          </Route>
          <Route path="*" element={<NotFound />} />
        </Routes>
      </AuthProvider>
    </ToastProvider>
  )
}
