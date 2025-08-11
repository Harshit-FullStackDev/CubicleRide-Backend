import React from "react";
import { Navigate } from "react-router-dom";

// Function to check if token is expired
const isTokenExpired = (token) => {
    try {
        const payload = JSON.parse(atob(token.split(".")[1]));
        const now = Math.floor(Date.now() / 1000);
        return payload.exp < now;
    } catch {
        return true; // Invalid token format
    }
};

const ProtectedRoute = ({ children, role }) => {
    const token = localStorage.getItem("token");
    const userRole = localStorage.getItem("role");

    if (!token || !userRole || userRole !== role || isTokenExpired(token)) {
        // ✅ Clear expired or invalid token
        localStorage.removeItem("token");
        localStorage.removeItem("role");
        return <Navigate to="/login" />;
    }

    return children;
};

export default ProtectedRoute;
