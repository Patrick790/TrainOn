import React from 'react';
import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom';
import MainPage from './components/MainPage';
import HallAdmin from './components/halladmin/HallAdmin';

function App() {
    return (
        <Router>
            <div className="app">
                <Routes>
                    <Route path="/" element={<Navigate to="/user-home" />} />
                    <Route path="/user-home" element={<MainPage />} />
                    <Route path="/hall-admin-dashboard" element={<HallAdmin />} />
                    <Route path="/hall-admin-dashboard/add" element={<HallAdmin />} />
                    <Route path="/hall-admin-dashboard/schedule" element={<HallAdmin />} />
                    <Route path="/hall-admin-dashboard/reservations" element={<HallAdmin />} />
                    <Route path="/hall-admin-dashboard/reviews" element={<HallAdmin />} />
                </Routes>
            </div>
        </Router>
    );
}

export default App;