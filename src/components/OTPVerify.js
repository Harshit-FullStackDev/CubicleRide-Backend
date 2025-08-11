import React, { useState, useEffect } from "react";
import api from "../api/axios";
import { useNavigate, useLocation } from "react-router-dom";
import { FaEnvelope, FaKey } from "react-icons/fa";

function OtpVerify() {
    const [email, setEmail] = useState("");
    const [otp, setOtp] = useState("");
    const [loading, setLoading] = useState(false);
    const navigate = useNavigate();
    const location = useLocation();

    useEffect(() => {
        if (location.state && location.state.email) {
            setEmail(location.state.email);
        }
    }, [location.state]);

    const handleSubmit = async (e) => {
        e.preventDefault();
        setLoading(true);
        try {
            await api.post("/auth/verify-otp", { email, otp });
            alert("Email verified! You can now login.");
            navigate("/login");
        } catch {
            alert("Invalid OTP or email. Please try again.");
        } finally {
            setLoading(false);
        }
    };

    return (
        <div className="min-h-screen flex items-center justify-center bg-gradient-to-br from-blue-50 to-blue-100 p-4">
            <div className="bg-white bg-opacity-95 p-8 rounded-2xl shadow-2xl w-full max-w-md flex flex-col items-center">
                <img src="/orangemantra Logo.png" alt="Logo" className="w-16 h-16 mb-4 rounded-full shadow" />
                <h2 className="text-2xl font-bold text-orange-500 mb-6 text-center">Verify Your Email</h2>
                <form onSubmit={handleSubmit} className="space-y-4 w-full">
                    <div className="flex items-center gap-3 bg-blue-50 rounded-xl px-4 py-3">
                        <FaEnvelope className="text-orange-400" />
                        <input
                            name="email"
                            placeholder="Email"
                            value={email}
                            onChange={e => setEmail(e.target.value)}
                            required
                            type="email"
                            className="bg-transparent w-full outline-none"
                        />
                    </div>
                    <div className="flex items-center gap-3 bg-blue-50 rounded-xl px-4 py-3">
                        <FaKey className="text-orange-400" />
                        <input
                            name="otp"
                            placeholder="Enter OTP"
                            value={otp}
                            onChange={e => setOtp(e.target.value)}
                            required
                            className="bg-transparent w-full outline-none"
                        />
                    </div>
                    <button
                        type="submit"
                        className="w-full bg-orange-600 hover:bg-blue-700 text-white py-3 rounded-xl transition font-semibold mt-2"
                        disabled={loading}
                    >
                        {loading ? "Verifying..." : "Verify Email"}
                    </button>
                </form>
            </div>
        </div>
    );
}

export default OtpVerify;

