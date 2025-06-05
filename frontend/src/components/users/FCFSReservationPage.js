import React, { useState, useEffect, useMemo } from 'react';
import { useNavigate } from 'react-router-dom';
import Footer from '../pageComponents/Footer';
import './FCFSReservationPage.css';

const FCFS_LOCAL_STORAGE_CITY_KEY = 'fcfsSelectedCity';
const FCFS_LOCAL_STORAGE_HALL_KEY = 'fcfsSelectedHall';

const FCFSReservationPage = () => {
    const navigate = useNavigate();
    const [selectedCity, setSelectedCity] = useState('');
    const [selectedHall, setSelectedHall] = useState('');
    const [currentWeek, setCurrentWeek] = useState(new Date());
    const [selectedDate, setSelectedDate] = useState(null);
    const [selectedSlots, setSelectedSlots] = useState([]);
    const [availableSlots, setAvailableSlots] = useState([]);
    const [sportsHalls, setSportsHalls] = useState([]);
    const [cities, setCities] = useState([]);
    const [loading, setLoading] = useState(false);
    const [selectedHallDetails, setSelectedHallDetails] = useState(null); // Nou state pentru detaliile sălii

    const getWeekStart = (date) => {
        const start = new Date(date);
        const day = start.getDay();
        const diff = start.getDate() - day + (day === 0 ? -6 : 1);
        start.setDate(diff);
        start.setHours(0, 0, 0, 0);
        return start;
    };

    const initialCurrentSystemWeekStart = useMemo(() => getWeekStart(new Date()), []);
    const nextSystemWeekStart = useMemo(() => {
        const nextWeek = new Date(initialCurrentSystemWeekStart);
        nextWeek.setDate(nextWeek.getDate() + 7);
        return nextWeek;
    }, [initialCurrentSystemWeekStart]);

    const today = useMemo(() => {
        const d = new Date();
        d.setHours(0, 0, 0, 0);
        return d;
    }, []);

    useEffect(() => { fetchCities(); }, []);

    // LocalStorage persistence
    useEffect(() => {
        const storedCity = localStorage.getItem(FCFS_LOCAL_STORAGE_CITY_KEY);
        if (storedCity && cities.length > 0) {
            const cityExists = cities.includes(storedCity);
            if (cityExists) {
                setSelectedCity(storedCity);
            } else {
                localStorage.removeItem(FCFS_LOCAL_STORAGE_CITY_KEY);
            }
        }
    }, [cities]);

    useEffect(() => {
        if (selectedCity && sportsHalls && sportsHalls.length > 0) {
            const storedHallId = localStorage.getItem(FCFS_LOCAL_STORAGE_HALL_KEY);
            if (storedHallId) {
                const hallExists = sportsHalls.some(hall => hall.id.toString() === storedHallId);
                if (hallExists) {
                    setSelectedHall(storedHallId);
                } else {
                    localStorage.removeItem(FCFS_LOCAL_STORAGE_HALL_KEY);
                    setSelectedHall('');
                }
            }
        }
    }, [sportsHalls, selectedCity]);

    useEffect(() => {
        if (selectedCity) localStorage.setItem(FCFS_LOCAL_STORAGE_CITY_KEY, selectedCity);
        else localStorage.removeItem(FCFS_LOCAL_STORAGE_CITY_KEY);
    }, [selectedCity]);

    useEffect(() => {
        if (selectedHall) localStorage.setItem(FCFS_LOCAL_STORAGE_HALL_KEY, selectedHall);
        else localStorage.removeItem(FCFS_LOCAL_STORAGE_HALL_KEY);
    }, [selectedHall]);

    useEffect(() => {
        if (selectedCity) fetchSportsHalls();
        else { setSportsHalls([]); setSelectedHall(''); }
    }, [selectedCity]);

    useEffect(() => {
        if (selectedHall) {
            fetchAvailableSlots();
            fetchHallDetails(); // Nouă funcție pentru a obține detaliile sălii
        } else {
            setAvailableSlots([]);
            setSelectedDate(null);
            setSelectedHallDetails(null); // Resetează detaliile sălii
        }
    }, [selectedHall, currentWeek]);

    // Auto-select first valid day
    useEffect(() => {
        if (availableSlots.length > 0) {
            const currentSelectedDateStillValid = selectedDate && availableSlots.some(slot => isSameDay(slot.date, selectedDate));
            if (currentSelectedDateStillValid) {
                const hasActiveSlotsOnSelectedDate = availableSlots.some(slot => isSameDay(slot.date, selectedDate));
                if (!hasActiveSlotsOnSelectedDate) {
                    const firstValidDay = availableSlots.find(slot => slot.date >= today);
                    setSelectedDate(firstValidDay ? new Date(firstValidDay.date.getFullYear(), firstValidDay.date.getMonth(), firstValidDay.date.getDate()) : null);
                }
            } else {
                const firstFutureSlot = availableSlots.find(slot => {
                    const slotDay = new Date(slot.date);
                    slotDay.setHours(0,0,0,0);
                    return slotDay >= today;
                });
                setSelectedDate(firstFutureSlot ? new Date(firstFutureSlot.date.getFullYear(), firstFutureSlot.date.getMonth(), firstFutureSlot.date.getDate()) : null);
            }
        } else {
            setSelectedDate(null);
        }
    }, [availableSlots, today]);

    const fetchCities = async () => {
        setLoading(true);
        try {
            const response = await fetch('http://localhost:8080/sportsHalls/cities');
            const citiesData = response.ok ? await response.json() : [];
            setCities(Array.isArray(citiesData) ? citiesData : []);
        } catch (error) {
            console.error('Eroare la încărcarea orașelor:', error);
            setCities([]);
        } finally {
            setLoading(false);
        }
    };

    const fetchSportsHalls = async () => {
        setLoading(true);
        try {
            const response = await fetch(`http://localhost:8080/sportsHalls?city=${encodeURIComponent(selectedCity)}`);
            const halls = response.ok ? await response.json() : [];
            setSportsHalls(Array.isArray(halls) ? halls : []);
        } catch (error) {
            console.error('Eroare la încărcarea sălilor:', error);
            setSportsHalls([]);
        } finally {
            setLoading(false);
        }
    };

    // Nouă funcție pentru a obține detaliile sălii selectate
    const fetchHallDetails = async () => {
        if (!selectedHall) return;

        try {
            const response = await fetch(`http://localhost:8080/sportsHalls/${selectedHall}`);
            if (response.ok) {
                const hallData = await response.json();
                setSelectedHallDetails(hallData);
            } else {
                console.error(`Eroare la obținerea detaliilor pentru sala ${selectedHall}`);
                setSelectedHallDetails(null);
            }
        } catch (error) {
            console.error('Eroare la obținerea detaliilor sălii:', error);
            setSelectedHallDetails(null);
        }
    };

    const fetchAvailableSlots = async () => {
        if (!selectedHall) return;
        setLoading(true);
        try {
            const [scheduleResponse, reservationsResponse] = await Promise.all([
                fetch(`http://localhost:8080/schedules/hall/${selectedHall}`),
                fetch(`http://localhost:8080/reservations?hallId=${selectedHall}`)
            ]);
            const schedule = scheduleResponse.ok ? await scheduleResponse.json() : [];
            const reservations = reservationsResponse.ok ? await reservationsResponse.json() : [];
            const slots = generateAvailableSlots(schedule, reservations, getWeekStart(currentWeek), getWeekEnd(currentWeek));
            setAvailableSlots(slots);
        } catch (error) {
            console.error('Eroare la încărcarea sloturilor:', error);
            setAvailableSlots([]);
        } finally {
            setLoading(false);
        }
    };

    const isSameDay = (date1, date2) => {
        if (!date1 || !date2) return false;
        return date1.getFullYear() === date2.getFullYear() && date1.getMonth() === date2.getMonth() && date1.getDate() === date2.getDate();
    };

    const generateAvailableSlots = (schedule, reservations, weekStart, weekEnd) => {
        const slots = [];
        const currentDateIterator = new Date(weekStart);
        const now = new Date();
        const todayDateOnly = new Date(now.getFullYear(), now.getMonth(), now.getDate());

        while (currentDateIterator <= weekEnd) {
            if (currentDateIterator < todayDateOnly) {
                currentDateIterator.setDate(currentDateIterator.getDate() + 1);
                continue;
            }

            const dayOfWeek = currentDateIterator.getDay() === 0 ? 7 : currentDateIterator.getDay();
            const daySchedule = schedule.find(s => s.dayOfWeek === dayOfWeek && s.isActive);

            if (daySchedule) {
                const timeSlots = generateTimeSlots(daySchedule.startTime, daySchedule.endTime);
                timeSlots.forEach(timeSlot => {
                    const slotDate = new Date(currentDateIterator);
                    if (isSameDay(slotDate, todayDateOnly)) {
                        const [startHourStr, startMinStr] = timeSlot.split('-')[0].split(':');
                        const slotStartDateTime = new Date(slotDate);
                        slotStartDateTime.setHours(parseInt(startHourStr, 10), parseInt(startMinStr, 10), 0, 0);
                        if (slotStartDateTime < now) return;
                    }

                    const isReserved = reservations.some(reservation => {
                        let reservationDate;
                        if (typeof reservation.date === 'string' && reservation.date.includes('T')) {
                            reservationDate = new Date(reservation.date);
                        } else if (typeof reservation.date === 'string') {
                            reservationDate = new Date(reservation.date + 'T00:00:00');
                        } else {
                            reservationDate = new Date(reservation.date);
                        }
                        return isSameDay(reservationDate, slotDate) && reservation.timeSlot === timeSlot;
                    });

                    if (!isReserved) {
                        slots.push({
                            id: `${formatDateForAPI(slotDate)}-${timeSlot}`,
                            date: new Date(slotDate),
                            timeSlot: timeSlot,
                            dayName: getDayName(slotDate),
                            available: true
                        });
                    }
                });
            }
            currentDateIterator.setDate(currentDateIterator.getDate() + 1);
        }
        return slots;
    };

    const generateTimeSlots = (startTime, endTime) => {
        const slots = [];
        const [startHour, startMin] = startTime.split(':').map(Number);
        const [endHour, endMin] = endTime.split(':').map(Number);
        let currentHour = startHour, currentMin = startMin;

        while (currentHour < endHour || (currentHour === endHour && currentMin < endMin)) {
            const slotStart = `${String(currentHour).padStart(2, '0')}:${String(currentMin).padStart(2, '0')}`;
            let tempHour = currentHour, tempMin = currentMin + 90;
            if (tempMin >= 60) {
                tempHour += Math.floor(tempMin / 60);
                tempMin = tempMin % 60;
            }
            if (tempHour > endHour || (tempHour === endHour && tempMin > endMin)) break;
            const slotEnd = `${String(tempHour).padStart(2, '0')}:${String(tempMin).padStart(2, '0')}`;
            slots.push(`${slotStart}-${slotEnd}`);
            currentHour = tempHour;
            currentMin = tempMin;
        }
        return slots;
    };

    const getWeekEnd = (date) => {
        const end = getWeekStart(date);
        end.setDate(end.getDate() + 6);
        end.setHours(23, 59, 59, 999);
        return end;
    };

    const getDayName = (date, short = false) => {
        const daysFull = ['Duminică', 'Luni', 'Marți', 'Miercuri', 'Joi', 'Vineri', 'Sâmbătă'];
        const daysShort = ['Du', 'Lu', 'Ma', 'Mi', 'Jo', 'Vi', 'Sâ'];
        return short ? daysShort[date.getDay()] : daysFull[date.getDay()];
    };

    const formatDateForAPI = (date) => {
        const year = date.getFullYear();
        const month = String(date.getMonth() + 1).padStart(2, '0');
        const day = String(date.getDate()).padStart(2, '0');
        return `${year}-${month}-${day}`;
    };

    const formatWeekRange = () => {
        const start = getWeekStart(currentWeek);
        const end = getWeekEnd(currentWeek);
        return `${start.getDate()} ${start.toLocaleDateString('ro-RO', { month: 'long' })} - ${end.getDate()} ${end.toLocaleDateString('ro-RO', { month: 'long' })} ${end.getFullYear()}`;
    };

    const handleCityChange = (city) => {
        setSelectedCity(city);
        setSelectedHall('');
        setSportsHalls([]);
        setAvailableSlots([]);
        setSelectedSlots([]);
        setCurrentWeek(new Date());
        setSelectedDate(null);
        setSelectedHallDetails(null); // Resetează detaliile sălii
    };

    const handleHallChange = (hallId) => {
        setSelectedHall(hallId);
        setAvailableSlots([]);
        setSelectedSlots([]);
        setCurrentWeek(new Date());
        setSelectedDate(null);
        setSelectedHallDetails(null); // Resetează detaliile sălii
    };

    const handleSlotSelect = (slotId) => {
        const slotData = availableSlots.find(s => s.id === slotId);
        if (!slotData) return;

        if (selectedSlots.includes(slotId)) {
            setSelectedSlots(selectedSlots.filter(id => id !== slotId));
        } else {
            const selectedSlotsForSameDay = selectedSlots.filter(selectedSlotId => {
                const selectedSlotData = availableSlots.find(s => s.id === selectedSlotId);
                return selectedSlotData && isSameDay(selectedSlotData.date, slotData.date);
            });

            if (selectedSlotsForSameDay.length > 0) {
                const newSelectedSlots = selectedSlots.filter(selectedSlotId => {
                    const selectedSlotData = availableSlots.find(s => s.id === selectedSlotId);
                    return !selectedSlotData || !isSameDay(selectedSlotData.date, slotData.date);
                });
                setSelectedSlots([...newSelectedSlots, slotId]);
            } else {
                setSelectedSlots([...selectedSlots, slotId]);
            }
        }
    };

    const handlePreviousWeek = () => {
        const displayedWeekStart = getWeekStart(currentWeek);
        if (displayedWeekStart.getTime() > initialCurrentSystemWeekStart.getTime()) {
            const prevWeek = new Date(currentWeek);
            prevWeek.setDate(currentWeek.getDate() - 7);
            setCurrentWeek(prevWeek);
            setSelectedDate(null);
        }
    };

    const handleNextWeek = () => {
        const displayedWeekStart = getWeekStart(currentWeek);
        if (displayedWeekStart.getTime() < nextSystemWeekStart.getTime()) {
            const nextWeekDate = new Date(currentWeek);
            nextWeekDate.setDate(currentWeek.getDate() + 7);
            setCurrentWeek(nextWeekDate);
            setSelectedDate(null);
        }
    };

    const handleProceedToPayment = () => {
        if (selectedSlots.length === 0) {
            alert('Vă rugăm să selectați cel puțin un slot pentru rezervare.');
            return;
        }

        // Pregătește datele pentru pagina de plată
        const reservationData = selectedSlots.map(slotId => {
            const slotData = availableSlots.find(s => s.id === slotId);
            return {
                hallId: parseInt(selectedHall),
                hallName: selectedHallName,
                date: formatDateForAPI(slotData.date),
                dateFormatted: slotData.date.toLocaleDateString('ro-RO', { weekday: 'long', day: 'numeric', month: 'long', year: 'numeric' }),
                timeSlot: slotData.timeSlot,
            };
        });

        // Navighează către pagina de plată cu datele rezervării
        navigate('/payment', {
            state: {
                reservations: reservationData,
                city: selectedCity
            }
        });
    };

    const selectedHallName = sportsHalls.find(hall => hall.id == selectedHall)?.name || '';
    const isPrevWeekButtonDisabled = getWeekStart(currentWeek).getTime() <= initialCurrentSystemWeekStart.getTime();
    const isNextWeekButtonDisabled = getWeekStart(currentWeek).getTime() >= nextSystemWeekStart.getTime();

    const daysForSelector = useMemo(() => {
        const startOfWeek = getWeekStart(currentWeek);
        const daysArray = [];
        for (let i = 0; i < 7; i++) {
            const day = new Date(startOfWeek);
            day.setDate(startOfWeek.getDate() + i);
            daysArray.push(day);
        }
        return daysArray;
    }, [currentWeek]);

    const dayHeaders = useMemo(() => daysForSelector.map(day => getDayName(day, true)), [daysForSelector]);

    const slotsForSelectedDay = useMemo(() => {
        if (!selectedDate || !availableSlots) return [];
        return availableSlots.filter(slot => isSameDay(slot.date, selectedDate)).sort((a, b) => a.timeSlot.localeCompare(b.timeSlot));
    }, [selectedDate, availableSlots]);

    const hasSelectedSlotForSelectedDay = useMemo(() => {
        if (!selectedDate || selectedSlots.length === 0) return false;
        return selectedSlots.some(slotId => {
            const slotData = availableSlots.find(s => s.id === slotId);
            return slotData && isSameDay(slotData.date, selectedDate);
        });
    }, [selectedDate, selectedSlots, availableSlots]);

    // Funcție pentru a formata prețul
    const formatPrice = (price) => {
        if (price == null) return '50.00'; // Preț implicit dacă nu există
        return Number(price).toFixed(2);
    };

    return (
        <div className="fcfs-reservation-page">
            {/* Hero Section cu Design Elegant */}
            <div className="fcfs-hero-section">
                <div className="light-line"></div>
                <div className="light-line"></div>
                <div className="light-line"></div>
                <div className="light-line"></div>
                <div className="fcfs-hero-container">
                    <div className="fcfs-header">
                        <button onClick={() => navigate('/')} className="fcfs-back-button">← Înapoi</button>
                        <h1 className="fcfs-title">Rezervarea locurilor rămase</h1>
                        <p className="fcfs-subtitle">Selectează ziua și ora dorită (un interval pe zi)</p>
                    </div>

                    {/* Container pentru Filtre */}
                    <div className="fcfs-filters-container">
                        <div className="fcfs-filters">
                            <div className="fcfs-filter-group">
                                <label htmlFor="city-select">Alege orașul:</label>
                                <select id="city-select" value={selectedCity} onChange={(e) => handleCityChange(e.target.value)} className="fcfs-select" disabled={loading}>
                                    <option value="">Selectează orașul</option>
                                    {cities.map(city => <option key={city} value={city}>{city}</option>)}
                                </select>
                            </div>
                            <div className="fcfs-filter-group">
                                <label htmlFor="hall-select">Alege sala de sport:</label>
                                <select id="hall-select" value={selectedHall} onChange={(e) => handleHallChange(e.target.value)} className="fcfs-select" disabled={!selectedCity || sportsHalls.length === 0 || loading}>
                                    <option value="">Selectează sala</option>
                                    {sportsHalls.map(hall => <option key={hall.id} value={hall.id}>{hall.name}</option>)}
                                </select>
                            </div>
                        </div>
                    </div>
                </div>
            </div>

            {/* Container pentru Conținutul Principal */}
            <div className="fcfs-content-container">
                <div className="fcfs-main-container">
                    {selectedHall && (
                        <div className="fcfs-content">
                            <div className="fcfs-week-navigation">
                                <button onClick={handlePreviousWeek} className="fcfs-nav-button" disabled={isPrevWeekButtonDisabled || loading}>←</button>
                                <h2 className="fcfs-week-title">{formatWeekRange()}</h2>
                                <button onClick={handleNextWeek} className="fcfs-nav-button" disabled={isNextWeekButtonDisabled || loading}>→</button>
                            </div>

                            <div className="fcfs-hall-info">
                                <h3>
                                    {selectedHallName ? (
                                        <>
                                            Program pentru {selectedHallName}
                                            {selectedHallDetails && (
                                                <span className="hall-price"> - {formatPrice(selectedHallDetails.tariff)} RON/interval</span>
                                            )}
                                        </>
                                    ) : (
                                        'Selectează o sală'
                                    )}
                                </h3>
                            </div>

                            <div className="fcfs-day-selector-container">
                                <div className="fcfs-day-headers">
                                    {dayHeaders.map((header, index) => <div key={index} className="fcfs-day-header-item">{header}</div>)}
                                </div>
                                <div className="fcfs-day-buttons">
                                    {daysForSelector.map(day => {
                                        const dayStr = day.toISOString().split('T')[0];
                                        const isSelected = selectedDate && isSameDay(day, selectedDate);
                                        const isPast = day < today;
                                        const hasSlotsForDay = availableSlots.some(s => isSameDay(s.date, day));
                                        const dayHasSelectedSlot = selectedSlots.some(slotId => {
                                            const slotData = availableSlots.find(s => s.id === slotId);
                                            return slotData && isSameDay(slotData.date, day);
                                        });
                                        const shouldShowIndicator = dayHasSelectedSlot && !isSameDay(day, today);

                                        let buttonClass = 'fcfs-day-button';
                                        if (isSelected) buttonClass += ' selected';
                                        if (isPast && !isSameDay(day, today)) buttonClass += ' past';
                                        if (!hasSlotsForDay && !isPast) buttonClass += ' no-slots';
                                        if (dayHasSelectedSlot) buttonClass += ' has-selection';

                                        const isDisabled = (isPast && !isSameDay(day, today)) || (!hasSlotsForDay && !isSameDay(day, today) && !isPast);

                                        return (
                                            <button
                                                key={dayStr}
                                                className={buttonClass}
                                                onClick={() => {
                                                    if (hasSlotsForDay || isSameDay(day,today)) {
                                                        setSelectedDate(day);
                                                    } else if (!isPast) {
                                                        alert('Nu există intervale disponibile pentru această zi.');
                                                    }
                                                }}
                                                disabled={isDisabled || loading}
                                            >
                                                {day.getDate()}
                                                {shouldShowIndicator && <span className="selected-indicator">●</span>}
                                            </button>
                                        );
                                    })}
                                </div>
                            </div>

                            {selectedDate && (
                                <div className="fcfs-time-slots-section">
                                    <h4 className="fcfs-time-slots-title">
                                        Intervalele disponibile pentru: {selectedDate.toLocaleDateString('ro-RO', { weekday: 'long', day: 'numeric', month: 'long' })}
                                        {hasSelectedSlotForSelectedDay && <span className="day-status"> (Interval selectat pentru această zi)</span>}
                                    </h4>
                                    {loading && slotsForSelectedDay.length === 0 ? (
                                        <div className="fcfs-loading"><p>Se încarcă intervalele...</p></div>
                                    ) : slotsForSelectedDay.length > 0 ? (
                                        <div className="fcfs-slots-list-for-day">
                                            {slotsForSelectedDay.map((slot, index) => (
                                                <div
                                                    key={slot.id}
                                                    className={`fcfs-slot-card-timeonly ${selectedSlots.includes(slot.id) ? 'selected' : ''} animating-in`}
                                                    style={{ animationDelay: `${index * 0.05}s` }}
                                                    onClick={() => !loading && handleSlotSelect(slot.id)}
                                                >
                                                    <div className="fcfs-slot-time">{slot.timeSlot}</div>
                                                    <div className="fcfs-slot-status-timeonly">{selectedSlots.includes(slot.id) ? 'Selectat' : 'Disponibil'}</div>
                                                </div>
                                            ))}
                                        </div>
                                    ) : (
                                        <div className="fcfs-no-slots">
                                            <p>Nu sunt intervale orare disponibile pentru ziua selectată sau toate intervalele au expirat.</p>
                                        </div>
                                    )}
                                </div>
                            )}

                            {(slotsForSelectedDay.length > 0 || selectedSlots.length > 0) && !loading && (
                                <div className="fcfs-actions">
                                    <div className="fcfs-buttons">
                                        <button onClick={() => setSelectedSlots([])} className="fcfs-cancel-button" disabled={loading || selectedSlots.length === 0}>Anulează Selecția</button>
                                        <button onClick={handleProceedToPayment} className="fcfs-confirm-button" disabled={selectedSlots.length === 0 || loading}>
                                            Continuă la Plată
                                        </button>
                                    </div>
                                </div>
                            )}
                        </div>
                    )}

                    {(!selectedCity || !selectedHall) && !loading && (
                        <div className="fcfs-empty-state">
                            <p>Selectează orașul și sala de sport pentru a vizualiza programul.</p>
                        </div>
                    )}
                </div>
            </div>

            {/* Footer Component */}
            <Footer />
        </div>
    );
};

export default FCFSReservationPage;