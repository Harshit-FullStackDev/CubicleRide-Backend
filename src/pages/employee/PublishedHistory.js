import React, { useEffect, useState } from "react";
import api from "../../api/axios";
import { FaCar, FaMapMarkerAlt, FaCalendarAlt, FaClock } from "react-icons/fa";

function PublishedHistory() {
  const [rides, setRides] = useState([]);

  useEffect(() => {
    const load = async () => {
      const res = await api.get("/ride/history/published");
      setRides(res.data);
    };
    load();
  }, []);

  const formatDate = (d) => new Date(d).toLocaleDateString();

  return (
    <div className="min-h-screen p-6 bg-gradient-to-br from-blue-50 to-blue-100">
      <h2 className="text-2xl font-bold text-blue-700 mb-4">Published Ride History</h2>
      {rides.length === 0 ? (
        <p className="text-gray-400">No past published rides.</p>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
          {rides.map(ride => (
            <div key={ride.id} className="bg-white rounded-xl shadow p-4">
              <div className="flex items-center gap-2"><FaMapMarkerAlt className="text-green-500"/> {ride.origin} → {ride.destination}</div>
              <div className="flex items-center gap-2 text-sm text-gray-600 mt-1"><FaCalendarAlt/> {formatDate(ride.date)} <FaClock className="ml-3"/> {ride.arrivalTime?.slice(0,5)}</div>
              <div className="flex items-center gap-2 text-sm text-gray-600 mt-1"><FaCar/> {ride.carDetails}</div>
              <span className="inline-block mt-2 text-xs px-2 py-1 rounded bg-gray-200 text-gray-700">{ride.status}</span>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}

export default PublishedHistory;
