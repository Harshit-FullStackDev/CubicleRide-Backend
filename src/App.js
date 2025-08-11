import React from "react";
import { Routes, Route, Navigate } from "react-router-dom";
import Register from "./pages/Register";
import Login from "./pages/Login";
import EmployeeDashboard from "./pages/employee/Dashboard";
import OfferRide from "./pages/employee/OfferRide";
import JoinRide from "./pages/employee/JoinRide";
import AdminDashboard from "./pages/admin/Dashboard";
import ViewEmployees from "./pages/admin/ViewEmployees";
import ViewRides from "./pages/admin/ViewRides";
import ProtectedRoute from "./components/ProtectedRoute";
import EditRide from "./pages/employee/EditRide";
import EditEmployee from "./pages/admin/EditEmployee";
import OtpVerify from "./components/OTPVerify";
import Notifications from "./pages/employee/Notifications";
import PublishedHistory from "./pages/employee/PublishedHistory";
import JoinedHistory from "./pages/employee/JoinedHistory";

function App() {
  return (
      <Routes>
        <Route path="/" element={<Navigate to="/login" />} />
        <Route path="/register" element={<Register />} />
        <Route path="/login" element={<Login />} />

        <Route
            path="/employee/dashboard"
            element={<ProtectedRoute role="EMPLOYEE"><EmployeeDashboard /></ProtectedRoute>}
        />
        <Route
            path="/employee/offer"
            element={<ProtectedRoute role="EMPLOYEE"><OfferRide /></ProtectedRoute>}
        />

        <Route
            path="/employee/join"
            element={<ProtectedRoute role="EMPLOYEE"><JoinRide /></ProtectedRoute>}
        />
        <Route
            path="/employee/notifications"
            element={<ProtectedRoute role="EMPLOYEE"><Notifications /></ProtectedRoute>}
        />
        <Route
            path="/employee/history/published"
            element={<ProtectedRoute role="EMPLOYEE"><PublishedHistory /></ProtectedRoute>}
        />
        <Route
            path="/employee/history/joined"
            element={<ProtectedRoute role="EMPLOYEE"><JoinedHistory /></ProtectedRoute>}
        />
          <Route path="/admin/employees/edit/:id" element={<EditEmployee />} />
        <Route
            path="/admin/dashboard"
            element={<ProtectedRoute role="ADMIN"><AdminDashboard /></ProtectedRoute>}
        />
        <Route
            path="/admin/employees"
            element={<ProtectedRoute role="ADMIN"><ViewEmployees /></ProtectedRoute>}
        />
        <Route
            path="/admin/rides"
            element={<ProtectedRoute role="ADMIN"><ViewRides /></ProtectedRoute>}
        />
            <Route path="/employee/edit/:id" element={<EditRide />} />
          <Route path="/otp-verify" element={<OtpVerify />} />
      </Routes>
  );
}

export default App;
