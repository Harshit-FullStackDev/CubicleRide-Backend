import React, { useEffect, useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import api from "../../api/axios";
import {
    FaUser, FaMapMarkerAlt, FaCar, FaChair, FaCalendarAlt, FaClock,
    FaEdit, FaTrash, FaBell, FaCheckCircle, FaSignOutAlt, FaBars, FaTimes, FaPlus, FaUsers
} from "react-icons/fa";

function EmployeeDashboard() {
    const navigate = useNavigate();
    const [empName, setEmpName] = useState("");
    const [empEmail, setEmpEmail] = useState("");
    const [empId, setEmpId] = useState("");
    const [publishedRides, setPublishedRides] = useState([]);
    const [joinedRides, setJoinedRides] = useState([]);
    const [stats, setStats] = useState({ total: 0 });
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);
    const [lastLogin, setLastLogin] = useState("");
    const [sidebarOpen, setSidebarOpen] = useState(false);

    useEffect(() => {
        const token = localStorage.getItem("token");
        const role = localStorage.getItem("role");
        if (!token || role !== "EMPLOYEE") {
            navigate("/login");
        }
        setEmpName(localStorage.getItem("name") || "Employee");
        setEmpEmail(localStorage.getItem("email") || "employee@company.com");
        const id = localStorage.getItem("empId") || "";
        setEmpId(id);
        setLastLogin(localStorage.getItem("lastLogin") || new Date().toLocaleString());
        fetchDashboardData(id);
    }, [navigate]);

    const fetchDashboardData = async (empId) => {
        setLoading(true);
        try {
            const [ridesRes, joinedRes] = await Promise.all([
                api.get("/ride/my-rides"),
                api.get(`/ride/joined/${empId}`)
            ]);
            setPublishedRides(ridesRes.data);
            setJoinedRides(joinedRes.data);
            let total = ridesRes.data.length;
            setStats({ total });
            setError(null);
        } catch (err) {
            setError("Failed to load dashboard data.");
        }
        setLoading(false);
    };

    const handleLogout = () => {
        localStorage.clear();
        navigate("/login");
    };

    const handleEdit = (id) => {
        navigate(`/employee/edit/${id}`);
    };

    const handleDelete = async (id) => {
        if (!window.confirm("Are you sure you want to delete this ride?")) return;
        try {
            await api.delete(`/ride/${id}`);
            fetchDashboardData(empId);
        } catch (err) {
            alert("Failed to delete ride.");
        }
    };

    const handleLeave = async (id) => {
        if (!window.confirm("Leave this ride?")) return;
        try {
            await api.post(`/ride/leave/${id}`, { empId });
            fetchDashboardData(empId);
        } catch (err) {
            alert("Failed to leave ride.");
        }
    };

    const formatDate = (dateStr) => {
        const d = new Date(dateStr);
        return d.toLocaleDateString(undefined, { year: "numeric", month: "short", day: "numeric" });
    };

    const formatTime = (timeStr) => timeStr?.slice(0, 5);

    const getInitials = (name) => name.split(" ").map(n => n[0]).join("").toUpperCase();

    // Animated card classes
    const cardAnim = "transition-all duration-300 ease-in-out transform hover:scale-[1.03] hover:shadow-2xl";
    const glass = "bg-white/70 backdrop-blur-lg shadow-xl border border-blue-100";

    if (loading) return <div className="flex items-center justify-center min-h-screen text-blue-700 animate-pulse text-xl font-bold">Loading dashboard...</div>;
    if (error) return <div className="flex items-center justify-center min-h-screen text-red-600 text-lg">{error}</div>;

    return (
        <div className="min-h-screen flex bg-gradient-to-br from-blue-50 via-white to-blue-100 relative overflow-hidden">
            {/* Background image */}
            <img src="/microsoft.jpg" alt="background" className="fixed inset-0 w-full h-full object-cover z-0 opacity-60 pointer-events-none select-none" style={{filter: 'blur(2px)'}} />
            {/* Sidebar */}
            <aside className={`fixed z-30 top-0 left-0 h-full w-64 p-6 flex flex-col gap-8 ${glass} ${sidebarOpen ? 'translate-x-0' : '-translate-x-full'} md:translate-x-0 transition-transform duration-300`}>
                <div className="flex items-center gap-3 mb-8">
                    <img src="/orangemantra%20Logo.png" alt="Logo" className="w-10 h-10 rounded-full shadow-lg border-2 border-blue-200" />
                    <span className="text-2xl font-bold text-orange-500 tracking-tight select-none">Orange<span className='text-blue-400'>Mantra</span></span>
                </div>
                <nav className="flex flex-col gap-4 text-orange-500 font-semibold">
                    <Link to="/employee/dashboard" className="flex items-center gap-3 hover:text-orange-400 transition"><FaUser /> Dashboard</Link>
                    <Link to="/employee/offer" className="flex items-center gap-3 hover:text-orange-400 transition"><FaPlus /> Offer a Ride</Link>
                    <Link to="/employee/join" className="flex items-center gap-3 hover:text-orange-400 transition"><FaUsers /> Join a Ride</Link>
                    <Link to="/employee/notifications" className="flex items-center gap-3 hover:text-orange-400 transition"><FaBell /> Notifications</Link>
                    <Link to="/employee/history/published" className="flex items-center gap-3 hover:text-orange-400 transition"><FaCar /> Published History</Link>
                    <Link to="/employee/history/joined" className="flex items-center gap-3 hover:text-orange-400 transition"><FaUsers /> Joined History</Link>
                </nav>
                <div className="mt-auto flex flex-col gap-2">
                    <button onClick={handleLogout} className="flex items-center gap-2 px-4 py-2 rounded-lg bg-gradient-to-r from-red-500 to-orange-500 text-white font-bold shadow hover:from-red-600 hover:to-orange-600 transition"><FaSignOutAlt /> Logout</button>
                </div>
            </aside>
            {/* Overlay for mobile sidebar */}
            {sidebarOpen && <div className="fixed inset-0 z-20 bg-black/30 md:hidden" onClick={() => setSidebarOpen(false)}></div>}
            {/* Main Content */}
            <div className="flex-1 flex flex-col min-h-screen md:ml-64">
                {/* Navbar */}
                <header className="flex items-center justify-between px-4 md:px-10 py-4 sticky top-0 z-10 bg-white/80 backdrop-blur-lg shadow border-b border-blue-100">
                    <button className="md:hidden text-2xl text-blue-700" onClick={() => setSidebarOpen(!sidebarOpen)}>
                        {sidebarOpen ? <FaTimes /> : <FaBars />}
                    </button>
                    <div className="flex items-center gap-3">
                        <span className="text-xl font-bold text-blue-700">Welcome, {empName.split(' ')[0]}</span>
                        <span className="hidden md:inline text-gray-400 text-sm ml-2">Last login: {lastLogin}</span>
                    </div>
                    <div className="flex items-center gap-4">
                        <div className="relative group">
                            <div className="w-10 h-10 rounded-full bg-gradient-to-br from-blue-400 to-blue-200 flex items-center justify-center text-xl font-bold text-white border-2 border-blue-300 cursor-pointer shadow-lg">
                                {getInitials(empName)}
                            </div>
                            <div className="absolute right-0 mt-2 w-40 bg-white rounded-xl shadow-lg p-3 hidden group-hover:block z-50">
                                <div className="text-blue-700 font-bold mb-2">{empName}</div>
                                <div className="text-xs text-gray-500 mb-2">{empEmail}</div>
                                <button onClick={handleLogout} className="w-full flex items-center gap-2 px-3 py-2 rounded-lg bg-gradient-to-r from-red-500 to-orange-500 text-white font-bold shadow hover:from-red-600 hover:to-orange-600 transition"><FaSignOutAlt /> Logout</button>
                            </div>
                        </div>
                    </div>
                </header>
                {/* Stats Cards */}
                <section className="w-full max-w-6xl mx-auto grid grid-cols-1 md:grid-cols-3 gap-6 mt-8 px-4 animate-fade-in-up">
                    <div className={`${glass} ${cardAnim} flex flex-col items-center p-6 rounded-2xl text-center`}>
                        <FaCar className="text-blue-500 text-4xl mb-2 animate-bounce" />
                        <div className="text-3xl font-bold text-blue-700">{stats.total}</div>
                        <div className="text-gray-500 font-semibold">Rides Published</div>
                    </div>
                    <Link to="/employee/notifications" className={`${glass} ${cardAnim} flex flex-col items-center p-6 rounded-2xl text-center`}>
                        <FaBell className="text-yellow-500 text-4xl mb-2" />
                        <div className="text-xl font-bold text-yellow-600">View Notifications</div>
                        <div className="text-gray-500 font-semibold">Go to notifications</div>
                    </Link>
                    <Link to="/employee/history/joined" className={`${glass} ${cardAnim} flex flex-col items-center p-6 rounded-2xl text-center`}>
                        <FaUsers className="text-green-500 text-4xl mb-2" />
                        <div className="text-xl font-bold text-green-700">Joined Rides History</div>
                        <div className="text-gray-500 font-semibold">See past joined rides</div>
                    </Link>
                </section>
                {/* Published Rides */}
                <section className="w-full max-w-6xl mx-auto mt-10 px-4 animate-fade-in-up">
                    <h3 className="text-2xl font-bold text-blue-700 mb-4 text-center">Your Published Rides</h3>
                    {publishedRides.length === 0 ? (
                        <p className="text-gray-400 text-center">You haven’t published any rides yet.</p>
                    ) : (
                        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
                            {publishedRides.map((ride) => (
                                <div
                                    key={ride.id}
                                    className={`${glass} ${cardAnim} rounded-2xl p-6 flex flex-col gap-3 relative group`}
                                >
                                    <div className="flex items-center gap-3 mb-1">
                                        <FaMapMarkerAlt className="text-green-500" />
                                        <span className="font-semibold">{ride.origin}</span>
                                        <span className="mx-2 text-gray-400">→</span>
                                        <span className="font-semibold">{ride.destination}</span>
                                    </div>
                                    <div className="flex items-center gap-3 text-sm">
                                        <FaCalendarAlt className="text-purple-500" />
                                        <span>{formatDate(ride.date)}</span>
                                        <FaClock className="ml-4 text-yellow-500" />
                                        <span>{formatTime(ride.arrivalTime)}</span>
                                    </div>
                                    <div className="flex items-center gap-3 text-sm">
                                        <FaCar className="text-gray-500" />
                                        <span>{ride.carDetails}</span>
                                    </div>
                                    <div className="flex items-center gap-3 text-sm">
                                        <FaChair className="text-pink-500" />
                                        <span>{ride.availableSeats}/{ride.totalSeats} seats available</span>
                                        <div className="w-24 h-2 bg-gray-200 rounded-full overflow-hidden ml-2" title={`Occupancy: ${ride.totalSeats - ride.availableSeats}/${ride.totalSeats}`}>
                                            <div
                                                className="h-2 bg-blue-400 transition-all duration-500"
                                                style={{ width: `${((ride.totalSeats - ride.availableSeats) / ride.totalSeats) * 100}%` }}
                                            ></div>
                                        </div>
                                    </div>
                                    <div className="flex items-center gap-3">
                                        <span className={`px-2 py-1 rounded text-xs font-semibold flex items-center gap-1 ${ride.status === "Active" ? "bg-green-100 text-green-700" : "bg-gray-200 text-gray-600"}`}>
                                            {ride.status === "Active" ? <FaCheckCircle className="text-green-500" /> : <FaClock className="text-gray-400" />} {ride.status || "Active"}
                                        </span>
                                    </div>
                                    {/* Owner info */}
                                    <div className="mt-2 text-xs text-blue-700">
                                        <strong>Owner:</strong> {ride.ownerName} ({ride.ownerEmpId})
                                    </div>
                                    {/* Joined Employees Avatars + Details */}
                                    <div className="flex flex-col gap-1 mt-2">
                                        <div className="flex items-center gap-2">
                                            <span className="text-xs font-semibold text-blue-700">Joined Employees:</span>
                                        </div>
                                        {ride.joinedEmployees && ride.joinedEmployees.length > 0 ? (
                                            <ul className="ml-2 list-disc text-xs text-gray-700">
                                                {ride.joinedEmployees.map(emp => (
                                                    <li key={emp.empId}>
                                                        {emp.name} ({emp.empId})
                                                    </li>
                                                ))}
                                            </ul>
                                        ) : (
                                            <span className="text-xs text-gray-400 ml-2">No employees joined</span>
                                        )}
                                    </div>
                                    {/* Actions */}
                                    <div className="flex gap-2 mt-3 opacity-0 group-hover:opacity-100 transition-opacity">
                                        <button
                                            onClick={() => handleEdit(ride.id)}
                                            className="flex items-center gap-1 bg-blue-500 hover:bg-blue-600 text-white px-3 py-1 rounded text-xs transition"
                                            title="Edit Ride"
                                        >
                                            <FaEdit /> Edit
                                        </button>
                                        <button
                                            onClick={() => handleDelete(ride.id)}
                                            className="flex items-center gap-1 bg-red-500 hover:bg-red-600 text-white px-3 py-1 rounded text-xs transition"
                                            title="Delete Ride"
                                        >
                                            <FaTrash /> Delete
                                        </button>
                                    </div>
                                </div>
                            ))}
                        </div>
                    )}
                </section>
                {/* Joined Rides */}
                <section className="w-full max-w-6xl mx-auto mt-10 mb-10 px-4 animate-fade-in-up">
                    <h3 className="text-2xl font-bold text-green-700 mb-4 text-center">Rides You've Joined</h3>
                    {joinedRides.length === 0 ? (
                        <p className="text-gray-400 text-center">You haven’t joined any rides yet.</p>
                    ) : (
                        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
                            {joinedRides.map((ride) => (
                                <div
                                    key={ride.id}
                                    className={`bg-green-50 ${cardAnim} rounded-2xl p-6 flex flex-col gap-3 border border-green-200 relative group`}
                                >
                                    <div className="flex items-center gap-3 mb-1">
                                        <FaMapMarkerAlt className="text-green-500" />
                                        <span className="font-semibold">{ride.origin}</span>
                                        <span className="mx-2 text-gray-400">→</span>
                                        <span className="font-semibold">{ride.destination}</span>
                                    </div>
                                    <div className="flex items-center gap-3 text-sm">
                                        <FaCalendarAlt className="text-purple-500" />
                                        <span>{formatDate(ride.date)}</span>
                                        <FaClock className="ml-4 text-yellow-500" />
                                        <span>{formatTime(ride.arrivalTime)}</span>
                                    </div>
                                    <div className="flex items-center gap-3 text-sm">
                                        <FaCar className="text-gray-500" />
                                        <span>{ride.carDetails}</span>
                                    </div>
                                    <div className="flex items-center gap-3 text-sm">
                                        <FaChair className="text-pink-500" />
                                        <span>{ride.availableSeats}/{ride.totalSeats} seats available</span>
                                    </div>
                                    <div className="flex items-center gap-3">
                                        <span className={`px-2 py-1 rounded text-xs font-semibold ${ride.status === "Active" ? "bg-green-100 text-green-700" : "bg-gray-200 text-gray-600"}`}>
                                            {ride.status || "Active"}
                                        </span>
                                    </div>
                                    {/* Owner info */}
                                    <div className="mt-2 text-xs text-blue-700">
                                        <strong>Owner:</strong> {ride.ownerName} ({ride.ownerEmpId})
                                    </div>
                                    {/* Joined Employees */}
                                    <div className="mt-2">
                                        <div className="flex items-center text-green-700 font-semibold text-xs mb-1">
                                            <FaUsers className="mr-1" /> Joined Employees:
                                        </div>
                                        {ride.joinedEmployees && ride.joinedEmployees.length > 0 ? (
                                            <ul className="ml-6 list-disc text-xs text-gray-700">
                                                {ride.joinedEmployees.map(emp => (
                                                    <li key={emp.empId}>
                                                        {emp.name} ({emp.empId})
                                                    </li>
                                                ))}
                                            </ul>
                                        ) : (
                                            <span className="ml-6 text-gray-400 text-xs">No one has joined yet.</span>
                                        )}
                                    </div>
                                    <div className="flex gap-2 mt-3 opacity-0 group-hover:opacity-100 transition-opacity">
                                        <button
                                            onClick={() => handleLeave(ride.id)}
                                            className="flex items-center gap-1 bg-red-500 hover:bg-red-600 text-white px-3 py-1 rounded text-xs transition"
                                            title="Leave Ride"
                                        >
                                            <FaUser /> Leave
                                        </button>
                                    </div>
                                </div>
                            ))}
                        </div>
                    )}
                </section>
            </div>
        </div>
    );
}
export default EmployeeDashboard;

