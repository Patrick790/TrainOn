import React, { Component } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { Camera, X, ImagePlus, ChevronLeft, ChevronRight, Trash2 } from 'lucide-react';
import './EditHallPage.css';

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
        description: 'Imagine principală a sălii (max. 10MB)',
        max: 1
    },
    {
        type: 'exterior',
        label: 'Imagine Exterior',
        description: 'Imagine cu fațada sălii (max. 10MB)',
        max: 1
    },
    {
        type: 'interior',
        label: 'Imagini Interior',
        description: 'Imagini din interiorul sălii (max. 10MB/imagine)',
        max: 4
    },
    {
        type: 'locker',
        label: 'Imagini Vestiare',
        description: 'Imagini cu vestiare (max. 10MB/imagine)',
        max: 2
    }
];


// Wrapper pentru a folosi hooks in component class
function withRouter(Component) {
    function ComponentWithRouterProp(props) {
        let params = useParams();
        let navigate = useNavigate();
        return <Component {...props} params={params} navigate={navigate} />;
    }
    return ComponentWithRouterProp;
}

class EditHallPage extends Component {
    constructor(props) {
        super(props);
        this.state = {
            hallId: props.params.id,
            hallData: {
                name: '',
                county: '',
                city: '',
                address: '',
                capacity: '',
                type: '',
                tariff: '',
                description: '',
                facilities: ''
            },
            existingImages: {
                cover: [],
                exterior: [],
                interior: [],
                locker: []
            },
            newImages: {
                cover: [],
                exterior: [],
                interior: [],
                locker: []
            },
            imageIdsToDelete: [],
            activeImageType: null,
            currentImageIndex: 0,
            isImageViewerOpen: false,
            isLoading: true,
            isSubmitting: false,
            submitError: null,
            submitSuccess: false
        };
    }

    async componentDidMount() {
        await this.fetchHallData();
    }

    fetchHallData = async () => {
        try {
            const { hallId } = this.state;
            const token = localStorage.getItem('jwtToken');

            if (!token) {
                this.setState({
                    submitError: 'Nu sunteți autentificat. Vă rugăm să vă autentificați pentru a edita sala.',
                    isLoading: false
                });
                return;
            }

            const response = await fetch(`http://localhost:8080/sportsHalls/${hallId}`, {
                method: 'GET',
                headers: {
                    Authorization: `Bearer ${token}`
                }
            });

            if (!response.ok) {
                throw new Error(`Eroare la încărcarea sălii: ${response.status}`);
            }

            const hallData = await response.json();
            console.log('Hall data loaded:', hallData);

            // Organizarea imaginilor pe categorii
            const existingImages = {
                cover: [],
                exterior: [],
                interior: [],
                locker: []
            };

            if (hallData.images && hallData.images.length > 0) {
                hallData.images.forEach(img => {
                    const type = img.description;
                    if (existingImages[type]) {
                        existingImages[type].push({
                            id: img.id,
                            url: `http://localhost:8080/images/${img.id}`,
                            type: img.description
                        });
                    }
                });
            }

            // Actualizam starea cu datele incarcate
            this.setState({
                hallData: {
                    name: hallData.name || '',
                    county: hallData.county || '',
                    city: hallData.city || '',
                    address: hallData.address || '',
                    capacity: hallData.capacity || '',
                    type: hallData.type || '',
                    tariff: hallData.tariff || '',
                    description: hallData.description || '',
                    facilities: hallData.facilities || ''
                },
                existingImages,
                isLoading: false
            });
        } catch (error) {
            console.error('Error fetching hall data:', error);
            this.setState({
                submitError: 'Eroare la incarcarea datelor salii',
                isLoading: false
            });
        }
    };

    handleInputChange = (e) => {
        const { name, value } = e.target;
        this.setState(prevState => ({
            hallData: {
                ...prevState.hallData,
                [name]: value
            }
        }));
    };

    handleFileChange = (e, imageType) => {
        const newFiles = Array.from(e.target.files);

        this.setState(prevState => {
            const currentCount = prevState.newImages[imageType].length + prevState.existingImages[imageType].length;
            const typeConfig = IMAGE_TYPES.find(type => type.type === imageType);
            const maxAllowed = typeConfig.max;

            const filesToAdd = newFiles.slice(0, maxAllowed - currentCount);

            return {
                newImages: {
                    ...prevState.newImages,
                    [imageType]: [
                        ...prevState.newImages[imageType],
                        ...filesToAdd.map(file => ({
                            file,
                            preview: URL.createObjectURL(file),
                            type: imageType
                        }))
                    ]
                }
            };
        });
    };

