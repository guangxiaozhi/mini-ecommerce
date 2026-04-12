import { useEffect, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { getProduct } from '../../api/products';
import './ProductDetail.css';

const COLORS = [
    { bg: 'linear-gradient(135deg,#dbeafe,#bfdbfe)', text: '#1d4ed8' },
    { bg: 'linear-gradient(135deg,#dcfce7,#bbf7d0)', text: '#15803d' },
    { bg: 'linear-gradient(135deg,#fef3c7,#fde68a)', text: '#b45309' },
    { bg: 'linear-gradient(135deg,#fce7f3,#fbcfe8)', text: '#be185d' },
    { bg: 'linear-gradient(135deg,#ede9fe,#ddd6fe)', text: '#6d28d9' },
    { bg: 'linear-gradient(135deg,#e0f2fe,#bae6fd)', text: '#0369a1' },
    { bg: 'linear-gradient(135deg,#fff7ed,#fed7aa)', text: '#c2410c' },
    { bg: 'linear-gradient(135deg,#f0fdf4,#bbf7d0)', text: '#166534' },
];

const getColor = id => COLORS[(id - 1) % COLORS.length];

function stockInfo(stock) {
    if (stock <= 0) return { label: 'Out of Stock',        cls: 'pd-stock--out' };
    if (stock <= 5) return { label: `Only ${stock} left!`, cls: 'pd-stock--low' };
    return              { label: 'In Stock',               cls: 'pd-stock--ok'  };
}

export default function ProductDetail({ isLoggedIn, onAdd, onNeedAuth }) {
    const { id } = useParams();
    const navigate = useNavigate();
    const [product, setProduct] = useState(null);
    const [loading, setLoading] = useState(true);
    const [showToast, setShowToast] = useState(false)
    const [error, setError] = useState(null);
    const [selectedImage, setSelectedImage] = useState(null);
    const [showFullView, setShowFullView] = useState(false);

    useEffect(() => {
        setSelectedImage(null)
        getProduct(id)
            .then(setProduct)
            .catch(() => setError('Product not found.'))
            .finally(() => setLoading(false));
    }, [id]);

    if (loading) return <p>Loading...</p>;
    if (error)   return <p>{error}</p>;

    const color = getColor(product.id);
    const stock = stockInfo(product.stock);

    // split bullets into highlights row and content rows
    const highlights = product.bullets?.find(b => b.brand || b.weight || b.dimension);
    const bulletPoints = product.bullets?.filter(b => b.content) ?? [];

    // pick primary image, fallback to first image
    const primaryImage = product.images?.find(img => img.isPrimary) ?? product.images?.[0];
    const sortedImages = product.images
        ?[...product.images].sort((a,b)=>(a.sortOrder ?? 0) - (b.sortOrder ?? 0))
        : [];
    const displayImage = selectedImage ?? primaryImage;

    return (
        <div className="pd-page">
            <div className="pd-layout">

                {/* LEFT — image */}
                <div className="pd-col-left">
                    {primaryImage ? (
                        <div className="pd-image-section">

                            {/* Thumbnail strip — only shown when >1 image */}
                            {sortedImages.length > 1 && (
                                <div className="pd-thumbnails">
                                    {sortedImages.slice(0, 5).map(img => (
                                        <button
                                            key={img.id}
                                            className={`pd-thumb ${displayImage?.id === img.id ? 'pd-thumb--active' : ''}`}
                                            onMouseEnter={() => setSelectedImage(img)}
                                        >
                                            <img src={img.imageUrl} alt="" />
                                        </button>
                                    ))}
                                    {sortedImages.length > 5 && (
                                        <button className="pd-thumb pd-thumb--more" onClick={() => setShowFullView(true)}>
                                            {sortedImages.length - 5}+
                                        </button>
                                    )}
                                </div>
                            )}

                            {/* Main image + full view button */}
                            <div className="pd-image-main">
                                <div className="pd-image">
                                    <img src={displayImage.imageUrl} alt={product.name} className="pd-image__img" />
                                </div>
                                <button className="pd-fullview-btn" onClick={() => setShowFullView(true)}>
                                    Click to see full view
                                </button>
                            </div>

                        </div>
                    ) : (
                        <div className="pd-image" style={{ background: color.bg }}>
                            <span className="pd-image__initial" style={{ color: color.text }}>
                                {product.name.charAt(0).toUpperCase()}
                            </span>
                        </div>
                    )}
                </div>

                {/* MIDDLE — product info */}
                <div className="pd-col-mid">
                    <h1 className="pd-name">{product.name}</h1>
                    <p className="pd-description">{product.description}</p>
                    <div className="pd-price">${Number(product.price).toFixed(2)}</div>

                    {/* Top highlights */}
                    {highlights && (
                        <div className="pd-highlights">
                            <h3 className="pd-highlights__title">Top highlights</h3>
                            <table className="pd-highlights__table">
                                <tbody>
                                {highlights.brand && (
                                    <tr>
                                        <td className="pd-highlights__label">Brand</td>
                                        <td>{highlights.brand}</td>
                                    </tr>
                                )}
                                {highlights.weight && (
                                    <tr>
                                        <td className="pd-highlights__label">Item Weight</td>
                                        <td>{highlights.weight}</td>
                                    </tr>
                                )}
                                {highlights.dimension && (
                                    <tr>
                                        <td className="pd-highlights__label">Item</td>
                                        <td>{highlights.dimension}</td>
                                    </tr>
                                )}
                                </tbody>
                            </table>
                        </div>
                    )}

                    {/* About this item */}
                    {bulletPoints.length > 0 && (
                        <div className="pd-bullets">
                            <h3 className="pd-bullets__title">About this item</h3>
                            <ul className="pd-bullets__list">
                                {bulletPoints.map(b => (
                                    <li key={b.id}>{b.content}</li>
                                ))}
                            </ul>
                        </div>
                    )}
                </div>

                {/* RIGHT — price, shipping, add to cart */}
                <div className="pd-col-right">
                    <div className="pd-right-card">
                        <div className="pd-right-price">${Number(product.price).toFixed(2)}</div>
                        <div className={`pd-right-stock ${stock.cls}`}>{stock.label}</div>

                        {product.shippingOptions?.length > 0 && (
                            <div className="pd-shipping">
                                {product.shippingOptions.map(s => (
                                    <div key={s.id} className="pd-shipping__row">
                                        <span className="pd-shipping__label">{s.label}</span>
                                        {s.description && <span className="pd-shipping__desc">{s.description}</span>}
                                    </div>
                                ))}
                            </div>
                        )}

                        <button
                            className="pd-btn-cart"
                            disabled={product.stock <= 0 }
                            onClick={async () => {
                                if (!isLoggedIn) { onNeedAuth?.(); return; }
                                try{
                                    await onAdd?.(product.id);
                                    setShowToast(true);
                                    setTimeout(()=>setShowToast(false),2000);
                                }catch{}
                            }}
                        >
                            {product.stock <= 0 ? 'Out of Stock' : 'Add to Cart'}
                        </button>
                    </div>
                </div>

            </div>
            {showToast && (
                <div className="pd-toast">
                    <span className="pd-toast__check">✓</span>Item added to cart!
                </div>
            )}
            {showFullView && displayImage && (
                <div className="pd-modal" onClick={() => setShowFullView(false)}>
                    <div className="pd-modal__content" onClick={e => e.stopPropagation()}>
                        <button className="pd-modal__close" onClick={() => setShowFullView(false)}>✕</button>

                        <div className="pd-modal__body">

                            {/* Left — selected image */}
                            <div className="pd-modal__left">
                                <img src={displayImage.imageUrl} alt={product.name} className="pd-modal__img" />
                            </div>

                            {/* Right — description + thumbnails */}
                            <div className="pd-modal__right">
                                <div className="pd-modal__info">
                                    <h2 className="pd-modal__name">{product.name}</h2>
                                    <p className="pd-modal__desc">{product.description}</p>
                                </div>
                                <div className="pd-modal__thumbs">
                                    {sortedImages.map(img => (
                                        <button
                                            key={img.id}
                                            className={`pd-thumb ${displayImage?.id === img.id ? 'pd-thumb--active' : ''}`}
                                            onClick={() => setSelectedImage(img)}
                                        >
                                            <img src={img.imageUrl} alt="" />
                                        </button>
                                    ))}
                                </div>
                            </div>

                        </div>
                    </div>
                </div>
            )}
        </div>
    );
}