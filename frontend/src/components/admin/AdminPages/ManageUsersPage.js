import React from 'react';

const ManageUsersPage = () => {
    return (
        <div className="global-admin-page">
            <h2 className="global-admin-page-title">Gestiunea utilizatorilor</h2>
            <div className="global-admin-page-content">
                <p>Aici va fi implementată funcționalitatea de gestiune a utilizatorilor platformei.</p>
                <div className="global-admin-placeholder">
                    <div>Administrator Sala 1 - Activ</div>
                    <div>Utilizator Regular - Activ</div>
                    <div>Administrator Sala 2 - Suspendat</div>
                </div>
            </div>
        </div>
    );
};

export default ManageUsersPage;