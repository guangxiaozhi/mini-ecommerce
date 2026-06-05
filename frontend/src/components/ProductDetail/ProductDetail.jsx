import { useEffect, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { getProduct } from '../../api/products';
import { createConversation } from '../../api/chat.js';
import Stars from "../Stars/Stars.jsx";
import {listProductReviews} from "../../api/reviews.js";
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

function Reviews({productId, ratingAvg, reviewCount}){
    const [page, setPage] = useState(0);
    const [sort, setSort] = useState('newest');
    const [data, setData] = useState({content:[], totalPages:0, totalElements:0});
    const [error, setError] = useState(null);
    const [loading, setLoading] = useState(true);

    useEffect(()=>{
        let cancelled = false;
        setLoading(true);
        setError(null);
        listProductReviews(productId, {page,size:10, sort})
            .then(d=>{if(!cancelled) setData(d);})
            .catch(e=>{if(!cancelled) setError(e.message);})
            .finally(()=>{if(!cancelled) setLoading(false);});
        return ()=>{cancelled = true;};
    }, [productId, page, sort]);

    function handleSort(e) {
        setSort(e.target.value);
        setPage(0);
    }

    return(
        <section className="pd-reviews">
            <h2 className="pd-reviews__title">Customer Reviews</h2>
            <div className="pd-reviews__summary">
                {reviewCount > 0
                    ? <>
                        <Stars rating={ratingAvg ?? 0} size="lg" />
                        <span className="pd-reviews__avg">{Number(ratingAvg).toFixed(1)} out of 5</span>
                        <span className="pd-reviews__count">({reviewCount.toLocaleString()} ratings)</span>
                    </>
                    : <span className="pd-reviews__none">No reviews yet</span>}
             </div>
             {reviewCount > 0 && (
                 <div className="pd-reviews__controls">
                     <label className="pd-reviews__sort-label">
                         Sort by:&nbsp;
                         <select value={sort} onChange={handleSort} className="pd-reviews__sort-select">
                             <option value="newest">Newest</option>
                             <option value="oldest">Oldest</option>
                             <option value="highest">Highest rated</option>
                             <option value="lowest">Lowest rated</option>
                         </select>
                     </label>
                 </div>
             )}
             {error && <div className="pd-reviews__error">{error}</div>}
             {loading && <div className="pd-reviews__loading">Loading…</div>}
             {!loading && data.content.length === 0 && reviewCount === 0 && (
                <p className="pd-reviews__empty">Be the first to review this product.</p>
             )}
             <ul className="pd-reviews__list">
                 {data.content.map(r => (
                     <li key={r.id} className="pd-review">
                         <div className="pd-review__head">
                             <Stars rating={r.rating} size="sm" />
                             <span className="pd-review__user">{r.username}</span>
                             {r.verifiedPurchase && <span className="pd-review__badge">Verified Purchase</span>}
                             {r.edited && <span className="pd-review__edited">(edited)</span>}
                         </div>
                         <p className="pd-review__comment">{r.comment}</p>
                         <p className="pd-review__date">{new Date(r.createdAt).toLocaleDateString()}</p>
                     </li>
                 ))}
             </ul>
             {data.totalPages > 1 && (
                 <div className="pd-reviews__pager">
                     <button disabled={page === 0} onClick={() => setPage(p=> p - 1)}>Prev</button>
                     <span>Page {page + 1} of {data.totalPages}</span>
                     <button disabled={page + 1 >= data.totalPages} onClick ={() => setPage(p => p + 1)}>Next</button>
                 </div>
             )}
        </section>
    );
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
    const [chatLoading, setChatLoading] = useState(false);

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

    async function handleContactSupport() {
        if (!isLoggedIn) {
            onNeedAuth?.();
            return;
        }
        const token = localStorage.getItem('token');
        if (!token) {
            onNeedAuth?.();
            return;
        }
        setChatLoading(true);
        setError(null);
        try {
            const conv = await createConversation(token, {
                type: 'INQUIRY',
                productId: Number(id),
            });
            navigate(`/chat/${conv.id}`, { state: { conversation: conv } });
        } catch (e) {
            setError(e.message || 'Could not start chat.');
        } finally {
            setChatLoading(false);
        }
    }

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
                        <button
                            type="button"
                            className="pd-btn-chat"
                            disabled={chatLoading}
                            onClick={handleContactSupport}
                        >
                            {chatLoading ? 'Starting chat...' : 'Contact customer service'}
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