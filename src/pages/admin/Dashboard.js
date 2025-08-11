import React, { useEffect, useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { FaUserShield, FaUsers, FaCarSide, FaSignOutAlt, FaEnvelope, FaUser, FaChevronRight } from "react-icons/fa";
import api from "../../api/axios";

function AdminDashboard() {
    const navigate = useNavigate();
    const [stats, setStats] = useState({ employees: 0, rides: 0 });
    const [recentEmployees, setRecentEmployees] = useState([]);
    const [recentRides, setRecentRides] = useState([]);
    const [loading, setLoading] = useState(true);

    const adminName = localStorage.getItem("name") || "Admin";
    const adminEmail = localStorage.getItem("email") || "admin@company.com";
    const lastLogin = localStorage.getItem("lastLogin") || new Date().toLocaleString();

    useEffect(() => {
        const fetchStats = async () => {
            try {
                const [empRes, rideRes, recentEmpRes, recentRideRes] = await Promise.all([
                    api.get("/admin/employees/count"),
                    api.get("/admin/rides/count"),
                    api.get("/admin/employees?size=3").catch(() => ({ data: [] })),
                    api.get("/admin/rides?size=3").catch(() => ({ data: [] })),
                ]);
                setStats({
                    employees: empRes.data.count,
                    rides: rideRes.data.count,
                });
                setRecentEmployees(recentEmpRes.data.content || recentEmpRes.data || []);
                setRecentRides(recentRideRes.data.content || recentRideRes.data || []);
            } catch {
                setStats({ employees: 0, rides: 0 });
                setRecentEmployees([]);
                setRecentRides([]);
            } finally {
                setLoading(false);
            }
        };
        fetchStats();
        localStorage.setItem("lastLogin", new Date().toLocaleString());
    }, []);

    const handleLogout = () => {
        localStorage.clear();
        navigate("/login");
    };

    return (
        <div className="min-h-screen bg-gradient-to-br from-orange-100 via-yellow-50 to-red-100 relative overflow-x-hidden">
            {/* Decorative background */}
            <div className="absolute inset-0 pointer-events-none z-0">
                <div className="absolute top-0 left-0 w-96 h-96 bg-orange-200 opacity-30 rounded-full blur-3xl -translate-x-1/2 -translate-y-1/2"></div>
                <div className="absolute bottom-0 right-0 w-96 h-96 bg-yellow-200 opacity-30 rounded-full blur-3xl translate-x-1/2 translate-y-1/2"></div>
            </div>
            {/* Top Navbar */}
            <nav className="flex justify-between items-center bg-white/80 backdrop-blur-md shadow-lg px-8 py-4 sticky top-0 z-20 border-b border-orange-100">
                <div className="flex items-center gap-3">
                    <FaUserShield className="text-2xl text-orange-500 drop-shadow" />
                    <span className="text-xl font-bold text-orange-700 tracking-wide">Admin Dashboard</span>
                </div>
                <div className="flex items-center gap-6">
                    <div className="flex items-center gap-2 text-gray-700">
                        <FaEnvelope /> <span className="hidden sm:inline">{adminEmail}</span>
                    </div>
                    <button
                        onClick={handleLogout}
                        className="flex items-center gap-2 bg-gradient-to-r from-red-500 to-orange-500 hover:from-red-600 hover:to-orange-600 text-white px-4 py-2 rounded-xl font-semibold shadow transition"
                        title="Logout"
                    >
                        <FaSignOutAlt /> <span className="hidden sm:inline">Logout</span>
                    </button>
                </div>
            </nav>
            {/* Main Content */}
            <main className="flex flex-col items-center justify-center p-4 z-10 relative">
                <div className="bg-white/80 backdrop-blur-xl rounded-3xl shadow-2xl p-8 w-full max-w-4xl mt-10 border border-orange-100">
                    {/* Profile Section */}
                    <div className="flex items-center gap-6 mb-10">
                        <div className="bg-gradient-to-br from-orange-200 to-yellow-100 rounded-full p-5 shadow-lg border-4 border-orange-300">
                            <FaUserShield className="text-5xl text-orange-500 drop-shadow" />
                        </div>
                        <div>
                            <h2 className="text-3xl font-extrabold text-gray-800 tracking-tight">{adminName}</h2>
                            <div className="text-xs text-gray-400 mt-1">
                                Last login: {lastLogin}
                            </div>
                        </div>
                    </div>
                    {/* Stats Section */}
                    <div className="grid grid-cols-1 sm:grid-cols-2 gap-8 mb-10">
                        <div
                            className="flex items-center bg-blue-50/80 rounded-2xl p-6 shadow group hover:shadow-xl hover:-translate-y-1 transition cursor-pointer border border-blue-100"
                            onClick={() => navigate("/admin/employees")}
                            title="View all employees"
                        >
                            <div className="mr-4 text-4xl">
                                <FaUsers className="text-blue-500 group-hover:scale-110 transition" />
                            </div>
                            <div>
                                <div className="text-3xl font-bold">{loading ? "..." : stats.employees}</div>
                                <div className="text-blue-700 text-base font-medium">Employees</div>
                            </div>
                            <FaChevronRight className="ml-auto text-blue-300 group-hover:text-blue-600 transition" />
                        </div>
                        <div
                            className="flex items-center bg-green-50/80 rounded-2xl p-6 shadow group hover:shadow-xl hover:-translate-y-1 transition cursor-pointer border border-green-100"
                            onClick={() => navigate("/admin/rides")}
                            title="View all rides"
                        >
                            <div className="mr-4 text-4xl">
                                <FaCarSide className="text-green-500 group-hover:scale-110 transition" />
                            </div>
                            <div>
                                <div className="text-3xl font-bold">{loading ? "..." : stats.rides}</div>
                                <div className="text-green-700 text-base font-medium">Rides</div>
                            </div>
                            <FaChevronRight className="ml-auto text-green-300 group-hover:text-green-600 transition" />
                        </div>
                    </div>
                    {/* Recent Activity */}
                    <div className="grid grid-cols-1 md:grid-cols-2 gap-8 mb-10">
                        <div>
                            <h3 className="font-semibold text-blue-700 mb-3 flex items-center gap-2 text-lg">
                                <FaUser className="text-blue-400" /> Recent Employees
                            </h3>
                            <ul className="space-y-2">
                                {recentEmployees.length === 0 ? (
                                    <li className="text-gray-400 text-sm">No recent employees.</li>
                                ) : (
                                    recentEmployees.map(emp => (
                                        <li
                                            key={emp.empId}
                                            className="flex items-center gap-2 text-gray-700 text-base bg-blue-50/70 rounded-lg px-3 py-2 hover:bg-blue-100 cursor-pointer transition"
                                            onClick={() => navigate(`/admin/employees/edit/${emp.empId}`)}
                                            title="Edit employee"
                                        >
                                            <FaUser className="text-blue-300" /> {emp.name} <span className="text-xs text-gray-400">({emp.empId})</span>
                                            <FaChevronRight className="ml-auto text-blue-200" />
                                        </li>
                                    ))
                                )}
                            </ul>
                        </div>
                        <div>
                            <h3 className="font-semibold text-green-700 mb-3 flex items-center gap-2 text-lg">
                                <FaCarSide className="text-green-400" /> Recent Rides
                            </h3>
                            <ul className="space-y-2">
                                {recentRides.length === 0 ? (
                                    <li className="text-gray-400 text-sm">No recent rides.</li>
                                ) : (
                                    recentRides.map(ride => (
                                        <li
                                            key={ride.id}
                                            className="flex items-center gap-2 text-gray-700 text-base bg-green-50/70 rounded-lg px-3 py-2 hover:bg-green-100 cursor-pointer transition"
                                            onClick={() => navigate(`/admin/rides/view/${ride.id}`)}
                                            title="View ride"
                                        >
                                            <FaCarSide className="text-green-300" /> {ride.origin} <span className="mx-1 text-gray-400">→</span> {ride.destination}
                                            <FaChevronRight className="ml-auto text-green-200" />
                                        </li>
                                    ))
                                )}
                            </ul>
                        </div>
                    </div>
                    {/* Actions */}
                    <div className="flex flex-col gap-4">
                        <Link
                            to="/admin/employees"
                            className="bg-gradient-to-r from-blue-600 to-blue-400 hover:from-blue-700 hover:to-blue-500 text-white py-3 rounded-xl font-semibold flex items-center justify-center gap-2 shadow transition"
                        >
                            <FaUsers /> View Employees
                        </Link>
                        <Link
                            to="/admin/rides"
                            className="bg-gradient-to-r from-green-600 to-green-400 hover:from-green-700 hover:to-green-500 text-white py-3 rounded-xl font-semibold flex items-center justify-center gap-2 shadow transition"
                        >
                            <FaCarSide /> View All Rides
                        </Link>
                    </div>
                </div>
            </main>
        </div>
    );
}

export default AdminDashboard;