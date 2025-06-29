import React, { Component } from 'react';
import './AddHallPage.css';
import { X, ImagePlus, Plus } from 'lucide-react';

const COUNTIES = [
    'Alba', 'Arad', 'Argeș', 'Bacău', 'Bihor', 'Bistrița-Năsăud', 'Botoșani',
    'Brăila', 'Brașov', 'București', 'Buzău', 'Călărași', 'Caraș-Severin',
    'Cluj', 'Constanța', 'Covasna', 'Dâmbovița', 'Dolj', 'Galați', 'Giurgiu',
    'Gorj', 'Harghita', 'Hunedoara', 'Ialomița', 'Iași', 'Ilfov', 'Maramureș',
    'Mehedinți', 'Mureș', 'Neamț', 'Olt', 'Prahova', 'Sălaj', 'Satu Mare',
    'Sibiu', 'Suceava', 'Teleorman', 'Timiș', 'Tulcea', 'Vâlcea', 'Vaslui',
    'Vrancea'
].sort();

const IMAGE_TYPES = [
    {
        type: 'cover',
        label: 'Imagine Copertă',
        description: 'Selectează imaginea principală a sălii (max. 10MB)',
        max: 1
    },
    {
        type: 'exterior',
        label: 'Imagine Exterior',
        description: 'Încarcă o imagine cu fațada sălii (max. 10MB)',
        max: 1
    },
    {
        type: 'interior',
        label: 'Imagini Interior',
        description: 'Maxim 4 imagini din interiorul sălii (max. 10MB/imagine)',
        max: 4
    },
    {
        type: 'locker',
        label: 'Imagini Vestiare',
        description: 'Maxim 2 imagini cu vestiare (max. 10MB/imagine)',
        max: 2
    }
];

class AddHallPage extends Component {
    constructor(props) {
        super(props);
        this.API_BASE_URL = process.env.REACT_APP_API_URL || '';
        this.state = {
            hallData: {
                name: '',
                county: '',
                city: '',
                address: '',
                capacity: '',
                type: '',
                tariff: '',
                description: '',
                facilities: '',
                phoneNumber: ''
            },
            images: {
                cover: [],
                exterior: [],
                interior: [],
                locker: []
            },
            previewImage: null,
            isSubmitting: false,
            submitError: null,
            submitSuccess: false
        };
    }

    handleInputChange = (e) => {
        const { name, value } = e.target;
        this.setState(prevState => ({
            hallData: {
                ...prevState.hallData,
                [name]: value
            }
        }));
    }

    handleFileChange = (e, imageType) => {
        const newFiles = Array.from(e.target.files);

        this.setState(prevState => {
            const currentCount = prevState.images[imageType].length;
            const typeConfig = IMAGE_TYPES.find(type => type.type === imageType);
            const maxAllowed = typeConfig.max;

            const filesToAdd = newFiles.slice(0, maxAllowed - currentCount);

            return {
                images: {
                    ...prevState.images,
                    [imageType]: [
                        ...prevState.images[imageType],
                        ...filesToAdd.map(file => ({
                            file,
                            preview: URL.createObjectURL(file),
                            type: imageType
                        }))
                    ]
                }
            };
        });
    }

    removeImage = (imageType, indexToRemove) => {
        this.setState(prevState => ({
            images: {
                ...prevState.images,
                [imageType]: prevState.images[imageType].filter((_, index) => index !== indexToRemove)
            }
        }));
    }

    openImagePreview = (imageUrl) => {
        this.setState({ previewImage: imageUrl });
    }

    closeImagePreview = () => {
        this.setState({ previewImage: null });
    }

    isValidPhoneNumber = (phoneNumber) => {
        const phoneRegex = /^(\+4|4|0)?(7[0-9]{8}|[23][0-9]{8})$/;
        return phoneRegex.test(phoneNumber.replace(/[\s\-\(\)]/g, ''));
    }

    prepareFormData = () => {
        const { hallData, images } = this.state;

        const processedHallData = {
            ...hallData,
            capacity: hallData.capacity ? parseInt(hallData.capacity, 10) : 0,
            tariff: hallData.tariff ? parseFloat(hallData.tariff) : 0
        };

        const formData = new FormData();

        formData.append('sportsHall', new Blob([JSON.stringify(processedHallData)], { type: 'application/json' }));

        const allImages = [];
        const imageTypes = [];

        Object.entries(images).forEach(([type, imagesList]) => {
            imagesList.forEach(img => {
                allImages.push(img.file);
                imageTypes.push(type);
            });
        });

        allImages.forEach(file => {
            formData.append('images', file);
        });

        imageTypes.forEach(type => {
            formData.append('imageTypes', type);
        });

        return formData;
    }

