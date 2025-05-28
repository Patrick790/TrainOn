import React from 'react';

const SecuritySettings = () => {
    const handleChangePassword = () => {
        // TODO: Implementează logica pentru schimbarea parolei
        console.log('Schimbă parola');
    };

    const handleTwoFactorAuth = () => {
        // TODO: Implementează logica pentru autentificarea în două pași
        console.log('Configurează autentificarea în două pași');
    };

    const handleDeleteAccount = () => {
        // TODO: Implementează logica pentru ștergerea contului
        if (window.confirm('Ești sigur că vrei să ștergi permanent contul? Această acțiune nu poate fi anulată.')) {
            console.log('Șterge contul');
            // Aici ar trebui să faci un request către backend pentru ștergerea contului
        }
    };

    return (
        <div className="profile-section">
            <div className="section-header">
                <h2>Securitate</h2>
            </div>

            <div className="security-section">
                <div className="security-item">
                    <div className="security-info">
                        <h3>Schimbă parola</h3>
                        <p>Actualizează-ți parola pentru a menține contul securizat</p>
                    </div>
                    <button
                        className="security-button"
                        onClick={handleChangePassword}
                    >
                        Schimbă parola
                    </button>
                </div>

                <div className="security-item">
                    <div className="security-info">
                        <h3>Autentificare în două pași</h3>
                        <p>Adaugă un nivel suplimentar de securitate la contul tău</p>
                    </div>
                    <button
                        className="security-button"
                        onClick={handleTwoFactorAuth}
                    >
                        Configurează
                    </button>
                </div>

                <div className="security-item danger">
                    <div className="security-info">
                        <h3>Șterge contul</h3>
                        <p>Șterge permanent contul și toate datele asociate</p>
                    </div>
                    <button
                        className="security-button danger"
                        onClick={handleDeleteAccount}
                    >
                        Șterge contul
                    </button>
                </div>
            </div>
        </div>
    );
};

export default SecuritySettings;