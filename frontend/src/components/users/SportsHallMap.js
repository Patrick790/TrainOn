import React, { useEffect, useState } from 'react';
import './SportsHallMap.css';

const SportsHallMap = ({ address, city, county }) => {
    const [mapUrl, setMapUrl] = useState('');
    const [isLoading, setIsLoading] = useState(true);
    const [error, setError] = useState(null);

    useEffect(() => {
        if (!address || !city) {
            setError('Adresa sau orașul lipsește');
            setIsLoading(false);
            return;
        }

        try {
            const fullAddress = `${address}, ${city}${county ? ', ' + county : ''}, Romania`;

            // Folosim Nominatim pentru a obține coordonatele (lat/lon) pe baza adresei
            // Nominatim este serviciul OpenStreetMap pentru geocoding
            fetch(`https://nominatim.openstreetmap.org/search?format=json&q=${encodeURIComponent(fullAddress)}`)
                .then(response => response.json())
                .then(data => {
                    if (data && data.length > 0) {
                        const lat = data[0].lat;
                        const lon = data[0].lon;

                        const openStreetMapUrl = `https://www.openstreetmap.org/export/embed.html?bbox=${parseFloat(lon)-0.01},${parseFloat(lat)-0.01},${parseFloat(lon)+0.01},${parseFloat(lat)+0.01}&layer=mapnik&marker=${lat},${lon}`;

                        setMapUrl(openStreetMapUrl);
                    } else {
                        const openStreetMapUrl = `https://www.openstreetmap.org/export/embed.html?bbox=20.0,44.0,30.0,48.0&layer=mapnik&query=${encodeURIComponent(fullAddress)}`;
                        setMapUrl(openStreetMapUrl);
                    }
                    setIsLoading(false);
                })
                .catch(err => {
                    console.error("Eroare în geocoding:", err);
                    const openStreetMapUrl = `https://www.openstreetmap.org/export/embed.html?bbox=20.0,44.0,30.0,48.0&layer=mapnik&query=${encodeURIComponent(fullAddress)}`;
                    setMapUrl(openStreetMapUrl);
                    setIsLoading(false);
                });
        } catch (err) {
            console.error("Eroare generală:", err);
            setError('Nu s-a putut încărca harta');
            setIsLoading(false);
        }
    }, [address, city, county]);

    if (isLoading) {
        return <div className="detail-map-loading">Se încarcă harta...</div>;
    }

    if (error) {
        return <div className="detail-map-error">{error}</div>;
    }

    return (
        <div className="detail-map-container">
            <h2 className="detail-section-title">Locație</h2>
            <div className="detail-map-wrapper">
                <iframe
                    src={mapUrl}
                    width="100%"
                    height="300"
                    frameBorder="0"
                    style={{ border: 0 }}
                    allowFullScreen
                    aria-hidden="false"
                    tabIndex="0"
                    title="Locație sală de sport"
                    className="detail-map-iframe"
                    loading="lazy"
                />
            </div>
            <div className="detail-map-address">
                <p className="detail-map-address-text">
                    {address}, {city}{county ? `, ${county}` : ''}
                </p>
                <a
                    href={`https://www.google.com/maps/search/?api=1&query=${encodeURIComponent(`${address}, ${city}, Romania`)}`}
                    target="_blank"
                    rel="noopener noreferrer"
                    className="detail-map-directions"
                >
                    Vezi indicații rutiere
                </a>
            </div>
        </div>
    );
};

export default SportsHallMap;