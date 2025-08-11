import React, { useEffect, useState } from "react";
import api from "../../api/axios";
import { useParams, useNavigate } from "react-router-dom";

function EditEmployee() {
    const { id } = useParams();
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);
    const [employee, setEmployee] = useState({ name: "", email: "", empId: "" });
    const navigate = useNavigate();

    useEffect(() => {
        api.get(`/admin/employees/${id}`)
            .then(res => setEmployee(res.data))
            .catch(() => setError("Failed to load employee."))
            .finally(() => setLoading(false));
    }, [id]);

    const handleChange = (e) => {
        setEmployee({ ...employee, [e.target.name]: e.target.value });
    };

    const handleSubmit = async (e) => {
        e.preventDefault();
        try {
            await api.put(`/admin/employees/${id}`, employee);
            alert("Employee updated!");
            navigate("/admin/employees");
        } catch {
            alert("Failed to update employee.");
        }
    };

    if (loading) return <div className="p-8 text-center">Loading...</div>;
    if (error) return <div className="p-8 text-center text-red-500">{error}</div>;

    return (
        <div className="min-h-screen flex items-center justify-center bg-gradient-to-br from-blue-50 to-blue-100">
            <form onSubmit={handleSubmit} className="bg-white p-8 rounded-xl shadow w-full max-w-md">
                <h2 className="text-2xl font-bold mb-6 text-blue-700">Edit Employee</h2>
                <div className="mb-4">
                    <label className="block mb-1 font-medium text-blue-700">Name</label>
                    <input
                        type="text"
                        name="name"
                        className="w-full border border-blue-200 rounded px-3 py-2 focus:ring-2 focus:ring-blue-400"
                        value={employee.name}
                        onChange={handleChange}
                        required
                    />
                </div>
                <div className="mb-4">
                    <label className="block mb-1 font-medium text-blue-700">Email</label>
                    <input
                        type="email"
                        name="email"
                        className="w-full border border-blue-200 rounded px-3 py-2 focus:ring-2 focus:ring-blue-400"
                        value={employee.email}
                        onChange={handleChange}
                        required
                    />
                </div>
                <div className="flex gap-4">
                    <button
                        type="submit"
                        className="bg-blue-600 text-white px-4 py-2 rounded hover:bg-blue-700 transition"
                    >
                        Save
                    </button>
                    <button
                        type="button"
                        className="bg-gray-200 text-gray-700 px-4 py-2 rounded hover:bg-gray-300 transition"
                        onClick={() => navigate("/admin/employees")}
                    >
                        Cancel
                    </button>
                </div>
            </form>
        </div>
    );
}

export default EditEmployee;