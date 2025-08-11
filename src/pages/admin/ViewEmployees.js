import React, { useEffect, useState } from "react";
import api from "../../api/axios";
import { FaUser, FaSearch, FaEdit, FaTrash, FaIdBadge, FaEnvelope, FaArrowLeft } from "react-icons/fa";
import { useNavigate } from "react-router-dom";

function ViewEmployees() {
    const [employees, setEmployees] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);
    const [search, setSearch] = useState("");
    const [deletingId, setDeletingId] = useState(null);
    const navigate = useNavigate();

    useEffect(() => {
        fetchEmployees();
    }, []);

    const fetchEmployees = () => {
        setLoading(true);
        api.get("/admin/employees")
            .then(res => setEmployees(res.data))
            .catch(() => setError("Failed to load employees."))
            .finally(() => setLoading(false));
    };

    const handleDelete = async (empId) => {
        if (!window.confirm("Are you sure you want to delete this employee?")) return;
        setDeletingId(empId);
        try {
            await api.delete(`/admin/employees/${empId}`);
            setEmployees(prev => prev.filter(emp => emp.empId !== empId));
        } catch {
            alert("Failed to delete employee.");
        } finally {
            setDeletingId(null);
        }
    };

    const handleBackToDashboard = () => {
        navigate("/admin/dashboard");
    };

    const filteredEmployees = employees.filter(emp =>
        (emp.name && emp.name.toLowerCase().includes(search.toLowerCase())) ||
        (emp.email && emp.email.toLowerCase().includes(search.toLowerCase())) ||
        (emp.empId && emp.empId.toLowerCase().includes(search.toLowerCase()))
    );

    if (loading) {
        return (
            <div className="flex items-center justify-center min-h-screen bg-gradient-to-br from-blue-50 to-blue-100">
                <div className="animate-spin rounded-full h-16 w-16 border-t-4 border-blue-500 border-solid"></div>
            </div>
        );
    }

    if (error) {
        return (
            <div className="flex items-center justify-center min-h-screen bg-gradient-to-br from-blue-50 to-blue-100">
                <div className="bg-red-100 border border-red-300 text-red-700 px-6 py-4 rounded-lg shadow">
                    {error}
                </div>
            </div>
        );
    }

    return (
        <div className="min-h-screen flex flex-col items-center bg-gradient-to-br from-blue-50 to-blue-100 p-6">
            <div className="bg-white rounded-2xl shadow-2xl p-8 w-full max-w-4xl">
                <button
                    onClick={handleBackToDashboard}
                    className="mb-6 flex items-center gap-2 px-4 py-2 bg-blue-100 text-blue-700 rounded hover:bg-blue-200 transition"
                >
                    <FaArrowLeft /> Back to Dashboard
                </button>
                <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4 mb-6">
                    <h2 className="text-2xl font-bold text-blue-700 flex items-center gap-2">
                        <FaUser className="text-blue-500" /> Employees
                    </h2>
                    <div className="relative w-full sm:w-80">
                        <input
                            type="text"
                            placeholder="Search by name, email, or ID..."
                            className="w-full pl-10 pr-4 py-2 rounded-lg border border-blue-200 focus:ring-2 focus:ring-blue-400 focus:outline-none transition"
                            value={search}
                            onChange={e => setSearch(e.target.value)}
                        />
                        <FaSearch className="absolute left-3 top-1/2 -translate-y-1/2 text-blue-400" />
                    </div>
                </div>
                <div className="overflow-x-auto rounded-lg border border-blue-100 shadow">
                    <table className="min-w-full bg-white">
                        <thead className="bg-blue-50 sticky top-0 z-10">
                        <tr>
                            <th className="py-3 px-4 text-left font-semibold text-blue-700"><FaIdBadge className="inline mr-1" /> Employee ID</th>
                            <th className="py-3 px-4 text-left font-semibold text-blue-700"><FaUser className="inline mr-1" /> Name</th>
                            <th className="py-3 px-4 text-left font-semibold text-blue-700"><FaEnvelope className="inline mr-1" /> Email</th>
                            <th className="py-3 px-4 text-center font-semibold text-blue-700">Actions</th>
                        </tr>
                        </thead>
                        <tbody>
                        {filteredEmployees.length === 0 ? (
                            <tr>
                                <td colSpan={4} className="py-6 text-center text-gray-400">
                                    No employees found.
                                </td>
                            </tr>
                        ) : (
                            filteredEmployees.map((emp, idx) => (
                                <tr
                                    key={emp.empId}
                                    className={`transition hover:bg-blue-50 ${idx % 2 === 0 ? "bg-white" : "bg-blue-50/50"}`}
                                >
                                    <td className="py-3 px-4">{emp.empId}</td>
                                    <td className="py-3 px-4 flex items-center gap-2">
                                        <FaUser className="text-blue-300" /> {emp.name}
                                    </td>
                                    <td className="py-3 px-4">{emp.email}</td>
                                    <td className="py-3 px-4">
                                        <div className="flex justify-center items-center gap-4">
                                            <button
                                                className="text-blue-600 hover:text-blue-800 transition p-2 rounded-full hover:bg-blue-100"
                                                onClick={() => navigate(`/admin/employees/edit/${emp.empId}`)}
                                                title="Edit"
                                            >
                                                <FaEdit />
                                            </button>
                                            <button
                                                className={`text-red-600 hover:text-red-800 transition p-2 rounded-full hover:bg-red-100 ${deletingId === emp.empId ? "opacity-50 cursor-not-allowed" : ""}`}
                                                onClick={() => handleDelete(emp.empId)}
                                                disabled={deletingId === emp.empId}
                                                title="Delete"
                                            >
                                                <FaTrash />
                                            </button>
                                        </div>
                                    </td>
                                </tr>
                            ))
                        )}
                        </tbody>
                    </table>
                </div>
            </div>
        </div>
    );
}

export default ViewEmployees;