    removeExistingImage = (imageType, index) => {
        this.setState(prevState => {
            const imageToRemove = prevState.existingImages[imageType][index];
            return {
                existingImages: {
                    ...prevState.existingImages,
                    [imageType]: prevState.existingImages[imageType].filter((_, i) => i !== index)
                },
                imageIdsToDelete: [...prevState.imageIdsToDelete, imageToRemove.id]
            };
        });
    };

    removeNewImage = (imageType, index) => {
        this.setState(prevState => ({
            newImages: {
                ...prevState.newImages,
                [imageType]: prevState.newImages[imageType].filter((_, i) => i !== index)
            }
        }));
    };

    openImageViewer = (imageType, index, isExisting = true) => {
        this.setState({
            activeImageType: imageType,
            currentImageIndex: index,
            isImageViewerOpen: true,
            viewingExistingImages: isExisting
        });
    };

    closeImageViewer = () => {
        this.setState({
            isImageViewerOpen: false,
            activeImageType: null,
            currentImageIndex: 0
        });
    };

    nextImage = () => {
        const { activeImageType, currentImageIndex, viewingExistingImages } = this.state;
        const images = viewingExistingImages
            ? this.state.existingImages[activeImageType]
            : this.state.newImages[activeImageType];

        if (currentImageIndex < images.length - 1) {
            this.setState(prevState => ({
                currentImageIndex: prevState.currentImageIndex + 1
            }));
        }
    };

    prevImage = () => {
        if (this.state.currentImageIndex > 0) {
            this.setState(prevState => ({
                currentImageIndex: prevState.currentImageIndex - 1
            }));
        }
    };

    validateForm = () => {
        const { hallData } = this.state;
        const { existingImages, newImages } = this.state;

        // Verificăm dacă toate câmpurile obligatorii sunt completate
        if (!hallData.name || !hallData.county || !hallData.city ||
            !hallData.address || !hallData.capacity || !hallData.type || !hallData.tariff) {
            this.setState({ submitError: 'Toate câmpurile marcate cu * sunt obligatorii' });
            return false;
        }

        // Verificăm dacă avem cel puțin o imagine de copertă (existentă sau nouă)
        if (existingImages.cover.length === 0 && newImages.cover.length === 0) {
            this.setState({ submitError: 'Trebuie să existe cel puțin o imagine de copertă' });
            return false;
        }

        return true;
    };

    prepareFormData = () => {
        const { hallData, newImages, imageIdsToDelete, hallId } = this.state;

        // Asigură-te că valorile numerice sunt numere, nu string-uri
        const processedHallData = {
            ...hallData,
            id: hallId, // Includem explicit ID-ul sălii
            capacity: hallData.capacity ? parseInt(hallData.capacity, 10) : 0,
            tariff: hallData.tariff ? parseFloat(hallData.tariff) : 0
        };

        // Creăm un FormData pentru a trimite date multipart
        const formData = new FormData();

        // Adăugăm datele sălii ca JSON
        formData.append('sportsHall', JSON.stringify(processedHallData));

        // Adăugăm IDs imaginilor de șters DOAR dacă există cel puțin una
        if (imageIdsToDelete && imageIdsToDelete.length > 0) {
            formData.append('deleteImageIds', JSON.stringify(imageIdsToDelete));
        }

        // Verificăm dacă există imagini noi pentru a le adăuga
        let hasNewImages = false;
        Object.values(newImages).forEach(imagesList => {
            if (imagesList.length > 0) {
                hasNewImages = true;
            }
        });

        // Adăugăm imaginile noi și tipurile lor DOAR dacă există cel puțin una
        if (hasNewImages) {
            let imageIndex = 0;
            Object.entries(newImages).forEach(([type, imagesList]) => {
                imagesList.forEach(img => {
                    formData.append(`newImages[${imageIndex}]`, img.file);
                    formData.append(`newImageTypes[${imageIndex}]`, type);
                    imageIndex++;
                });
            });
        }

        return formData;
    };

