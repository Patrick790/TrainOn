import React from 'react';
import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom';
import MainPage from './components/users/MainPage';
import SportsHallDetailPage from './components/users/SportsHallDetailPage';
import SearchResultsPage from './components/users/SearchResultsPage';
import ProfileCreationPage from './components/users/ProfileCreationPage';
import HallAdmin from './components/halladmin/HallAdmin';
import EditHallPage from './components/halladmin/AdminPages/EditHallPage';
import Admin from "./components/admin/Admin";

function App() {
    return (
        <Router>
            <div className="app">
                <Routes>
                    <Route path="/" element={<Navigate to="/user-home" />} />
                    <Route path="/user-home" element={<MainPage />} />
                    <Route path="/sportsHalls/:id" element={<SportsHallDetailPage />} />
                    <Route path="/search" element={<SearchResultsPage />} />
                    <Route path="/profile-creation" element={<ProfileCreationPage />} />

                    {/* Hall Admin Routes */}
                    <Route path="/hall-admin-dashboard" element={<HallAdmin />} />
                    <Route path="/hall-admin-dashboard/add" element={<HallAdmin />} />
                    <Route path="/hall-admin-dashboard/schedule" element={<HallAdmin />} />
                    <Route path="/hall-admin-dashboard/reservations" element={<HallAdmin />} />
                    <Route path="/hall-admin-dashboard/reviews" element={<HallAdmin />} />
                    <Route path="/edit-hall/:id" element={<EditHallPage />} />

                    {/* Admin Routes */}
                    <Route path="/admin-dashboard" element={<Admin />} />
                    <Route path="/admin-dashboard/view-halls" element={<Admin />} />
                    <Route path="/admin-dashboard/approve-registrations" element={<Admin />} />
                    <Route path="/admin-dashboard/view-schedule" element={<Admin />} />
                    <Route path="/admin-dashboard/manage-users" element={<Admin />} />
                    <Route path="/admin-dashboard/send-emails" element={<Admin />} />
                </Routes>
            </div>
        </Router>
    );
}

export default App;