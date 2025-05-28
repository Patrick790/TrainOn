import React, { useState } from 'react';
import './StarRating.css';

const StarRating = ({ defaultRating = 0, onChange, readOnly = false, size = 'medium' }) => {
    const [rating, setRating] = useState(defaultRating);
    const [hoverRating, setHoverRating] = useState(0);

    const handleClick = (value) => {
        if (readOnly) return;

        setRating(value);
        if (onChange) {
            onChange(value);
        }
    };

    const handleMouseOver = (value) => {
        if (readOnly) return;
        setHoverRating(value);
    };

    const handleMouseLeave = () => {
        if (readOnly) return;
        setHoverRating(0);
    };

    return (
        <div className={`star-rating ${size}`}>
            {[1, 2, 3, 4, 5].map((value) => (
                <span
                    key={value}
                    className={`star ${
                        value <= (hoverRating || rating) ? 'filled' : 'empty'
                    } ${readOnly ? 'readonly' : ''}`}
                    onClick={() => handleClick(value)}
                    onMouseOver={() => handleMouseOver(value)}
                    onMouseLeave={handleMouseLeave}
                >
                    {value <= (hoverRating || rating) ? '★' : '☆'}
                </span>
            ))}
        </div>
    );
};

export default StarRating;