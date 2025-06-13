import React, { Component } from 'react';
import { MapPin, ChevronDown } from 'lucide-react';
import './SearchBar.css';

const API_BASE_URL = process.env.REACT_APP_API_URL || '';

class SearchBar extends Component {

    constructor(props) {
        super(props);
        this.state = {
            location: '',
            activity: 'Sport',
            searchQuery: '',
            cities: [],
            loadingCities: true,
            isLocationDropdownOpen: false,
            isActivityDropdownOpen: false,
            // STATE-URI PENTRU AUTOCOMPLETE
            hallSuggestions: [],
            showHallSuggestions: false,
            loadingSuggestions: false
        };

        // Debounce pentru autocomplete
        this.suggestionTimeout = null;
    }

    componentDidMount() {
        this.fetchCities();
        // Adăugăm event listener pentru click outside
        document.addEventListener('click', this.handleClickOutside);

        // Setăm valorile din URL după ce componentele se încarcă
        this.setValuesFromURL();
    }

    componentWillUnmount() {
        // Curățăm event listener-ul
        document.removeEventListener('click', this.handleClickOutside);
        // Curățăm timeout-ul pentru suggestions
        if (this.suggestionTimeout) {
            clearTimeout(this.suggestionTimeout);
        }
    }

    // METODĂ NOUĂ: Citește parametrii din URL și setează starea
    setValuesFromURL = () => {
        const urlParams = new URLSearchParams(window.location.search);
        const cityFromURL = urlParams.get('city');
        const sportFromURL = urlParams.get('sport');
        const queryFromURL = urlParams.get('query');

        // Actualizăm starea cu valorile din URL (dacă există)
        this.setState({
            location: cityFromURL || this.state.location,
            activity: sportFromURL || 'Sport',
            searchQuery: queryFromURL || ''
        });

        // Notifică componenta părinte despre orașul din URL
        if (this.props.onCityChange && cityFromURL) {
            this.props.onCityChange(cityFromURL);
        }
    };

    // Handler pentru click outside pentru a închide dropdown-urile
    handleClickOutside = (event) => {
        if (!event.target.closest('.search-bar')) {
            this.setState({
                isLocationDropdownOpen: false,
                isActivityDropdownOpen: false,
                showHallSuggestions: false
            });
        }
    };

    // Metodă pentru încărcarea orașelor din baza de date
    fetchCities = async () => {
        this.setState({ loadingCities: true });
        try {
            const response = await fetch(`${API_BASE_URL}/sportsHalls/cities`);
            if (response.ok) {
                const citiesData = await response.json();
                const cities = Array.isArray(citiesData) ? citiesData : [];

                // Verificăm dacă avem un oraș din URL
                const urlParams = new URLSearchParams(window.location.search);
                const cityFromURL = urlParams.get('city');

                // Setăm orașul din URL sau primul oraș disponibil
                const defaultCity = cityFromURL || (cities.length > 0 ? cities[0] : '');

                this.setState({
                    cities,
                    loadingCities: false,
                    location: defaultCity
                }, () => {
                    // Notifică componenta părinte despre orașul selectat
                    if (this.props.onCityChange && this.state.location) {
                        this.props.onCityChange(this.state.location);
                    }
                });
            } else {
                console.error('Eroare la încărcarea orașelor:', response.status);
                this.setState({
                    cities: [],
                    loadingCities: false,
                    location: ''
                });
            }
        } catch (error) {
            console.error('Eroare la încărcarea orașelor:', error);
            this.setState({
                cities: [],
                loadingCities: false,
                location: ''
            });
        }
    };

    handleLocationChange = (location) => {
        this.setState({
            location: location,
            isLocationDropdownOpen: false
        });

        // Notifică componenta părinte despre schimbarea orașului
        if (this.props.onCityChange) {
            this.props.onCityChange(location);
        }
    }

    handleActivityChange = (activity) => {
        this.setState({
            activity: activity,
            isActivityDropdownOpen: false
        });
    }

    toggleLocationDropdown = () => {
        this.setState(prevState => ({
            isLocationDropdownOpen: !prevState.isLocationDropdownOpen,
            isActivityDropdownOpen: false,
            showHallSuggestions: false
        }));
    }

    toggleActivityDropdown = () => {
        this.setState(prevState => ({
            isActivityDropdownOpen: !prevState.isActivityDropdownOpen,
            isLocationDropdownOpen: false,
            showHallSuggestions: false
        }));
    }

    handleSearchChange = (e) => {
        const query = e.target.value;
        this.setState({
            searchQuery: query,
            showHallSuggestions: query.length >= 2 // Arată sugestiile doar pentru query-uri de minim 2 caractere
        });

        // Debounce pentru autocomplete - așteaptă 300ms după ce utilizatorul se oprește din tastare
        if (this.suggestionTimeout) {
            clearTimeout(this.suggestionTimeout);
        }

        if (query.length >= 2) {
            this.suggestionTimeout = setTimeout(() => {
                this.fetchHallSuggestions(query);
            }, 300);
        } else {
            this.setState({
                hallSuggestions: [],
                showHallSuggestions: false
            });
        }
    }

