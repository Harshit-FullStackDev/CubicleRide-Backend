import React, { useEffect, useState } from "react";
import api from "../../api/axios";
import { FaUser, FaMapMarkerAlt, FaCar, FaChair, FaUsers, FaCalendarAlt, FaClock } from "react-icons/fa";

function ViewRides() {
    const [rides, setRides] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);

    useEffect(() => {
        api.get("/admin/rides")
            .then(res => setRides(res.data))
            .catch(() => setError("Failed to load rides."))
            .finally(() => setLoading(false));
    }, []);

    if (loading) {
        return (
            <div className="flex items-center justify-center h-64">
                <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-500"></div>
            </div>
        );
    }

    if (error) {
        return (
            <div className="flex items-center justify-center h-64 text-red-600 font-semibold">
                {error}
            </div>
        );
    }

    return (
        <div className="min-h-screen bg-gradient-to-br from-blue-50 to-blue-100 p-8">
            <h2 className="text-3xl font-bold mb-8 text-blue-700 text-center">All Rides</h2>
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-8">
                {rides.map(ride => (
                    <div key={ride.id} className="bg-white rounded-2xl shadow-xl p-6 flex flex-col gap-3 hover:shadow-2xl transition">
                        <div className="flex items-center gap-3 mb-2">
                            <FaUser className="text-blue-400" />
                            <span className="font-semibold text-gray-700">Owner:</span>
                            <span className="text-gray-800">{ride.ownerEmpId || "N/A"}</span>
                        </div>
                        <div className="flex items-center gap-3">
                            <FaMapMarkerAlt className="text-green-500" />
                            <span className="font-semibold">{ride.origin || "?"}</span>
                            <span className="mx-2 text-gray-400">→</span>
                            <span className="font-semibold">{ride.destination || "?"}</span>
                        </div>
                        <div className="flex items-center gap-3">
                            <FaCalendarAlt className="text-purple-500" />
                            <span>{ride.date || "N/A"}</span>
                            <FaClock className="ml-4 text-yellow-500" />
                            <span>{ride.arrivalTime || "N/A"}</span>
                        </div>
                        <div className="flex items-center gap-3">
                            <FaCar className="text-gray-500" />
                            <span>{ride.carDetails || "N/A"}</span>
                        </div>
                        <div className="flex items-center gap-3">
                            <FaChair className="text-pink-500" />
                            <span>
                                {ride.availableSeats !== undefined && ride.totalSeats !== undefined
                                    ? `${ride.availableSeats}/${ride.totalSeats} seats`
                                    : "N/A"}
                            </span>
                        </div>
                        <div className="flex items-center gap-3">
                            <FaUsers className="text-indigo-500" />
                            <span>
                                {ride.joinedEmpIds && ride.joinedEmpIds.length > 0
                                    ? ride.joinedEmpIds.join(", ")
                                    : <span className="text-gray-400 italic">No joined employees</span>}
                            </span>
                        </div>
                    </div>
                ))}
            </div>
        </div>
    );
}

export default ViewRides;