    validateForm = () => {
        const { hallData, images } = this.state;

        if (!hallData.name || !hallData.county || !hallData.city ||
            !hallData.address || !hallData.capacity || !hallData.type ||
            !hallData.tariff || !hallData.phoneNumber) {
            this.setState({ submitError: 'Toate câmpurile marcate cu * sunt obligatorii' });
            return false;
        }

        if (!this.isValidPhoneNumber(hallData.phoneNumber)) {
            this.setState({
                submitError: 'Numărul de telefon nu este valid. Folosiți format românesc (ex: 0722123456, +40722123456)'
            });
            return false;
        }

        if (images.cover.length === 0) {
            this.setState({ submitError: 'Trebuie să adăugați cel puțin o imagine de copertă' });
            return false;
        }

        return true;
    }

    handleSubmit = async (e) => {
        e.preventDefault();

        if (!this.validateForm()) {
            return;
        }

        this.setState({ isSubmitting: true, submitError: null, submitSuccess: false });

        try {
            const { hallData, images } = this.state;

            const processedHallData = {
                ...hallData,
                capacity: hallData.capacity ? parseInt(hallData.capacity, 10) : 0,
                tariff: hallData.tariff ? parseFloat(hallData.tariff) : 0
            };

            const formData = new FormData();

            formData.append('sportsHall', JSON.stringify(processedHallData));

            let imageIndex = 0;
            Object.entries(images).forEach(([type, imagesList]) => {
                imagesList.forEach(img => {
                    formData.append(`images[${imageIndex}]`, img.file);
                    formData.append(`imageTypes[${imageIndex}]`, type);
                    imageIndex++;
                });
            });

            const token = localStorage.getItem('jwtToken');

            if (!token) {
                this.setState({
                    isSubmitting: false,
                    submitError: 'Nu sunteți autentificat. Trebuie să vă autentificați ca administrator de săli pentru a adăuga o sală.'
                });
                return;
            }

            console.log("FormData entries:");
            for (let entry of formData.entries()) {
                console.log(entry[0], entry[1]);
            }

            const response = await fetch(`${this.API_BASE_URL}/sportsHalls`, {
                method: 'POST',
                body: formData,
                headers: {
                    'Authorization': `Bearer ${token}`
                },
            });

            if (response.ok) {
                const data = await response.json();
                console.log('Hall created successfully:', data);

                this.setState({
                    isSubmitting: false,
                    submitSuccess: true,
                    hallData: {
                        name: '',
                        county: '',
                        city: '',
                        address: '',
                        capacity: '',
                        type: '',
                        tariff: '',
                        description: '',
                        facilities: '',
                        phoneNumber: ''
                    },
                    images: {
                        cover: [],
                        exterior: [],
                        interior: [],
                        locker: []
                    }
                });
            } else {
                const errorText = await response.text();
                console.error('Error creating hall:', errorText);
                console.error('Response status:', response.status);

                this.setState({
                    isSubmitting: false,
                    submitError: `Eroare (${response.status}): ${errorText || 'A apărut o eroare la salvarea sălii'}`
                });
            }
        } catch (error) {
            console.error('Error creating hall:', error);
            this.setState({
                isSubmitting: false,
                submitError: 'Eroare de conexiune la server: ' + error.message
            });
        }
    }

    renderImageUploadSection = (imageTypeConfig) => {
        const { type, label, description, max } = imageTypeConfig;
        const currentImages = this.state.images[type];

        return (
            <div key={type} className="image-upload-section">
                <div className="form-section-header">
                    <h2>{label} {type === 'cover' && '*'}</h2>
                    <p>{description} (Maxim {max})</p>
                </div>

                <div className="image-upload-container">
                    {currentImages.length < max && (
                        <>
                            <input
                                type="file"
                                id={`${type}Images`}
                                accept="image/*"
                                multiple={max > 1}
                                onChange={(e) => this.handleFileChange(e, type)}
                                className="image-input"
                            />
                            <label htmlFor={`${type}Images`} className="image-upload-label">
                                <ImagePlus size={36} />
                                <span>Adaugă Imagini</span>
                            </label>
                        </>
                    )}
                </div>

                {currentImages.length > 0 && (
                    <div className="image-preview-container">
                        {currentImages.map((imageObj, index) => (
                            <div key={index} className="image-preview-thumbnail">
                                <img
                                    src={imageObj.preview}
                                    alt={`${label} Preview ${index + 1}`}
                                    onClick={() => this.openImagePreview(imageObj.preview)}
                                />
                                <button
                                    type="button"
                                    className="remove-image-btn"
                                    onClick={() => this.removeImage(type, index)}
                                >
                                    <X size={12} />
                                </button>
                            </div>
                        ))}
                    </div>
                )}
            </div>
        );
    }

