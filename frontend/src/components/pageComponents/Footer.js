import React, { useState } from 'react';
import './Footer.css';
import ContactModal from './ContactModal';

const Footer = () => {
    const currentYear = new Date().getFullYear();
    const [isContactModalOpen, setIsContactModalOpen] = useState(false);

    const openContactModal = (e) => {
        e.preventDefault();
        setIsContactModalOpen(true);
    };

    const closeContactModal = () => {
        setIsContactModalOpen(false);
    };

    return (
        <footer className="global-footer">
            <div className="global-footer-content">
                <div className="global-footer-logo">
                    <div className="global-footer-logo-icon"></div>
                    <span className="global-footer-logo-text">TrainOn</span>
                </div>
                <div className="global-footer-links">
                    <a href="/terms" className="global-footer-link">Termeni și condiții</a>
                    <a href="/privacy" className="global-footer-link">Politica de confidențialitate</a>
                    <a href="#" onClick={openContactModal} className="global-footer-link">Contact</a>
                </div>
                <div className="global-footer-copyright">
                    &copy; {currentYear} Platforma de Administrare
                </div>
            </div>

            <ContactModal
                isOpen={isContactModalOpen}
                onClose={closeContactModal}
            />
        </footer>
    );
};

export default Footer;