    handleSubmit = async (e) => {
        e.preventDefault();

        // Validăm datele
        if (!this.validateForm()) {
            return;
        }

        this.setState({ isSubmitting: true, submitError: null, submitSuccess: false });

        try {
            const formData = this.prepareFormData();
            const token = localStorage.getItem('jwtToken');
            const { hallId } = this.state;

            if (!token) {
                this.setState({
                    isSubmitting: false,
                    submitError: 'Nu sunteți autentificat'
                });
                return;
            }

            // Pentru debugging - verifică ce conține FormData
            console.log("FormData entries:");
            for (let entry of formData.entries()) {
                console.log(entry[0], entry[1]);
            }

            const response = await fetch(`http://localhost:8080/sportsHalls/${hallId}`, {
                method: 'PUT',
                body: formData,
                headers: {
                    'Authorization': `Bearer ${token}`
                }
            });

            if (response.ok) {
                const data = await response.json();
                console.log('Hall updated successfully:', data);

                // Salvăm datele actualizate direct în stare
                const existingImages = {
                    cover: [],
                    exterior: [],
                    interior: [],
                    locker: []
                };

                if (data.images && data.images.length > 0) {
                    data.images.forEach(img => {
                        const type = img.description;
                        if (existingImages[type]) {
                            existingImages[type].push({
                                id: img.id,
                                url: `http://localhost:8080/images/${img.id}`,
                                type: img.description
                            });
                        }
                    });
                }

                // Actualizăm starea cu datele noi și resetăm listele de imagini
                this.setState({
                    hallData: {
                        name: data.name || '',
                        county: data.county || '',
                        city: data.city || '',
                        address: data.address || '',
                        capacity: data.capacity || '',
                        type: data.type || '',
                        tariff: data.tariff || '',
                        description: data.description || '',
                        facilities: data.facilities || ''
                    },
                    existingImages,  // Actualizăm direct cu imaginile noi
                    imageIdsToDelete: [], // Resetăm lista de imagini de șters
                    newImages: {  // Resetăm lista de imagini noi
                        cover: [],
                        exterior: [],
                        interior: [],
                        locker: []
                    },
                    isSubmitting: false,
                    submitSuccess: true
                });
            } else {
                // Gestionăm eroarea
                const errorText = await response.text();
                console.error('Error updating hall:', errorText);
                console.error('Response status:', response.status);

                this.setState({
                    isSubmitting: false,
                    submitError: `Eroare (${response.status}): ${errorText || 'A apărut o eroare la actualizarea sălii'}`
                });
            }
        } catch (error) {
            console.error('Error updating hall:', error);
            this.setState({
                isSubmitting: false,
                submitError: 'Eroare de conexiune la server: ' + error.message
            });
        }
    };

    handleCancel = () => {
        this.props.navigate('/hall-admin-dashboard');
    };

    renderImageSection = () => {
        const { existingImages, newImages } = this.state;

        return (
            <div className="edit-hall-images-section">
                <h2 className="section-title">Imagini</h2>

                <div className="image-categories">
                    {IMAGE_TYPES.map(type => (
                        <div key={type.type} className="image-category">
                            <h3>{type.label} {type.type === 'cover' && '*'}</h3>
                            <p className="image-description">{type.description} (Maxim {type.max})</p>

                            <div className="images-container">
                                {/* Imagini existente */}
                                {existingImages[type.type].map((img, index) => (
                                    <div key={`existing-${index}`} className="image-item">
                                        <div className="image-preview">
                                            <img
                                                src={img.url}
                                                alt={`${type.label} ${index + 1}`}
                                                onClick={() => this.openImageViewer(type.type, index, true)}
                                            />
                                            <button
                                                type="button"
                                                className="delete-image-btn"
                                                onClick={() => this.removeExistingImage(type.type, index)}
                                            >
                                                <Trash2 size={16} />
                                            </button>
                                        </div>
                                    </div>
                                ))}

                                {/* Imagini noi */}
                                {newImages[type.type].map((img, index) => (
                                    <div key={`new-${index}`} className="image-item">
                                        <div className="image-preview">
                                            <img
                                                src={img.preview}
                                                alt={`${type.label} nou ${index + 1}`}
                                                onClick={() => this.openImageViewer(type.type, index, false)}
                                            />
                                            <button
                                                type="button"
                                                className="delete-image-btn"
                                                onClick={() => this.removeNewImage(type.type, index)}
                                            >
                                                <Trash2 size={16} />
                                            </button>
                                        </div>
                                    </div>
                                ))}

                                {/* Buton adăugare imagine nouă */}
                                {(existingImages[type.type].length + newImages[type.type].length) < type.max && (
                                    <div className="add-image-container">
                                        <label htmlFor={`add-${type.type}`} className="add-image-label">
                                            <ImagePlus size={24} />
                                            <span>Adaugă</span>
                                            <input
                                                type="file"
                                                id={`add-${type.type}`}
                                                accept="image/*"
                                                onChange={(e) => this.handleFileChange(e, type.type)}
                                                className="hidden-input"
                                            />
                                        </label>
                                    </div>
                                )}
                            </div>
                        </div>
                    ))}
                </div>
            </div>
        );
    };

