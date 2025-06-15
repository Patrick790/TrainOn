import React from 'react';
import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom';
import MainPage from './components/users/MainPage';
import SportsHallDetailPage from './components/users/SportsHallDetailPage';
import SearchResultsPage from './components/users/SearchResultsPage';
import ProfileCreationPage from './components/users/ProfileCreationPage';
import ProfilePage from './components/users/profile/ProfilePage';
import ReservationsPage from "./components/users/reservationsView/ReservationsPage";
import FCFSReservationPage from './components/users/FCFSReservationPage';
import StripePaymentPage from './components/users/StripePaymentPage';
import StripePaymentSuccessPage from './components/users/StripePaymentSuccessPage';
import HallAdmin from './components/halladmin/HallAdmin';
import EditHallPage from './components/halladmin/AdminPages/EditHallPage';
import AdminProfilePage from './components/halladmin/AdminPages/profile/AdminProfilePage';
import Admin from "./components/admin/Admin";
import PrivacyPolicy from './components/pageComponents/PrivacyPolicy';
import TermsOfService from './components/pageComponents/TermsOfService';
import HallReviews from './components/feedback/HallReviews';
import AppAdminProfilePage from "./components/admin/AdminPages/profile/AppAdminProfilePage";
import ResetPasswordPage from './components/login/ResetPasswordPage';

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
                    <Route path="/profile" element={<ProfilePage />} />
                    <Route path="/reservations" element={<ReservationsPage />} />
                    <Route path="/fcfs-reservation" element={<FCFSReservationPage />} />
                    <Route path="/sportsHalls/:hallId/reviews" element={<HallReviews />} />

                    {/* Ruta pentru resetarea parolei */}
                    <Route path="/reset-password" element={<ResetPasswordPage />} />

                    {/* Payment Routes */}
                    <Route path="/payment" element={<StripePaymentPage />} />
                    <Route path="/payment-success" element={<StripePaymentSuccessPage />} />
                    <Route path="/payment-cancel" element={<StripePaymentSuccessPage />} />

                    {/* Legal Pages */}
                    <Route path="/privacy" element={<PrivacyPolicy />} />
                    <Route path="/terms" element={<TermsOfService />} />

                    {/* Hall Admin Routes */}
                    <Route path="/hall-admin-dashboard" element={<HallAdmin />} />
                    <Route path="/hall-admin-dashboard/add" element={<HallAdmin />} />
                    <Route path="/hall-admin-dashboard/schedule" element={<HallAdmin />} />
                    <Route path="/hall-admin-dashboard/reservations" element={<HallAdmin />} />
                    <Route path="/hall-admin-dashboard/reviews" element={<HallAdmin />} />
                    <Route path="/edit-hall/:id" element={<EditHallPage />} />
                    <Route path="/admin-profile" element={<AdminProfilePage />} />

                    {/* Admin Routes */}
                    <Route path="/admin-dashboard" element={<Admin />} />
                    <Route path="/admin-dashboard/view-halls" element={<Admin />} />
                    <Route path="/admin-dashboard/approve-registrations" element={<Admin />} />
                    <Route path="/admin-dashboard/view-schedule" element={<Admin />} />
                    <Route path="/admin-dashboard/manage-users" element={<Admin />} />
                    <Route path="/admin-dashboard/send-emails" element={<Admin />} />
                    <Route path="/app-admin-profile" element={<AppAdminProfilePage />} />

                </Routes>
            </div>
        </Router>
    );
}

export default App;