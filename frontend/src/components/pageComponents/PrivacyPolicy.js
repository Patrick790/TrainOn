import React from 'react';
import { Link } from 'react-router-dom';
import './PrivacyPolicy.css';
import Footer from './Footer';

const PrivacyPolicy = () => {
    return (
        <div className="privacy-page">
            <header className="privacy-header">
                <div className="privacy-header-content">
                    <Link to="/" className="privacy-logo-link">
                        <div className="privacy-logo-icon"></div>
                        <span className="privacy-logo-text">TrainOn</span>
                    </Link>
                    <Link to="/" className="privacy-back-button">
                        Înapoi la pagina principală
                    </Link>
                </div>
            </header>

            <main className="privacy-content">
                <div className="privacy-container">
                    <h1 className="privacy-title">Politica de Confidențialitate</h1>
                    <p className="privacy-last-updated">Ultima actualizare: 8 Mai, 2025</p>

                    <section className="privacy-section">
                        <h2>1. Introducere</h2>
                        <p>Bine ați venit la TrainOn. Confidențialitatea datelor dumneavoastră personale este o prioritate pentru noi. Această Politică de Confidențialitate explică modul în care colectăm, utilizăm, divulgăm și protejăm informațiile personale atunci când utilizați platforma noastră de rezervare a sălilor de sport.</p>
                        <p>Vă rugăm să citiți cu atenție această politică pentru a înțelege cum tratăm datele dumneavoastră.</p>
                    </section>

                    <section className="privacy-section">
                        <h2>2. Informațiile pe care le colectăm</h2>
                        <h3>2.1 Informații personale</h3>
                        <p>Putem colecta următoarele informații personale:</p>
                        <ul>
                            <li>Nume și prenume</li>
                            <li>Adresă de email</li>
                            <li>Număr de telefon</li>
                            <li>Data nașterii</li>
                            <li>Informații despre contul dumneavoastră</li>
                            <li>Istoricul rezervărilor</li>
                            <li>Preferințele legate de activitățile sportive</li>
                        </ul>

                        <h3>2.2 Informații colectate automat</h3>
                        <p>Atunci când utilizați platforma noastră, anumite informații sunt colectate automat, inclusiv:</p>
                        <ul>
                            <li>Adresa IP</li>
                            <li>Tipul dispozitivului</li>
                            <li>Tipul și versiunea browserului</li>
                            <li>Sistemul de operare</li>
                            <li>Date despre utilizarea platformei</li>
                            <li>Cookie-uri și tehnologii similare</li>
                        </ul>
                    </section>

                    <section className="privacy-section">
                        <h2>3. Cum utilizăm informațiile dumneavoastră</h2>
                        <p>Utilizăm informațiile personale colectate pentru:</p>
                        <ul>
                            <li>Facilitarea și procesarea rezervărilor de săli de sport</li>
                            <li>Crearea și gestionarea contului dumneavoastră</li>
                            <li>Îmbunătățirea serviciilor noastre</li>
                            <li>Comunicarea cu dumneavoastră despre rezervări, actualizări sau modificări</li>
                            <li>Trimiterea de notificări legate de serviciile noastre</li>
                            <li>Prevenirea fraudelor și asigurarea securității platformei</li>
                            <li>Respectarea obligațiilor legale</li>
                        </ul>
                    </section>

                    <section className="privacy-section">
                        <h2>4. Divulgarea informațiilor</h2>
                        <p>Putem divulga informațiile dumneavoastră personale către:</p>
                        <ul>
                            <li>Administratorii sălilor de sport pentru facilitarea rezervărilor</li>
                            <li>Furnizori de servicii terțe care ne ajută să operăm platforma</li>
                            <li>Autorități publice, când este cerut de lege</li>
                            <li>Parteneri de afaceri, cu consimțământul dumneavoastră explicit</li>
                        </ul>
                        <p>Nu vom vinde, închiria sau distribui informațiile dumneavoastră personale către terți fără consimțământul dumneavoastră, cu excepția cazurilor prevăzute în această politică.</p>
                    </section>

                    <section className="privacy-section">
                        <h2>5. Securitatea datelor</h2>
                        <p>Implementăm măsuri de securitate tehnice și organizaționale pentru a proteja informațiile dumneavoastră personale împotriva accesului neautorizat, pierderii sau modificării. Aceste măsuri includ criptarea datelor, accesul limitat la informații și instruirea personalului.</p>
                        <p>Cu toate acestea, nicio metodă de transmitere sau stocare electronică nu este 100% sigură. Prin urmare, nu putem garanta securitatea absolută a datelor dumneavoastră.</p>
                    </section>

                    <section className="privacy-section">
                        <h2>6. Drepturile dumneavoastră</h2>
                        <p>În conformitate cu legislația aplicabilă privind protecția datelor, aveți următoarele drepturi:</p>
                        <ul>
                            <li>Dreptul de acces la datele personale</li>
                            <li>Dreptul la rectificarea datelor inexacte</li>
                            <li>Dreptul la ștergerea datelor ("dreptul de a fi uitat")</li>
                            <li>Dreptul la restricționarea prelucrării</li>
                            <li>Dreptul la portabilitatea datelor</li>
                            <li>Dreptul de a vă opune prelucrării</li>
                            <li>Dreptul de a nu face obiectul unei decizii bazate exclusiv pe prelucrarea automată</li>
                        </ul>
                        <p>Pentru a vă exercita aceste drepturi, vă rugăm să ne contactați folosind informațiile de contact furnizate la sfârșitul acestei politici.</p>
                    </section>

                    <section className="privacy-section">
                        <h2>7. Cookie-uri</h2>
                        <p>Utilizăm cookie-uri și tehnologii similare pentru a îmbunătăți experiența dumneavoastră pe platformă. Puteți gestiona preferințele pentru cookie-uri prin setările browserului dumneavoastră. Pentru mai multe informații despre cookie-urile pe care le folosim, consultați Politica noastră privind Cookie-urile.</p>
                    </section>

                    <section className="privacy-section">
                        <h2>8. Modificări ale Politicii de Confidențialitate</h2>
                        <p>Ne rezervăm dreptul de a modifica această Politică de Confidențialitate în orice moment. Orice modificări vor fi postate pe această pagină, iar data "Ultima actualizare" va fi revizuită. Vă încurajăm să revizuiți periodic această politică pentru a fi informat despre cum protejăm informațiile dumneavoastră.</p>
                    </section>

                    <section className="privacy-section">
                        <h2>9. Contact</h2>
                        <p>Dacă aveți întrebări sau preocupări legate de această Politică de Confidențialitate sau de practicile noastre privind datele, vă rugăm să ne contactați:</p>
                        <div className="privacy-contact-info">
                            <p><strong>Email:</strong> trainon.application@gmail.com</p>
                            <p><strong>Telefon:</strong> +40 354 662 177</p>
                            <p><strong>Adresă:</strong> Strada Universității 7-9, Cluj-Napoca, România</p>
                        </div>
                    </section>
                </div>
            </main>

            <Footer />
        </div>
    );
};

export default PrivacyPolicy;