    // Metodă pentru a încărca sugestiile de nume săli
    fetchHallSuggestions = async (query) => {
        this.setState({ loadingSuggestions: true });
        try {
            const response = await fetch(`${API_BASE_URL}/sportsHalls/names/suggestions?query=${encodeURIComponent(query)}`);
            if (response.ok) {
                const suggestions = await response.json();
                this.setState({
                    hallSuggestions: suggestions,
                    loadingSuggestions: false
                });
            } else {
                console.error('Eroare la încărcarea sugestiilor:', response.status);
                this.setState({
                    hallSuggestions: [],
                    loadingSuggestions: false
                });
            }
        } catch (error) {
            console.error('Eroare la încărcarea sugestiilor:', error);
            this.setState({
                hallSuggestions: [],
                loadingSuggestions: false
            });
        }
    };

    // Handler pentru selectarea unei sugestii
    handleSuggestionSelect = (suggestion) => {
        this.setState({
            searchQuery: suggestion,
            showHallSuggestions: false,
            hallSuggestions: []
        });
    };

    handleSearch = (e) => {
        e.preventDefault();

        const searchParams = new URLSearchParams();

        // Construim parametrii de căutare în funcție de ce a completat utilizatorul

        // Adăugăm numele sălii dacă există
        if (this.state.searchQuery && this.state.searchQuery.trim().length > 0) {
            searchParams.append('query', this.state.searchQuery.trim());
        }

        // Adăugăm orașul dacă este selectat
        if (this.state.location && this.state.location.trim().length > 0) {
            searchParams.append('city', this.state.location.trim());
        }

        // Adăugăm sportul (doar dacă nu este "Sport" - valoarea default)
        if (this.state.activity && this.state.activity !== 'Sport') {
            searchParams.append('sport', this.state.activity);
        }

        // Validăm că avem cel puțin un criteriu de căutare
        if (!this.state.searchQuery?.trim() && !this.state.location?.trim() && this.state.activity === 'Sport') {
            alert('Vă rugăm să introduceți cel puțin un criteriu de căutare: numele sălii, orașul sau sportul.');
            return;
        }

        // Redirecționăm către pagina de search cu noii parametri (comportamentul original)
        window.location.href = `/search?${searchParams.toString()}`;
    }

    render() {
        const { cities, loadingCities, hallSuggestions, showHallSuggestions, loadingSuggestions } = this.state;
        const activities = ['Sport', 'Inot', 'Tenis', 'Fotbal', 'Baschet', 'Handbal', 'Volei', 'Polivalenta'];

        return (
            <div className="search-container">
                <form onSubmit={this.handleSearch} className="search-bar">
                    <div className="input-group">
                        <MapPin className="icon" />
                        <div className="custom-select">
                            <div
                                className="selected-option"
                                onClick={this.toggleLocationDropdown}
                            >
                                {loadingCities
                                    ? 'Se încarcă orașele...'
                                    : this.state.location || 'Selectează orașul'
                                }
                                <ChevronDown className="icon-small" />
                            </div>
                            {this.state.isLocationDropdownOpen && !loadingCities && (
                                <div className="options-list">
                                    {cities.length > 0 ? (
                                        cities.map((city) => (
                                            <div
                                                key={city}
                                                className="option"
                                                onClick={() => this.handleLocationChange(city)}
                                            >
                                                {city}
                                            </div>
                                        ))
                                    ) : (
                                        <div className="option disabled">
                                            Nu există orașe disponibile
                                        </div>
                                    )}
                                </div>
                            )}
                        </div>
                    </div>

                    <div className="input-group">
                        <div className="custom-select">
                            <div
                                className="selected-option"
                                onClick={this.toggleActivityDropdown}
                            >
                                {this.state.activity}
                                <ChevronDown className="icon-small" />
                            </div>
                            {this.state.isActivityDropdownOpen && (
                                <div className="options-list">
                                    {activities.map((activity) => (
                                        <div
                                            key={activity}
                                            className="option"
                                            onClick={() => this.handleActivityChange(activity)}
                                        >
                                            {activity}
                                        </div>
                                    ))}
                                </div>
                            )}
                        </div>
                    </div>

                    <div className="input-group search-input-group">
                        <input
                            type="text"
                            value={this.state.searchQuery}
                            onChange={this.handleSearchChange}
                            placeholder="Denumire sala de sport"
                            className="search-input"
                            autoComplete="off"
                        />

                        {/* Dropdown pentru sugestiile de săli */}
                        {showHallSuggestions && (
                            <div className="suggestions-list">
                                {loadingSuggestions ? (
                                    <div className="suggestion loading">Se încarcă sugestiile...</div>
                                ) : hallSuggestions.length > 0 ? (
                                    hallSuggestions.map((suggestion, index) => (
                                        <div
                                            key={index}
                                            className="suggestion"
                                            onClick={() => this.handleSuggestionSelect(suggestion)}
                                        >
                                            {suggestion}
                                        </div>
                                    ))
                                ) : (
                                    <div className="suggestion no-results">
                                        Nu s-au găsit săli cu acest nume
                                    </div>
                                )}
                            </div>
                        )}
                    </div>

                    <button
                        type="submit"
                        className="search-button"
                        disabled={loadingCities}
                    >
                        CAUTĂ
                    </button>
                </form>
            </div>
        );
    }
}

export default SearchBar;