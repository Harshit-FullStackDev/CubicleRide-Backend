import React, { useEffect, useState } from "react";
import api from "../../api/axios";
import { FaBell, FaCheckCircle } from "react-icons/fa";

function Notifications() {
  const [notifications, setNotifications] = useState([]);
  const empId = localStorage.getItem("empId");

  const load = async () => {
    try {
      const res = await api.get(`/notifications/${empId}`);
      setNotifications(res.data);
    } catch {
      setNotifications([]);
    }
  };

  useEffect(() => { load(); }, []);

  const clearOne = async (id) => {
    try {
      await api.delete(`/notifications/${id}`);
      setNotifications(notifications.filter(n => n.id !== id));
    } catch {}
  };

  return (
    <div className="min-h-screen flex justify-center items-start p-6 bg-gradient-to-br from-blue-50 to-blue-100">
      <div className="bg-white rounded-2xl shadow-xl p-6 w-full max-w-2xl">
        <div className="flex items-center gap-2 mb-4">
          <FaBell className="text-yellow-500 text-2xl" />
          <h2 className="text-xl font-bold text-blue-700">Notifications</h2>
        </div>
        {notifications.length === 0 ? (
          <p className="text-gray-400">No notifications.</p>
        ) : (
          <ul className="space-y-2">
            {notifications.map(n => (
              <li key={n.id} className="flex items-center gap-2 bg-yellow-50 rounded-lg px-3 py-2 shadow-sm">
                <FaCheckCircle className="text-green-400" /> {n.message}
                <button onClick={() => clearOne(n.id)} className="ml-auto text-red-500 hover:text-red-700">Clear</button>
              </li>
            ))}
          </ul>
        )}
      </div>
    </div>
  );
}

export default Notifications;
