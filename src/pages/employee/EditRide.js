import React, { useEffect, useState } from "react";
import { useParams, useNavigate } from "react-router-dom";
import { FaCar, FaMapMarkerAlt, FaCalendarAlt, FaUsers, FaClock } from "react-icons/fa";
import api from "../../api/axios";

function EditRide() {
    const { id } = useParams();
    const navigate = useNavigate();

    const [ride, setRide] = useState(null);
    const [locations, setLocations] = useState([]);
    const [loading, setLoading] = useState(true);
    const [errors, setErrors] = useState({});
    const [submitting, setSubmitting] = useState(false);

    // Fetch ride details
    useEffect(() => {
        const fetchRide = async () => {
            try {
                const res = await api.get(`/ride/edit/${id}`);
                setRide(res.data);
            } catch (err) {
                alert("Failed to fetch ride details.");
                navigate("/employee/dashboard");
            } finally {
                setLoading(false);
            }
        };
        fetchRide();
    }, [id, navigate]);

    // Fetch location list
    useEffect(() => {
        const fetchLocations = async () => {
            try {
                const res = await api.get("/locations");
                setLocations(res.data);
            } catch (err) {
                console.error("Failed to load locations", err);
            }
        };
        fetchLocations();
    }, []);

    const handleChange = (e) => {
        setRide({ ...ride, [e.target.name]: e.target.value });
    };

    const validate = () => {
        const errs = {};
        if (!ride.origin) errs.origin = "Origin is required";
        if (!ride.destination) errs.destination = "Destination is required";
        if (!ride.date) errs.date = "Date is required";
        if (!ride.arrivalTime) errs.arrivalTime = "Arrival time is required";
        if (!ride.carDetails) errs.carDetails = "Car details required";
        if (!ride.totalSeats || ride.totalSeats < 1) errs.totalSeats = "Total seats must be at least 1";
        if (ride.availableSeats > ride.totalSeats) errs.availableSeats = "Available seats cannot exceed total seats";
        return errs;
    };

    const handleSubmit = async (e) => {
        e.preventDefault();
        const errs = validate();
        if (Object.keys(errs).length) {
            setErrors(errs);
            return;
        }
        setSubmitting(true);
        try {
            await api.put(`/ride/edit/${id}`, ride);
            alert("Ride updated!");
            navigate("/employee/dashboard");
        } catch (err) {
            alert("Failed to update ride.");
        } finally {
            setSubmitting(false);
        }
    };

    if (loading || !ride) return (
        <div className="flex items-center justify-center h-64">
            <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-500"></div>
        </div>
    );

    return (
        <div className="min-h-screen flex items-center justify-center bg-gradient-to-br from-blue-50 to-blue-100">
            <div className="bg-white shadow-lg rounded-lg p-8 w-full max-w-2xl">
                <h2 className="text-3xl font-bold mb-6 text-blue-700 flex items-center gap-2">
                    <FaCar className="text-blue-500" /> Edit Ride
                </h2>
                <form onSubmit={handleSubmit} className="space-y-5">
                    <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                        <div>
                            <label className="block mb-1 font-medium flex items-center gap-1">
                                <FaMapMarkerAlt /> Origin
                            </label>
                            <select
                                name="origin"
                                value={ride.origin}
                                onChange={handleChange}
                                className="border p-2 w-full rounded"
                            >
                                <option value="">Select Origin</option>
                                {locations.map((loc) => (
                                    <option key={loc.id} value={loc.name}>{loc.name}</option>
                                ))}
                            </select>
                            {errors.origin && <span className="text-red-500 text-xs">{errors.origin}</span>}
                        </div>

                        <div>
                            <label className="block mb-1 font-medium flex items-center gap-1">
                                <FaMapMarkerAlt /> Destination
                            </label>
                            <select
                                name="destination"
                                value={ride.destination}
                                onChange={handleChange}
                                className="border p-2 w-full rounded"
                            >
                                <option value="">Select Destination</option>
                                {locations.map((loc) => (
                                    <option key={loc.id} value={loc.name}>{loc.name}</option>
                                ))}
                            </select>
                            {errors.destination && <span className="text-red-500 text-xs">{errors.destination}</span>}
                        </div>

                        <div>
                            <label className="block mb-1 font-medium flex items-center gap-1">
                                <FaCalendarAlt /> Date
                            </label>
                            <input
                                name="date"
                                type="date"
                                value={ride.date}
                                onChange={handleChange}
                                className="border p-2 w-full rounded"
                            />
                            {errors.date && <span className="text-red-500 text-xs">{errors.date}</span>}
                        </div>

                        <div>
                            <label className="block mb-1 font-medium flex items-center gap-1">
                                <FaClock /> Arrival Time
                            </label>
                            <input
                                name="arrivalTime"
                                type="time"
                                value={ride.arrivalTime}
                                onChange={handleChange}
                                className="border p-2 w-full rounded"
                            />
                            {errors.arrivalTime && <span className="text-red-500 text-xs">{errors.arrivalTime}</span>}
                        </div>

                        <div>
                            <label className="block mb-1 font-medium flex items-center gap-1">
                                <FaCar /> Car Details
                            </label>
                            <input
                                name="carDetails"
                                value={ride.carDetails}
                                onChange={handleChange}
                                className="border p-2 w-full rounded"
                                placeholder="Car Details"
                            />
                            {errors.carDetails && <span className="text-red-500 text-xs">{errors.carDetails}</span>}
                        </div>

                        <div>
                            <label className="block mb-1 font-medium flex items-center gap-1">
                                <FaUsers /> Total Seats
                            </label>
                            <input
                                name="totalSeats"
                                type="number"
                                min={1}
                                max={8}
                                value={ride.totalSeats}
                                onChange={handleChange}
                                className="border p-2 w-full rounded"
                                placeholder="Total Seats"
                            />
                            {errors.totalSeats && <span className="text-red-500 text-xs">{errors.totalSeats}</span>}
                        </div>

                        <div>
                            <label className="block mb-1 font-medium flex items-center gap-1">
                                <FaUsers /> Available Seats
                            </label>
                            <input
                                name="availableSeats"
                                type="number"
                                min={0}
                                max={ride.totalSeats}
                                value={ride.availableSeats}
                                onChange={handleChange}
                                className="border p-2 w-full rounded"
                                placeholder="Available Seats"
                            />
                            {errors.availableSeats && <span className="text-red-500 text-xs">{errors.availableSeats}</span>}
                        </div>
                    </div>

                    <div className="flex gap-4 mt-6">
                        <button
                            type="submit"
                            disabled={submitting}
                            className={`bg-blue-600 hover:bg-blue-700 text-white px-6 py-2 rounded font-semibold transition ${submitting ? "opacity-60 cursor-not-allowed" : ""}`}
                        >
                            {submitting ? "Saving..." : "Save Changes"}
                        </button>
                        <button
                            type="button"
                            onClick={() => navigate("/employee/dashboard")}
                            className="bg-gray-200 hover:bg-gray-300 text-gray-700 px-6 py-2 rounded font-semibold transition"
                        >
                            Cancel
                        </button>
                    </div>
                </form>
            </div>
        </div>
    );
}

export default EditRide;
