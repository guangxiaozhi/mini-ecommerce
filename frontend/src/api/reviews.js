
const PRODUCTS_BASE = `/api/products`;
const ITEMS_BASE = `/api/order-items`;
const REVIEWS_BASE = `/api/reviews`;
const ACCOUNT_REVIEWS_BASE = `/api/account/reviews`;
const ADMIN_REVIEWS_BASE = `/api/admin/reviews`;

async function handle(res){
    let data;
    try{data = await  res.json();} catch{data = null;}
    if(!res.ok) throw new Error(data ?.message || `${res.status} ${res.statusText}`);
    return data;
}

function authHeaders(token){
    return {'Authorization': `Bearer ${token}`};
}

function jsonHeaders(token){
    return{'Content-Type': 'application/json', ...authHeaders(token)};
}

//Public
export function listProductReviews(productId, { page = 0, size = 10, sort= 'newest' } = {}) {
         const url = `${PRODUCTS_BASE}/${productId}/reviews?page=${page}&size=${size}&sort=${encodeURIComponent(sort)}`;
         return fetch(url).then(handle);
}

 // Author
export function getEligibility(token, orderItemId) {
         return fetch(`${ITEMS_BASE}/${orderItemId}/review/eligibility`, { headers: authHeaders(token) }).then(handle);
}

export function createReview(token, orderItemId, { rating, comment }) {
         return fetch(`${ITEMS_BASE}/${orderItemId}/review`, {
                 method: 'POST',
                 headers: jsonHeaders(token),
                 body: JSON.stringify({ rating, comment }),
             }).then(handle);
}

export function updateReview(token, reviewId, { rating, comment }) {
         return fetch(`${REVIEWS_BASE}/${reviewId}`, {
                 method: 'PATCH',
                 headers: jsonHeaders(token),
                 body: JSON.stringify({ rating, comment }),
             }).then(handle);
}

export function deleteReview(token, reviewId) {
         return fetch(`${REVIEWS_BASE}/${reviewId}`, {
                 method: 'DELETE',
                 headers: authHeaders(token),
             }).then(async (res) => {
                 if (!res.ok) {
                         let msg = `${res.status} ${res.statusText}`;
                         try { const d = await res.json(); msg = d?.message ?? msg; } catch {}
                         throw new Error(msg);
                     }
             });
}

export function listMyReviews(token, { page = 0, size = 10, sort = 'newest' } = {}) {
         const url = `${ACCOUNT_REVIEWS_BASE}?page=${page}&size=${size}&sort=${
        encodeURIComponent(sort)}`;
         return fetch(url, { headers: authHeaders(token) }).then(handle);
}

 // Admin
export function listAdminReviews(token, { productName, username, hiddenOnly =false, page = 0, size = 10, sort = 'newest' } = {}) {
         const params = new URLSearchParams({ page, size, sort, hiddenOnly });
         if (productName) params.set('productName', productName);
         if (username) params.set('username', username);
         return fetch(`${ADMIN_REVIEWS_BASE}?${params}`, { headers: authHeaders(token) }).then(handle);
}

export function adminHideReview(token, reviewId) {
         return fetch(`${ADMIN_REVIEWS_BASE}/${reviewId}/hide`, { method: 'POST', headers: authHeaders(token) })
                 .then(async (res) => { if (!res.ok) throw new Error(`${res.status}`); });
}
    
export function adminUnhideReview(token, reviewId) {
             return fetch(`${ADMIN_REVIEWS_BASE}/${reviewId}/unhide`, { method: 'POST', headers: authHeaders(token) })
            .then(async (res) => { if (!res.ok) throw new Error(`${res.status}`); });
}