import React, { Component } from 'react';
import { Star } from 'lucide-react';
import './MyHallsPage.css';
// Importă imaginea direct în componentă
import lpsImage from './lps.jpg'; // Asigură-te că numele fișierului este corect

class MyHallsPage extends Component {
    constructor(props) {
        super(props);
        this.state = {
            halls: [
                {
                    id: 1,
                    name: 'Sala Liceul Cu Program Sportiv',
                    location: 'Cluj-Napoca',
                    status: 'active',
                    hasImage: true,
                    image: lpsImage,
                    rating: 4.5,
                    reviewCount: 32,
                    description: 'Sală de sport modernă cu echipamente de ultimă generație.'
                },
                {
                    id: 2,
                    name: 'Bazin Olimpic',
                    location: 'Cluj-Napoca',
                    status: 'active',
                    hasImage: false,
                    rating: 4.2,
                    reviewCount: 18,
                    description: 'Bazin olimpic cu 8 culoare și facilități complete.'
                },
                {
                    id: 3,
                    name: 'Teren Tenis Central',
                    location: 'Cluj-Napoca',
                    status: 'maintenance',
                    hasImage: false,
                    rating: 4.7,
                    reviewCount: 24,
                    description: 'Teren de tenis cu suprafață hard, ideal pentru competiții.'
                }
            ],
            apiBaseUrl: 'http://localhost:8080/api'
        };
    }

    fetchHalls = async () => {
        try {
            const token = localStorage.getItem('authToken');
            const response = await fetch(`${this.state.apiBaseUrl}/halls/my-halls`, {
                headers: {
                    'Authorization': `Bearer ${token}`
                }
            });

            if (response.ok) {
                const data = await response.json();
                this.setState({ halls: data });
            }
        } catch (error) {
            console.error('Error fetching halls:', error);
        }
    }

    componentDidMount() {
        // Uncomment when backend is ready
        // this.fetchHalls();
    }

    renderRatingStars = (rating) => {
        // Numărul total de stele este 5
        const totalStars = 5;
        const fullStars = Math.floor(rating);
        const hasHalfStar = rating % 1 >= 0.3 && rating % 1 <= 0.7;
        const hasAlmostFullStar = rating % 1 > 0.7;

        return (
            <div className="hall-rating-stars">
                {[...Array(totalStars)].map((_, index) => {
                    // Pentru stele complete
                    if (index < fullStars) {
                        return (
                            <Star
                                key={index}
                                size={18}
                                fill="#FFD700"
                                color="#FFD700"
                                className="star-icon"
                            />
                        );
                    }
                    // Pentru jumătate de stea
                    else if (index === fullStars && hasHalfStar) {
                        return (
                            <div className="half-star-container" key={index}>
                                <Star
                                    size={18}
                                    fill="#FFD700"
                                    color="#FFD700"
                                    className="half-star"
                                />
                                <Star
                                    size={18}
                                    fill="none"
                                    color="#D1D5DB"
                                    className="star-icon star-background"
                                />
                            </div>
                        );
                    }
                    // Pentru aproape o stea completă (peste 0.7)
                    else if (index === fullStars && hasAlmostFullStar) {
                        return (
                            <Star
                                key={index}
                                size={18}
                                fill="#FFD700"
                                color="#FFD700"
                                className="star-icon"
                            />
                        );
                    }
                    // Pentru stele goale
                    else {
                        return (
                            <Star
                                key={index}
                                size={18}
                                fill="none"
                                color="#D1D5DB"
                                className="star-icon"
                            />
                        );
                    }
                })}
            </div>
        );
    }

    render() {
        const { halls } = this.state;

        return (
            <div className="admin-page">
                <h1 className="admin-page-title">Sălile Mele</h1>

                <div className="halls-grid">
                    {halls.length > 0 ? (
                        halls.map(hall => (
                            <div key={hall.id} className="hall-card">
                                <div className="hall-image-container">
                                    <div className={`hall-status-indicator ${hall.status}`}>
                                        {hall.status === 'active' ? 'Activ' : 'Mentenanță'}
                                    </div>

                                    {hall.hasImage && hall.image ? (
                                        <img
                                            src={hall.image}
                                            alt={hall.name}
                                            className="hall-image"
                                        />
                                    ) : (
                                        <div className="hall-placeholder">
                                            <div className="hall-initial">{hall.name.charAt(0)}</div>
                                        </div>
                                    )}
                                </div>

                                <div className="hall-content">
                                    <h3 className="hall-name">{hall.name}</h3>
                                    <p className="hall-location">{hall.location}</p>

                                    <p className="hall-description">{hall.description}</p>

                                    <div className="hall-rating">
                                        {this.renderRatingStars(hall.rating)}
                                        <span className="rating-text">
                                            {hall.rating.toFixed(1)} ({hall.reviewCount} recenzii)
                                        </span>
                                    </div>

                                    <div className="hall-actions">
                                        <button className="hall-action-button">Programare</button>
                                        <button className="hall-action-button">Rezervări</button>
                                        <button className="hall-action-button edit">Editare</button>
                                    </div>
                                </div>
                            </div>
                        ))
                    ) : (
                        <div className="empty-state">
                            <p>Nu aveți săli de sport adăugate. Adăugați prima sală folosind butonul "Adăugare".</p>
                        </div>
                    )}
                </div>
            </div>
        );
    }
}

export default MyHallsPage;