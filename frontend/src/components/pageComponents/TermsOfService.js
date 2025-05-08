import React from 'react';
import { Link } from 'react-router-dom';
import './TermsOfService.css';
import Footer from './Footer';

const TermsOfService = () => {
    return (
        <div className="terms-page">
            <header className="terms-header">
                <div className="terms-header-content">
                    <Link to="/" className="terms-logo-link">
                        <div className="terms-logo-icon"></div>
                        <span className="terms-logo-text">TrainOn</span>
                    </Link>
                    <Link to="/" className="terms-back-button">
                        Înapoi la pagina principală
                    </Link>
                </div>
            </header>

            <main className="terms-content">
                <div className="terms-container">
                    <h1 className="terms-title">Termeni și Condiții</h1>
                    <p className="terms-last-updated">Ultima actualizare: 8 Mai, 2025</p>

                    <section className="terms-section">
                        <h2>1. Introducere</h2>
                        <p>Acești Termeni și Condiții ("Termeni") guvernează utilizarea de către dumneavoastră a platformei TrainOn ("Platforma"), inclusiv a site-ului web și a aplicației mobile, precum și a serviciilor asociate. Accesând sau utilizând Platforma noastră, sunteți de acord să respectați acești Termeni și orice documente incluse prin referință.</p>
                        <p>Vă rugăm să citiți cu atenție acești Termeni înainte de a utiliza Platforma noastră.</p>
                    </section>

                    <section className="terms-section">
                        <h2>2. Acceptarea Termenilor</h2>
                        <p>Prin accesarea sau utilizarea Platformei, confirmați că ați citit, înțelegeți și sunteți de acord să fiți legat de acești Termeni. Dacă nu sunteți de acord cu toți Termenii sau nu puteți respecta acești Termeni, nu trebuie să accesați sau să utilizați Platforma.</p>
                    </section>

                    <section className="terms-section">
                        <h2>3. Definițiile Termenilor</h2>
                        <p>În acești Termeni, următorii termeni vor avea următoarele semnificații:</p>
                        <ul>
                            <li><strong>"Noi", "Nouă"</strong> sau <strong>"TrainOn"</strong> se referă la platforma TrainOn și la operatorii acesteia.</li>
                            <li><strong>"Utilizator"</strong> se referă la orice persoană care accesează sau utilizează Platforma.</li>
                            <li><strong>"Cont"</strong> se referă la înregistrarea dvs. pe Platformă.</li>
                            <li><strong>"Servicii"</strong> se referă la toate serviciile furnizate de TrainOn prin intermediul Platformei.</li>
                            <li><strong>"Sală de sport"</strong> se referă la facilitățile disponibile pentru rezervare prin intermediul Platformei.</li>
                            <li><strong>"Administrator"</strong> se referă la o persoană sau entitate care administrează o sală de sport listată pe Platformă.</li>
                        </ul>
                    </section>

                    <section className="terms-section">
                        <h2>4. Utilizarea Platformei</h2>
                        <h3>4.1 Eligibilitate</h3>
                        <p>Pentru a utiliza Platforma, trebuie să aveți cel puțin 18 ani sau vârsta legală a majoratului în jurisdicția dumneavoastră (oricare este mai mare). Dacă accesați sau utilizați Platforma în numele unei entități juridice, declarați și garantați că aveți autoritatea de a lega acea entitate de acești Termeni.</p>

                        <h3>4.2 Înregistrare și Conturi</h3>
                        <p>Anumite funcții ale Platformei necesită crearea unui cont. Când creați un cont, trebuie să furnizați informații corecte, actuale și complete. Sunteți responsabil pentru menținerea confidențialității credențialelor contului dvs. și pentru toate activitățile care au loc sub contul dvs. Trebuie să ne notificați imediat despre orice utilizare neautorizată a contului dvs.</p>

                        <h3>4.3 Comportament Utilizator</h3>
                        <p>Când utilizați Platforma, sunteți de acord să:</p>
                        <ul>
                            <li>Respectați toate legile și reglementările aplicabile</li>
                            <li>Nu încălcați niciun drept de proprietate intelectuală sau alte drepturi</li>
                            <li>Nu postați sau transmiteți conținut ilegal, dăunător, defăimător, obscen sau ofensator</li>
                            <li>Nu interferați sau nu încercați să interferați cu funcționarea Platformei</li>
                            <li>Nu încercați să obțineți acces neautorizat la nicio parte a Platformei</li>
                            <li>Nu utilizați Platforma pentru a trimite spam, virus sau alte coduri malițioase</li>
                        </ul>
                    </section>

                    <section className="terms-section">
                        <h2>5. Rezervările</h2>
                        <h3>5.1 Efectuarea Rezervărilor</h3>
                        <p>Platforma TrainOn vă permite să efectuați rezervări la sălile de sport disponibile. Când efectuați o rezervare, sunteți de acord să:</p>
                        <ul>
                            <li>Furnizați informații corecte și complete despre rezervarea dumneavoastră</li>
                            <li>Respectați regulile și politicile specifice sălii de sport</li>
                            <li>Plătiți toate taxele asociate cu rezervarea dumneavoastră</li>
                        </ul>

                        <h3>5.2 Anularea și Rambursările</h3>
                        <p>Politicile de anulare pot varia în funcție de sala de sport. Vă rugăm să verificați politicile specifice înainte de a efectua o rezervare. În general:</p>
                        <ul>
                            <li>Anulările efectuate cu cel puțin 24 de ore înainte de timpul rezervat pot fi eligibile pentru o rambursare completă sau parțială</li>
                            <li>Anulările efectuate cu mai puțin de 24 de ore înainte de timpul rezervat pot fi supuse unor taxe de anulare</li>
                            <li>Ne rezervăm dreptul de a anula rezervările în cazul unor circumstanțe excepționale, cu rambursare completă</li>
                        </ul>
                    </section>

                    <section className="terms-section">
                        <h2>6. Plăți și Taxe</h2>
                        <p>Utilizarea anumitor servicii poate necesita plata unor taxe. Toate taxele sunt afișate în lei românești (RON) și includ TVA-ul aplicabil.</p>
                        <p>Prin efectuarea unei plăți, autorizați TrainOn sau procesatorii noștri de plăți să perceapă taxa specificată folosind metoda de plată pe care ați ales-o. Ne rezervăm dreptul de a modifica prețurile în orice moment, cu notificare prealabilă.</p>
                    </section>

                    <section className="terms-section">
                        <h2>7. Proprietatea Intelectuală</h2>
                        <p>Platforma și tot conținutul, caracteristicile și funcționalitatea acesteia (inclusiv, dar fără a se limita la text, grafice, logo-uri, imagini și software) sunt deținute de TrainOn sau de licențiatorii săi și sunt protejate de legi privind drepturile de autor, mărci comerciale, brevete și alte legi de proprietate intelectuală.</p>
                        <p>Nu aveți permisiunea să reproduceți, distribuiți, modificați, creați lucrări derivate, afișați public, efectuați public, republicați, descărcați, stocați sau transmiteți orice material de pe Platformă, cu excepția cazului în care este permis în mod explicit în acești Termeni.</p>
                    </section>

                    <section className="terms-section">
                        <h2>8. Confidențialitate</h2>
                        <p>Utilizarea Platformei este, de asemenea, guvernată de Politica noastră de Confidențialitate, care poate fi găsită <Link to="/privacy" className="terms-link">aici</Link>. Prin utilizarea Platformei, sunteți de acord cu colectarea, utilizarea și divulgarea informațiilor dumneavoastră așa cum este descris în Politica de Confidențialitate.</p>
                    </section>

                    <section className="terms-section">
                        <h2>9. Limitarea Răspunderii</h2>
                        <p>În măsura permisă de lege, TrainOn nu va fi răspunzător pentru nicio daună indirectă, incidentală, specială, consecventă sau punitivă, sau pentru orice pierdere de profit sau venit, indiferent dacă este cauzată de delict (inclusiv neglijență), încălcarea contractului sau altfel, chiar dacă este previzibilă.</p>
                        <p>Nu garantăm că Platforma va fi sigură sau lipsită de erori sau viruși. Sunteți responsabil pentru configurarea tehnologiei informaționale, programelor de calculator și platformei pentru a accesa site-ul nostru. Ar trebui să utilizați propriul software de protecție împotriva virușilor.</p>
                    </section>

                    <section className="terms-section">
                        <h2>10. Despăgubire</h2>
                        <p>Sunteți de acord să ne despăgubiți, să ne apărați și să ne protejați pe noi și pe licențiatorii, afiliații, partenerii și furnizorii noștri, și pe fiecare dintre ofițerii, directorii, angajații, agenții și contractorii lor respectivi, de și împotriva oricăror reclamații, pierderi, daune, responsabilități, costuri și cheltuieli (inclusiv, dar fără a se limita la, onorariile și costurile rezonabile ale avocaților) care rezultă din sau în legătură cu încălcarea de către dumneavoastră a acestor Termeni sau utilizarea necorespunzătoare a Platformei.</p>
                    </section>

                    <section className="terms-section">
                        <h2>11. Rezilierea</h2>
                        <p>Ne rezervăm dreptul, la discreția noastră, de a restricționa, suspenda sau rezilia accesul dumneavoastră la toate sau la orice parte a Platformei, cu sau fără notificare, pentru orice motiv sau fără motiv, inclusiv, dar fără a se limita la, încălcarea acestor Termeni.</p>
                        <p>După reziliere, dreptul dumneavoastră de a utiliza Platforma va înceta imediat. Toate prevederile din acești Termeni care, prin natura lor, ar trebui să supraviețuiască rezilierii, vor supraviețui rezilierii.</p>
                    </section>

                    <section className="terms-section">
                        <h2>12. Modificări ale Termenilor</h2>
                        <p>Ne rezervăm dreptul, la discreția noastră, de a modifica sau înlocui acești Termeni în orice moment. Dacă o revizuire este semnificativă, vom depune eforturi rezonabile pentru a oferi o notificare cu cel puțin 30 de zile înainte ca noii termeni să intre în vigoare. Ceea ce constituie o schimbare semnificativă va fi determinat la discreția noastră.</p>
                        <p>Continuarea utilizării Platformei după orice astfel de modificări constituie acceptarea noilor Termeni. Dacă nu sunteți de acord cu noii termeni, vă rugăm să încetați utilizarea Platformei.</p>
                    </section>

                    <section className="terms-section">
                        <h2>13. Legea Aplicabilă</h2>
                        <p>Acești Termeni vor fi guvernați și interpretați în conformitate cu legile României, fără a ține cont de principiile conflictului de legi.</p>
                        <p>Orice dispută, controversă sau pretenție care decurge din sau în legătură cu acești Termeni sau încălcarea, rezilierea sau invaliditatea acestora va fi soluționată definitiv de instanțele competente din România.</p>
                    </section>

                    <section className="terms-section">
                        <h2>14. Contact</h2>
                        <p>Dacă aveți întrebări sau preocupări cu privire la acești Termeni, vă rugăm să ne contactați:</p>
                        <div className="terms-contact-info">
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

export default TermsOfService;