import React, { useState, useEffect, useRef } from "react";
import api from "../api/axios";
import { useNavigate } from "react-router-dom";
import { FaEnvelope, FaLock } from "react-icons/fa";

function Login() {
    const [data, setData] = useState({ email: "", password: "" });
    const [loading, setLoading] = useState(false);
    const [errors, setErrors] = useState({});
    const navigate = useNavigate();
    const emailRef = useRef(null);

    useEffect(() => {
        const token = localStorage.getItem("token");
        const role = localStorage.getItem("role");
        if (token && role === "EMPLOYEE") navigate("/employee/dashboard");
        else if (token && role === "ADMIN") navigate("/admin/dashboard");
        // focus email on load for quick login
        setTimeout(() => emailRef.current?.focus(), 0);
    }, [navigate]);

    const handleChange = (e) =>
        setData({ ...data, [e.target.name]: e.target.value });

    const handleSubmit = async (e) => {
        e.preventDefault();
        setErrors({});
        setLoading(true);
        try {
            const res = await api.post("/auth/login", {
                email: data.email,
                password: data.password
            });
            const token = res.data.token;
            localStorage.setItem("token", token);
            const payload = JSON.parse(atob(token.split(".")[1]));
            localStorage.setItem("role", payload.role);
            localStorage.setItem("empId", payload.empId);
            localStorage.setItem("name", payload.name);
            localStorage.setItem("email", data.email);

            if (payload.role === "EMPLOYEE") navigate("/employee/dashboard");
            else navigate("/admin/dashboard");
        } catch (err) {
            setErrors({ form: "Invalid credentials" });
        } finally {
            setLoading(false);
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
            <div className="bg-white-400 bg-opacity-95 p-8 rounded-2xl shadow-1xl w-fit max-w-md flex flex-col items-center">
                <img src="/orangemantra Logo.png" alt="Logo" className="w-16 h-16 mb-4 rounded-full shadow" />
                <h2 className="text-3xl font-bold text-orange-500 mb-6 text-center font-serif italic">Welcome Back!</h2>
                <p className="mb-6 text-white font-bold text-center">Login to continue.</p>
                {loading ? (
                    <div className="text-center text-orange-600 text-xl font-semibold animate-pulse">
                        Logging in, please wait...
                    </div>
                ) : (
                    <form onSubmit={handleSubmit} className="space-y-5 w-full">
                        {errors.form && (<div className="bg-red-100 text-red-800 p-3 rounded">{errors.form}</div>)}
                        <div className="relative">
                            <div className="flex items-center gap-3 bg-blue-50 rounded-xl px-4 py-4">
                                <FaEnvelope className="text-orange-400" />
                                <input
                                    ref={emailRef}
                                    name="email"
                                    type="email"
                                    className="bg-transparent w-full outline-none pt-3 pb-1 peer"
                                    value={data.email}
                                    onChange={handleChange}
                                    required
                                />
                                <label className={`absolute left-12 top-1/2 -translate-y-1/2 text-gray-400 transition-all pointer-events-none peer-focus:text-xs peer-focus:-top-2 peer-focus:translate-y-0 ${data.email ? "text-xs -top-2 translate-y-0" : ""}`}>
                                    Email
                                </label>
                            </div>
                        </div>
                        <div className="relative">
                            <div className="flex items-center gap-3 bg-blue-50 rounded-xl px-4 py-4">
                                <FaLock className="text-orange-400" />
                                <input
                                    name="password"
                                    type="password"
                                    className="bg-transparent w-full outline-none pt-3 pb-1 peer"
                                    value={data.password}
                                    onChange={handleChange}
                                    required
                                />
                                <label className={`absolute left-12 top-1/2 -translate-y-1/2 text-gray-400 transition-all pointer-events-none peer-focus:text-xs peer-focus:-top-2 peer-focus:translate-y-0 ${data.password ? "text-xs -top-2 translate-y-0" : ""}`}>
                                    Password
                                </label>
                            </div>
                        </div>
                        <button type="submit"
                                className="w-full bg-orange-600 hover:bg-blue-700 text-white py-3 rounded-xl transition font-semibold mt-2">
                            Login
                        </button>
                        <p className="text-center text-sm mt-4 text-gray-600">
                            Don&#39;t have an account?{" "}
                            <span
                                onClick={() => navigate("/register")}
                                className="text-orange-700 font-semibold cursor-pointer hover:underline"
                            >
                                Register
                            </span>
                        </p>
                    </form>
                )}
            </div>
        </div>
    );
}

export default Login;