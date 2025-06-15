import React, { useState, useEffect } from 'react';
import axios from 'axios';
import './SendEmailsPage.css';

const SendEmailsPage = () => {
    const [emailData, setEmailData] = useState({
        recipientType: 'all_users',
        specificUser: '',
        subject: '',
        content: ''
    });
    const [users, setUsers] = useState([]);
    const [isLoading, setIsLoading] = useState(false);
    const [isSending, setIsSending] = useState(false);
    const [message, setMessage] = useState({ type: '', text: '' });
    const [searchTerm, setSearchTerm] = useState('');
    const [filteredUsers, setFilteredUsers] = useState([]);
    const [showUserDropdown, setShowUserDropdown] = useState(false);
    const [recipientStats, setRecipientStats] = useState({
        all_users: 0,
        all_admins: 0
    });

    const API_URL = process.env.REACT_APP_API_URL || '';
    const getAuthHeader = () => {
        const token = localStorage.getItem('jwtToken');
        return token ? { headers: { 'Authorization': `Bearer ${token}` } } : {};
    };

    useEffect(() => {
        const fetchData = async () => {
            setIsLoading(true);
            try {
                // Fetch users
                const usersResponse = await axios.get(`${API_URL}/users`, getAuthHeader());
                setUsers(usersResponse.data);
                setFilteredUsers(usersResponse.data);

                // Calculate recipient stats
                const userCount = usersResponse.data.filter(user => user.userType === 'user').length;
                const adminCount = usersResponse.data.filter(user => user.userType === 'admin').length;

                setRecipientStats({
                    all_users: userCount,
                    all_admins: adminCount
                });
            } catch (error) {
                console.error('Error fetching data:', error);
                setMessage({
                    type: 'error',
                    text: 'Nu s-au putut încărca datele. Vă rugăm să încercați din nou.'
                });
            } finally {
                setIsLoading(false);
            }
        };

        fetchData();
    }, []);

    // Filter users based on search term
    useEffect(() => {
        if (searchTerm.trim() === '') {
            setFilteredUsers(users);
        } else {
            const filtered = users.filter(user =>
                user.name?.toLowerCase().includes(searchTerm.toLowerCase()) ||
                user.email?.toLowerCase().includes(searchTerm.toLowerCase())
            );
            setFilteredUsers(filtered);
        }
    }, [searchTerm, users]);

    const handleInputChange = (e) => {
        const { name, value } = e.target;
        setEmailData(prev => ({ ...prev, [name]: value }));

        setMessage({ type: '', text: '' });
    };

    const handleRecipientTypeChange = (e) => {
        const recipientType = e.target.value;
        setEmailData(prev => ({
            ...prev,
            recipientType,
            specificUser: recipientType === 'specific_user' ? prev.specificUser : ''
        }));
    };

    const handleUserSearch = (e) => {
        const value = e.target.value;
        setSearchTerm(value);
        setEmailData(prev => ({ ...prev, specificUser: value }));
        setShowUserDropdown(value.trim() !== '');
    };

    const selectUser = (user) => {
        setEmailData(prev => ({ ...prev, specificUser: user.email }));
        setSearchTerm(user.name);
        setShowUserDropdown(false);
    };

    const validateForm = () => {
        if (emailData.recipientType === 'specific_user' && !emailData.specificUser) {
            setMessage({ type: 'error', text: 'Vă rugăm să selectați un utilizator specific.' });
            return false;
        }
        if (!emailData.subject.trim()) {
            setMessage({ type: 'error', text: 'Vă rugăm să introduceți un subiect.' });
            return false;
        }
        if (!emailData.content.trim()) {
            setMessage({ type: 'error', text: 'Vă rugăm să introduceți conținutul email-ului.' });
            return false;
        }
        return true;
    };

    const handleSubmit = async (e) => {
        e.preventDefault();

        if (!validateForm()) return;

        setIsSending(true);
        setMessage({ type: '', text: '' });

        try {
            const emailPayload = {
                recipientType: emailData.recipientType,
                recipient: emailData.recipientType === 'specific_user' ? emailData.specificUser : null,
                subject: emailData.subject,
                content: emailData.content
            };

            const response = await axios.post(
                `${API_URL}/admin/send-email`,
                emailPayload,
                getAuthHeader()
            );

            setMessage({
                type: 'success',
                text: 'Email-urile au fost trimise cu succes!'
            });

            setEmailData({
                recipientType: 'all_users',
                specificUser: '',
                subject: '',
                content: ''
            });
            setSearchTerm('');

        } catch (error) {
            console.error('Error sending emails:', error);
            setMessage({
                type: 'error',
                text: error.response?.data?.message || 'A apărut o eroare la trimiterea email-urilor. Vă rugăm să încercați din nou.'
            });
        } finally {
            setIsSending(false);
        }
    };

    const getRecipientCount = () => {
        switch (emailData.recipientType) {
            case 'all_users':
                return recipientStats.all_users;
            case 'all_admins':
                return recipientStats.all_admins;
            case 'specific_user':
                return emailData.specificUser ? 1 : 0;
            default:
                return 0;
        }
    };

    const checkIsAdmin = () => {
        return localStorage.getItem('userType') === 'admin';
    };

    if (!checkIsAdmin()) {
        return (
            <div className="global-admin-page">
                <div className="email-error-message">
                    <h2>Acces restricționat</h2>
                    <p>Această pagină este disponibilă doar pentru administratori.</p>
                </div>
            </div>
        );
    }

    return (
        <div className="global-admin-page">
            <h2 className="global-admin-page-title">Trimiterea email-urilor</h2>

            <div className="email-form-container">
                <div className="email-form-header">
                    <h3>Compune mesaj nou</h3>
                    <p className="email-form-description">
                        Trimite email-uri către utilizatorii platformei
                    </p>
                </div>

                <form onSubmit={handleSubmit} className="email-form">
                    <div className="email-form-section">
                        <label className="email-form-label">Destinatar:</label>
                        <div className="email-selection-container">
                            <select
                                className="email-form-select"
                                name="recipientType"
                                value={emailData.recipientType}
                                onChange={handleRecipientTypeChange}
                            >
                                <option value="all_users">Toți utilizatorii ({recipientStats.all_users})</option>
                                <option value="all_admins">Toți administratorii ({recipientStats.all_admins})</option>
                                <option value="specific_user">Utilizator specific</option>
                            </select>

                            {emailData.recipientType === 'specific_user' && (
                                <div className="email-user-search-container">
                                    <input
                                        type="text"
                                        placeholder="Căutați după nume sau email"
                                        className="email-user-search-input"
                                        value={searchTerm}
                                        onChange={handleUserSearch}
                                        onFocus={() => setShowUserDropdown(searchTerm.trim() !== '')}
                                    />

                                    {showUserDropdown && (
                                        <div className="email-user-dropdown">
                                            {isLoading ? (
                                                <div className="email-dropdown-item-loading">Se încarcă utilizatorii...</div>
                                            ) : filteredUsers.length > 0 ? (
                                                filteredUsers.map(user => (
                                                    <div
                                                        key={user.id}
                                                        className="email-dropdown-item"
                                                        onClick={() => selectUser(user)}
                                                    >
                                                        <div className="email-dropdown-user-name">{user.name}</div>
                                                        <div className="email-dropdown-user-email">{user.email}</div>
                                                    </div>
                                                ))
                                            ) : (
                                                <div className="email-dropdown-item-empty">Nu s-au găsit utilizatori</div>
                                            )}
                                        </div>
                                    )}
                                </div>
                            )}

                            <div className="email-recipient-count">
                                Destinatari: <span>{getRecipientCount()}</span>
                            </div>
                        </div>
                    </div>

                    <div className="email-form-section">
                        <label className="email-form-label">Subiect:</label>
                        <input
                            type="text"
                            name="subject"
                            value={emailData.subject}
                            onChange={handleInputChange}
                            placeholder="Introduceți subiectul email-ului"
                            className="email-form-input"
                        />
                    </div>

                    <div className="email-form-section">
                        <label className="email-form-label">Conținut:</label>
                        <div className="email-textarea-container">
                            <textarea
                                name="content"
                                value={emailData.content}
                                onChange={handleInputChange}
                                placeholder="Introduceți conținutul email-ului"
                                className="email-form-textarea"
                            ></textarea>
                            <div className="email-textarea-tools">
                                <div className="email-textarea-info">
                                    {emailData.content.length} caractere
                                </div>
                            </div>
                        </div>
                    </div>

                    {message.text && (
                        <div className={`email-message ${message.type}`}>
                            {message.text}
                        </div>
                    )}

                    <div className="email-form-actions">
                        <button
                            type="submit"
                            className="email-send-button"
                            disabled={isSending}
                        >
                            {isSending ? 'Se trimite...' : 'Trimite email'}
                        </button>
                    </div>
                </form>
            </div>
        </div>
    );
};

export default SendEmailsPage;