    render() {
        const { hallData, previewImage, isSubmitting, submitError, submitSuccess } = this.state;

        return (
            <div className="admin-page">
                <h1 className="admin-page-title">Adaugă o sală</h1>

                {submitSuccess && (
                    <div className="success-message">
                        Sala a fost adăugată cu succes!
                    </div>
                )}

                {submitError && (
                    <div className="error-message">
                        {submitError}
                    </div>
                )}

                <form onSubmit={this.handleSubmit} className="add-hall-form">
                    <div className="form-grid">
                        <div className="form-group">
                            <label htmlFor="name">Nume Sală *</label>
                            <input
                                type="text"
                                id="name"
                                name="name"
                                value={hallData.name}
                                onChange={this.handleInputChange}
                                placeholder="Numele sălii"
                                required
                            />
                        </div>

                        <div className="form-group">
                            <label htmlFor="type">Tip Sală *</label>
                            <select
                                id="type"
                                name="type"
                                value={hallData.type}
                                onChange={this.handleInputChange}
                                required
                            >
                                <option value="">Selectează tipul sălii</option>
                                <option value="fotbal">Fotbal</option>
                                <option value="baschet">Baschet</option>
                                <option value="volei">Volei</option>
                                <option value="tenis">Tenis</option>
                                <option value="multipla">Sală Multiplă</option>
                            </select>
                        </div>

                        <div className="location-row">
                            <div className="form-group">
                                <label htmlFor="county">Județ *</label>
                                <select
                                    id="county"
                                    name="county"
                                    value={hallData.county}
                                    onChange={this.handleInputChange}
                                    required
                                >
                                    <option value="">Selectează Județ</option>
                                    {COUNTIES.map(county => (
                                        <option key={county} value={county}>{county}</option>
                                    ))}
                                </select>
                            </div>

                            <div className="form-group">
                                <label htmlFor="city">Oraș/Localitate *</label>
                                <input
                                    type="text"
                                    id="city"
                                    name="city"
                                    value={hallData.city}
                                    onChange={this.handleInputChange}
                                    placeholder="Numele localității"
                                    required
                                />
                            </div>
                        </div>

                        <div className="form-group full-width">
                            <label htmlFor="address">Adresă *</label>
                            <input
                                type="text"
                                id="address"
                                name="address"
                                value={hallData.address}
                                onChange={this.handleInputChange}
                                placeholder="Adresa completă"
                                required
                            />
                        </div>

                        <div className="form-group">
                            <label htmlFor="phoneNumber">Număr de telefon *</label>
                            <input
                                type="tel"
                                id="phoneNumber"
                                name="phoneNumber"
                                value={hallData.phoneNumber}
                                onChange={this.handleInputChange}
                                placeholder="0722123456 sau +40722123456"
                                required
                            />
                        </div>

                        <div className="form-group">
                            <label htmlFor="capacity">Capacitate spectatori *</label>
                            <input
                                type="number"
                                id="capacity"
                                name="capacity"
                                value={hallData.capacity}
                                onChange={this.handleInputChange}
                                placeholder="Capacitate maximă tribune"
                                min="1"
                                required
                            />
                        </div>

                        <div className="form-group">
                            <label htmlFor="tariff">Tarif (Lei/1h 30min) *</label>
                            <input
                                type="number"
                                id="tariff"
                                name="tariff"
                                value={hallData.tariff}
                                onChange={this.handleInputChange}
                                placeholder="Tarif pe o oră și jumătate"
                                min="0"
                                step="0.01"
                                required
                            />
                        </div>

                        <div className="form-group full-width">
                            <label htmlFor="description">Descriere</label>
                            <textarea
                                id="description"
                                name="description"
                                value={hallData.description}
                                onChange={this.handleInputChange}
                                placeholder="Adaugă o descriere detaliată a sălii"
                                rows="4"
                            />
                        </div>

                        <div className="form-group full-width">
                            <label htmlFor="facilities">Facilități</label>
                            <textarea
                                id="facilities"
                                name="facilities"
                                value={hallData.facilities}
                                onChange={this.handleInputChange}
                                placeholder="Enumeră facilitățile sălii"
                                rows="3"
                            />
                        </div>
                    </div>

                    {IMAGE_TYPES.map(this.renderImageUploadSection)}

                    <div className="form-actions">
                        <button
                            type="submit"
                            className="hall-action-button edit"
                            disabled={isSubmitting}
                        >
                            {isSubmitting ? (
                                'Se salvează...'
                            ) : (
                                <>
                                    <Plus size={20} /> Adaugă Sală
                                </>
                            )}
                        </button>
                    </div>
                </form>

                {previewImage && (
                    <div className="image-preview-modal" onClick={this.closeImagePreview}>
                        <div className="image-preview-modal-content">
                            <button
                                className="close-preview-btn"
                                onClick={this.closeImagePreview}
                            >
                                <X size={24} />
                            </button>
                            <img src={previewImage} alt="Preview" />
                        </div>
                    </div>
                )}
            </div>
        );
    }
}

export default AddHallPage;