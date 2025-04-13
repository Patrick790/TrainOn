import React from 'react';

const SendEmailsPage = () => {
    return (
        <div className="global-admin-page">
            <h2 className="global-admin-page-title">Trimiterea email-urilor</h2>
            <div className="global-admin-page-content">
                <p>Aici va fi implementată funcționalitatea de trimitere a email-urilor către utilizatori și administratori.</p>
                <div className="global-admin-placeholder">
                    <div style={{ marginBottom: '20px' }}>
                        <label>Destinatar:</label>
                        <select style={{ margin: '0 10px' }}>
                            <option>Toți utilizatorii</option>
                            <option>Toți administratorii</option>
                            <option>Utilizator specific</option>
                        </select>
                    </div>
                    <div style={{ marginBottom: '20px' }}>
                        <label>Subiect:</label>
                        <input type="text" style={{ margin: '0 10px', width: '300px' }} placeholder="Introduceti subiectul email-ului" />
                    </div>
                    <div style={{ marginBottom: '20px' }}>
                        <label>Conținut:</label>
                        <textarea style={{ display: 'block', margin: '10px 0', width: '400px', height: '150px' }} placeholder="Introduceti continutul email-ului"></textarea>
                    </div>
                    <button style={{ padding: '8px 15px', background: '#6d28d9', color: 'white', border: 'none', borderRadius: '4px' }}>Trimite email</button>
                </div>
            </div>
        </div>
    );
};

export default SendEmailsPage;