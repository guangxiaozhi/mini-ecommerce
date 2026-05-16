import { useState } from 'react';
import './ProductCard.css';
import {useNavigate} from "react-router-dom";
import Stars from "../Stars/Stars.jsx";

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

const getColor       = id => COLORS[(id - 1) % COLORS.length];

function stockInfo(stock) {
    if (stock <= 0) return { label: 'Out of Stock',        cls: 'pc-stock--out' };
    if (stock <= 5) return { label: `Only ${stock} left!`, cls: 'pc-stock--low' };
    return              { label: 'In Stock',               cls: 'pc-stock--ok'  };
}

export default function ProductCard({ product, isLoggedIn, onAdd, onNeedAuth }) {
    const [status, setStatus] = useState('idle');
    const [errMsg, setErrMsg] = useState('');

    const color      = getColor(product.id);
    const stock      = stockInfo(product.stock);
    const outOfStock = product.stock <= 0;
    const navigate = useNavigate();
    const reviewCount = Number(product.reviewCount) || 0;
    const  ratingAvg = product.ratingAvg != null ? Number(product.ratingAvg): null;

    async function handleAdd() {
        if (!isLoggedIn)                       { onNeedAuth(); return; }
        if (outOfStock || status === 'adding') return;

        setStatus('adding');
        setErrMsg('');
        try {
            await onAdd(product.id);
            setStatus('added');
            setTimeout(() => setStatus('idle'), 2000);
        } catch (e) {
            setErrMsg(e.message || 'Could not add to cart');
            setStatus('error');
            setTimeout(() => setStatus('idle'), 3000);
        }
    }

    function btnLabel() {
        if (!isLoggedIn)         return 'Sign in to buy';
        if (outOfStock)          return 'Out of Stock';
        if (status === 'adding') return 'Adding…';
        if (status === 'added')  return '✓ Added to Cart';
        return 'Add to Cart';
    }

    return (
        <article
            className={`pc-card${outOfStock ? ' pc-card--oos' : ''}`}
            onClick={() => navigate(`/products/${product.id}`)}
            style={{ cursor: 'pointer' }}
        >

            <div className="pc-card__image" style={!product.images?.length ? { background: color.bg } : {}}>
                {product.images?.length
                    ? <img src={product.images[0].imageUrl} alt={product.name} className="pc-card__img" />
                    : <span className="pc-card__initial" style={{ color: color.text }}>
                           {product.name.charAt(0).toUpperCase()}
                      </span>
                }
            </div>

            <div className="pc-card__body">
                <h3 className="pc-card__name"
                    title={product.name}
                >
                    {product.name}
                </h3>

                <div className="pc-card__rating">
                    {reviewCount >0
                        ? <>
                            <Stars rating={ratingAvg ?? 0} />
                            <span className="pc-card__review-count">
                                ({reviewCount.toLocaleString()})
                            </span>
                        </>
                        : <span className="pc-card__no-reviews">No reviews yet </span>}
                </div>

                <div className="pc-card__price">
                    ${Number(product.price).toFixed(2)}
                </div>

                <div className={`pc-card__stock ${stock.cls}`}>
                    {stock.label}
                </div>

                {errMsg && <div className="pc-card__err">{errMsg}</div>}

                <button
                    type="button"
                    className={`pc-card__btn${status === 'added' ? ' pc-card__btn--added' : ''}`}
                    disabled={outOfStock || status === 'adding'}
                    onClick={(e)=>{e.stopPropagation(); handleAdd();}}
                >
                    {btnLabel()}
                </button>
            </div>
        </article>
    );
}