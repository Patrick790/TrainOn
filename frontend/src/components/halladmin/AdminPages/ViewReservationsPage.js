import React, { Component } from 'react';

class ViewReservationsPage extends Component {
    render() {
        return (
            <div className="admin-page">
                <h1 className="admin-page-title">Vizualizare Rezervări</h1>
                <div className="reservations-list">
                    <p>Aici vor fi afișate rezervările.</p>
                    {/* Lista de rezervări care va fi implementată */}
                </div>
            </div>
        );
    }
}

export default ViewReservationsPage;