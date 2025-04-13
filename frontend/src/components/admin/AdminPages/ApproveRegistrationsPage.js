import React from 'react';

const ApproveRegistrationsPage = () => {
    return (
        <div className="global-admin-page">
            <h2 className="global-admin-page-title">Aprobarea cererilor de înregistrare</h2>
            <div className="global-admin-page-content">
                <p>Aici vor fi afișate cererile de înregistrare a echipelor care necesită aprobare.</p>
                <div className="global-admin-placeholder">
                    <div>Echipa Vulturii - Cerere în așteptare</div>
                    <div>Rapid Junior - Cerere în așteptare</div>
                    <div>Club Sportiv Municipal - Cerere în așteptare</div>
                </div>
            </div>
        </div>
    );
};

export default ApproveRegistrationsPage;