    renderImageViewer = () => {
        const {
            isImageViewerOpen,
            activeImageType,
            currentImageIndex,
            viewingExistingImages
        } = this.state;

        if (!isImageViewerOpen || !activeImageType) return null;

        const images = viewingExistingImages
            ? this.state.existingImages[activeImageType]
            : this.state.newImages[activeImageType];

        if (images.length === 0 || currentImageIndex >= images.length) return null;

        const currentImage = images[currentImageIndex];
        const typeConfig = IMAGE_TYPES.find(type => type.type === activeImageType);

        return (
            <div className="image-viewer-overlay" onClick={this.closeImageViewer}>
                <div className="image-viewer-container" onClick={e => e.stopPropagation()}>
                    <div className="image-viewer-header">
                        <h3>{typeConfig.label}</h3>
                        <button className="close-viewer-btn" onClick={this.closeImageViewer}>
                            <X size={24} />
                        </button>
                    </div>

                    <div className="image-viewer-content">
                        <button
                            className={`nav-btn prev ${currentImageIndex === 0 ? 'disabled' : ''}`}
                            onClick={this.prevImage}
                            disabled={currentImageIndex === 0}
                        >
                            <ChevronLeft size={24} />
                        </button>

                        <div className="main-image-container">
                            <img
                                src={viewingExistingImages ? currentImage.url : currentImage.preview}
                                alt={`${typeConfig.label} ${currentImageIndex + 1}`}
                                className="main-image"
                            />
                        </div>

                        <button
                            className={`nav-btn next ${currentImageIndex === images.length - 1 ? 'disabled' : ''}`}
                            onClick={this.nextImage}
                            disabled={currentImageIndex === images.length - 1}
                        >
                            <ChevronRight size={24} />
                        </button>
                    </div>

                    <div className="image-counter">
                        {currentImageIndex + 1} / {images.length}
                    </div>
                </div>
            </div>
        );
    };

    render() {
        const {
            hallData,
            isLoading,
            isSubmitting,
            submitError,
            submitSuccess
        } = this.state;

        if (isLoading) {
            return <div className="loading-container">Se încarcă datele sălii...</div>;
        }

        return (
            <div className="edit-hall-page">
                <div className="edit-hall-header">
                    <h1>Editare Sală: {hallData.name}</h1>
                    <button
                        className="back-button"
                        onClick={this.handleCancel}
                    >
                        Înapoi la Sălile Mele
                    </button>
                </div>

                {submitSuccess && (
                    <div className="success-message">
                        Sala a fost actualizată cu succes!
                    </div>
                )}

                {submitError && (
                    <div className="error-message">
                        {submitError}
                    </div>
                )}

                <form onSubmit={this.handleSubmit} className="edit-hall-form">
                    {/* Secțiunea de imagini */}
                    {this.renderImageSection()}

                    {/* Secțiunea de informații */}
                    <div className="hall-info-section">
                        <h2 className="section-title">Informații despre sală</h2>

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
                    </div>

                    <div className="form-actions">
                        <button
                            type="button"
                            className="cancel-button"
                            onClick={this.handleCancel}
                        >
                            Anulează
                        </button>
                        <button
                            type="submit"
                            className="save-button"
                            disabled={isSubmitting}
                        >
                            {isSubmitting ? 'Se salvează...' : 'Salvează Modificările'}
                        </button>
                    </div>
                </form>

                {/* Image Viewer Modal */}
                {this.renderImageViewer()}
            </div>
        );
    }
}

export default withRouter(EditHallPage);