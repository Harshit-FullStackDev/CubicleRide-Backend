import React, { useState, useEffect, useRef } from "react";
import api from "../api/axios";
import { useNavigate } from "react-router-dom";
import { FaIdBadge, FaUser, FaEnvelope, FaLock } from "react-icons/fa";

function Register() {
    const [data, setData] = useState({
        empId: "",
        name: "",
        email: "",
        password: ""
    });
    const [errors, setErrors] = useState({});
    const empRef = useRef(null);

    const navigate = useNavigate();

    useEffect(() => {
        const token = localStorage.getItem("token");
        const role = localStorage.getItem("role");
        if (token && role === "EMPLOYEE") navigate("/employee/dashboard");
        else if (token && role === "ADMIN") navigate("/admin/dashboard");
    }, [navigate]);

    const handleChange = (e) => {
        setData({ ...data, [e.target.name]: e.target.value });
    };

    const handleSubmit = async (e) => {
        e.preventDefault();
        setErrors({});
        // quick email validation client-side
        const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
        if (!emailRegex.test(data.email)) {
            setErrors({ email: "Please enter a valid email." });
            return;
        }
        try {
            await api.post("/auth/register", data);
            alert("Registered! Please check your email for OTP.");
            navigate("/otp-verify", { state: { email: data.email } });
        } catch (err) {
            const status = err?.response?.status;
            const raw = err?.response?.data;
            let msgText = "Registration failed";
            if (typeof raw === "string") {
                msgText = raw;
            } else if (raw && typeof raw === "object") {
                msgText = raw.message || raw.error || msgText;
            } else if (err?.message) {
                msgText = err.message;
            }
            if (status === 409 || (typeof msgText === "string" && msgText.toLowerCase().includes("already"))) {
                setErrors({ form: "Already registered (use emp_id or email if same)." });
            } else {
                setErrors({ form: String(msgText) });
            }
        }
    };

    return (
        <div style={{
            minHeight: "100vh",
            backgroundImage: "url('/microsoft-background.png')",
            backgroundSize: "cover",
            backgroundPosition: "center",
            backgroundRepeat: "no-repeat",
            display: "flex",
            alignItems: "center",
            justifyContent: "center",
            padding: "1rem"
        }}>
            <div className=" p-8 rounded-2xl shadow-1xl w-full max-w-md flex flex-col items-center">
                <img src="/orangemantra Logo.png" alt="Logo" className="w-16 h-16 mb-4 rounded-full shadow" />
                <h2 className="text-2xl font-bold text-orange-500 mb-6 text-center px-6 font-serif">Employee Registration</h2>
                <form onSubmit={handleSubmit} className="space-y-5 w-full">
                    {errors.form && (<div className="bg-red-100 text-red-800 p-3 rounded">{errors.form}</div>)}
                    <div className="relative">
                        <div className="flex items-center gap-3 bg-blue-50 rounded-xl px-4 py-4">
                            <FaIdBadge className="text-orange-400" />
                            <input
                                ref={empRef}
                                name="empId"
                                className="bg-transparent w-full outline-none pt-3 pb-1 peer"
                                value={data.empId}
                                onChange={handleChange}
                                required
                            />
                            <label className={`absolute left-12 top-1/2 -translate-y-1/2 text-gray-400 transition-all pointer-events-none peer-focus:text-xs peer-focus:-top-2 peer-focus:translate-y-0 ${data.empId ? "text-xs -top-2 translate-y-0" : ""}`}>
                                Employee ID
                            </label>
                        </div>
                    </div>
                    <div className="relative">
                        <div className="flex items-center gap-3 bg-blue-50 rounded-xl px-4 py-4">
                            <FaUser className="text-orange-400" />
                            <input
                                name="name"
                                className="bg-transparent w-full outline-none pt-3 pb-1 peer"
                                value={data.name}
                                onChange={handleChange}
                                required
                            />
                            <label className={`absolute left-12 top-1/2 -translate-y-1/2 text-gray-400 transition-all pointer-events-none peer-focus:text-xs peer-focus:-top-2 peer-focus:translate-y-0 ${data.name ? "text-xs -top-2 translate-y-0" : ""}`}>
                                Name
                            </label>
                        </div>
                    </div>
                    <div className="relative">
                        <div className="flex items-center gap-3 bg-blue-50 rounded-xl px-4 py-4">
                            <FaEnvelope className="text-orange-400" />
                            <input
                                name="email"
                                className="bg-transparent w-full outline-none pt-3 pb-1 peer"
                                value={data.email}
                                onChange={handleChange}
                                required
                                type="email"
                            />
                            <label className={`absolute left-12 top-1/2 -translate-y-1/2 text-gray-400 transition-all pointer-events-none peer-focus:text-xs peer-focus:-top-2 peer-focus:translate-y-0 ${data.email ? "text-xs -top-2 translate-y-0" : ""}`}>
                                Email
                            </label>
                        </div>
                        {errors.email && <div className="text-red-600 text-xs mt-1">{errors.email}</div>}
                    </div>
                    <div className="relative">
                        <div className="flex items-center gap-3 bg-blue-50 rounded-xl px-4 py-4">
                            <FaLock className="text-orange-400" />
                            <input
                                name="password"
                                className="bg-transparent w-full outline-none pt-3 pb-1 peer"
                                value={data.password}
                                onChange={handleChange}
                                required
                                type="password"
                            />
                            <label className={`absolute left-12 top-1/2 -translate-y-1/2 text-gray-400 transition-all pointer-events-none peer-focus:text-xs peer-focus:-top-2 peer-focus:translate-y-0 ${data.password ? "text-xs -top-2 translate-y-0" : ""}`}>
                                Password
                            </label>
                        </div>
                    </div>
                    <button
                        type="submit"
                        className="w-full bg-orange-600 hover:bg-blue-700 text-white py-3 rounded-xl transition font-semibold mt-2"
                    >
                        Register
                    </button>
                    <p className="text-center text-sm mt-4 text-gray-600">
                        Already have an account?{" "}
                        <span
                            onClick={() => navigate("/login")}
                            className="text-orange-600 font-semibold cursor-pointer hover:underline"
                        >
                            Login
                        </span>
                    </p>
                </form>
            </div>
        </div>
    );
}

export default Register;