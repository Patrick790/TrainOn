import React from 'react';
import './ContactModal.css';

const ContactModal = ({ isOpen, onClose }) => {
    if (!isOpen) return null;

    return (
        <div className="contact-modal-overlay" onClick={onClose}>
            <div className="contact-modal" onClick={(e) => e.stopPropagation()}>
                <div className="contact-modal-header">
                    <h2>Contactează-ne</h2>
                    <button className="contact-modal-close" onClick={onClose}>×</button>
                </div>
                <div className="contact-modal-body">
                    <div className="contact-info-item">
                        <div className="contact-icon email-icon">✉️</div>
                        <div className="contact-details">
                            <h3>Email</h3>
                            <a href="mailto:trainon.application@gmail.com">trainon.application@gmail.com</a>
                        </div>
                    </div>

                    <div className="contact-info-item">
                        <div className="contact-icon phone-icon">📞</div>
                        <div className="contact-details">
                            <h3>Telefon</h3>
                            <a href="tel:+40354662177">+40 354 662 177</a>
                        </div>
                    </div>

                    <div className="contact-info-item">
                        <div className="contact-icon address-icon">📍</div>
                        <div className="contact-details">
                            <h3>Adresă</h3>
                            <p>Strada Universității 7-9, Cluj-Napoca, România</p>
                        </div>
                    </div>

                    <div className="contact-info-item">
                        <div className="contact-icon hours-icon">🕒</div>
                        <div className="contact-details">
                            <h3>Program</h3>
                            <p>Luni - Vineri: 9:00 - 18:00</p>
                            <p>Sâmbătă: 10:00 - 14:00</p>
                            <p>Duminică: Închis</p>
                        </div>
                    </div>
                </div>
                <div className="contact-modal-footer">
                    <p>Echipa TrainOn îți stă la dispoziție pentru orice întrebare!</p>
                </div>
            </div>
        </div>
    );
};

export default ContactModal;