//============================================================
// MyStore Frontend - Complete App.js
// ============================================================

const GRAPHQL_URL = window.location.hostname === 'localhost'
  ? 'http://localhost:8085/graphql'
  : 'https://YOUR-BACKEND-URL.railway.app/graphql';  // ← Badala jar deploy kela asel tar

// ============================================================
// STATE
// ============================================================
let authToken = sessionStorage.getItem('authToken');
let currentUser = JSON.parse(sessionStorage.getItem('currentUser') || 'null');
let isRegisterMode = false;

let cart = JSON.parse(localStorage.getItem('cart') || '[]');
let favorites = JSON.parse(localStorage.getItem('favorites') || '[]');

let forgotToken = null;
let toastTimer = null;
let loadReqId = 0;

const productsById = new Map();

// Reviews state
let currentReviewProductId = null;
let currentProductDetailId = null;
let selectedRating = 0;
let detailQuantity = 1;

// ============================================================
// GRAPHQL HELPER
// ============================================================
async function gql(query, variables = {}) {
  const headers = { 'Content-Type': 'application/json' };
  if (authToken) headers['Authorization'] = 'Bearer ' + authToken;

  let res;
  try {
    res = await fetch(GRAPHQL_URL, {
      method: 'POST',
      headers,
      body: JSON.stringify({ query, variables })
    });
  } catch (networkErr) {
    throw new Error('Network error. Is the server running?');
  }

  if (!res.ok) {
    if (res.status === 401 || res.status === 403) {
      handleUnauthorized();
    }
    throw new Error(`HTTP ${res.status}`);
  }

  const text = await res.text();
  let data;
  try { data = JSON.parse(text); }
  catch { throw new Error('Invalid server response'); }

  if (data.errors && data.errors.length > 0) {
    const msg = data.errors[0].message || 'Request failed';
    if (/unauth|jwt|expired|forbidden|access denied/i.test(msg)) {
      handleUnauthorized();
    }
    throw new Error(msg);
  }
  return data.data;
}

function handleUnauthorized() {
  if (authToken) {
    authToken = null;
    currentUser = null;
    sessionStorage.removeItem('authToken');
    sessionStorage.removeItem('currentUser');
    updateAuthUI();
    showToast('Session expired. Please login again.', true);
  }
}

// ============================================================
// HELPERS
// ============================================================
function show(id) { const el = document.getElementById(id); if (el) el.classList.remove('hidden'); }
function hide(id) { const el = document.getElementById(id); if (el) el.classList.add('hidden'); }

function showToast(msg, isError = false) {
  const toast = document.getElementById('toast');
  if (!toast) return;
  clearTimeout(toastTimer);
  toast.textContent = msg;
  toast.className = 'toast' + (isError ? ' error' : '');
  toastTimer = setTimeout(() => toast.classList.add('hidden'), 3500);
}

function escapeHtml(str) {
  if (str === null || str === undefined) return '';
  const div = document.createElement('div');
  div.textContent = String(str);
  return div.innerHTML;
}

function safeUrl(u) {
  if (!u || typeof u !== 'string') return null;
  try {
    const url = new URL(u, location.origin);
    return ['http:', 'https:', 'data:'].includes(url.protocol) ? url.href : null;
  } catch { return null; }
}

let _placeholder = null;
function getPlaceholderImage() {
  if (_placeholder) return _placeholder;
  const svg = `<svg xmlns="http://www.w3.org/2000/svg" width="300" height="180">
    <rect width="100%" height="100%" fill="#e0e0e0"/>
    <text x="50%" y="50%" font-family="Arial" font-size="14"
          fill="#999" text-anchor="middle" dominant-baseline="middle">No Image</text>
  </svg>`;
  _placeholder = 'data:image/svg+xml;base64,' + btoa(svg);
  return _placeholder;
}

function buildImage(src, alt, className) {
  const img = document.createElement('img');
  img.src = safeUrl(src) || getPlaceholderImage();
  img.alt = alt || 'Product';
  if (className) img.className = className;
  img.onerror = () => { img.src = getPlaceholderImage(); };
  return img;
}

function getStarsDisplay(rating) {
  const fullStars = Math.floor(rating);
  const hasHalf = rating - fullStars >= 0.5;
  const emptyStars = 5 - fullStars - (hasHalf ? 1 : 0);
  return '★'.repeat(fullStars) + (hasHalf ? '⯨' : '') + '☆'.repeat(emptyStars);
}

// ============================================================
// AUTH UI
// ============================================================
function updateAuthUI() {
  const loginBtn = document.getElementById('loginBtn');
  const userMenu = document.getElementById('userMenu');
  const userName = document.getElementById('userName');
  const adminMenu = document.getElementById('adminMenu');
  const addProductBtn = document.getElementById('addProductBtn');

  if (currentUser) {
    if (loginBtn) loginBtn.classList.add('hidden');
    if (userMenu) userMenu.classList.remove('hidden');
    if (userName) userName.textContent = '👤 ' + (currentUser.name || '');

    if (currentUser.role === 'ADMIN') {
      if (adminMenu) adminMenu.classList.remove('hidden');
      if (addProductBtn) addProductBtn.disabled = false;
    } else {
      if (adminMenu) adminMenu.classList.add('hidden');
      if (addProductBtn) addProductBtn.disabled = true;
    }
  } else {
    if (loginBtn) loginBtn.classList.remove('hidden');
    if (userMenu) userMenu.classList.add('hidden');
    if (adminMenu) adminMenu.classList.add('hidden');
    if (addProductBtn) addProductBtn.disabled = true;
  }
}

function isAdmin() { return currentUser && currentUser.role === 'ADMIN'; }

// ============================================================
// BADGES
// ============================================================
function updateCartBadge() {
  const badge = document.getElementById('cartCount');
  if (badge) badge.textContent = cart.reduce((sum, i) => sum + (i.quantity || 0), 0);
}

function updateFavBadge() {
  const badge = document.getElementById('favCount');
  if (badge) badge.textContent = favorites.length;
}

function saveCart() { localStorage.setItem('cart', JSON.stringify(cart)); updateCartBadge(); }
function saveFavorites() { localStorage.setItem('favorites', JSON.stringify(favorites)); updateFavBadge(); }

// ============================================================
// LOAD PRODUCTS
// ============================================================
async function loadProducts(search = '') {
  const reqId = ++loadReqId;
  show('loading');
  hide('empty');

  try {
    const data = await gql(`
      query($search: String) {
        getAllProducts(page: 0, size: 50, search: $search) {
          content {
            id
            name
            description
            price
            stockQuantity
            category
            imageUrl
            averageRating
            totalReviews
          }
        }
      }
    `, { search: search || null });

    if (reqId !== loadReqId) return;
    if (!data || !data.getAllProducts) throw new Error('Invalid response');

    renderProducts(data.getAllProducts.content);
  } catch (err) {
    if (reqId !== loadReqId) return;
    console.error('Load products error:', err);
    showToast('Error: ' + err.message, true);
  } finally {
    if (reqId === loadReqId) hide('loading');
  }
}

// ============================================================
// RENDER PRODUCTS
// ============================================================
function renderProducts(products) {
  const container = document.getElementById('products');
  container.innerHTML = '';
  productsById.clear();

  if (!products || products.length === 0) { show('empty'); return; }
  hide('empty');

  products.forEach(p => {
    const idStr = String(p.id);
    productsById.set(idStr, p);

    const isFav = favorites.some(f => String(f.id) === idStr);
    const outOfStock = p.stockQuantity === 0;

    // Rating data
    const avgRating = p.averageRating || 0;
    const totalReviews = p.totalReviews || 0;
    const starsDisplay = getStarsDisplay(avgRating);

    const ratingHtml = totalReviews > 0
      ? `<div class="product-rating">
           <span class="stars-display">${starsDisplay}</span>
           <span class="rating-value">${avgRating.toFixed(1)}</span>
           <span class="review-count">(${totalReviews})</span>
         </div>`
      : `<div class="product-rating no-reviews">No reviews yet</div>`;

    const card = document.createElement('div');
    card.className = 'product';
    card.dataset.id = idStr;

    // Favorite heart
    const favBtn = document.createElement('button');
    favBtn.className = 'fav-heart' + (isFav ? ' active' : '');
    favBtn.textContent = isFav ? '❤️' : '🤍';
    favBtn.setAttribute('aria-label', 'Toggle favorite');
    favBtn.dataset.id = idStr;
    favBtn.addEventListener('click', (e) => {
      e.stopPropagation();
      toggleFavorite(idStr);
    });
    card.appendChild(favBtn);

    // Image — click to open detail
    const imgEl = buildImage(p.imageUrl, p.name);
    imgEl.style.cursor = 'pointer';
    imgEl.addEventListener('click', () => openProductDetail(idStr));
    card.appendChild(imgEl);

    // Info — click to open detail
    const info = document.createElement('div');
    info.className = 'product-info';
    info.style.cursor = 'pointer';
    info.innerHTML = `
      <h3>${escapeHtml(p.name)}</h3>
      <div class="category">${escapeHtml(p.category || 'General')}</div>
      ${ratingHtml}
      <div class="price">₹${escapeHtml(p.price)}</div>
      <div class="stock ${outOfStock ? 'low' : ''}">Stock: ${escapeHtml(p.stockQuantity)}</div>
    `;
    info.addEventListener('click', () => openProductDetail(idStr));

    // Buttons
    const btnWrap = document.createElement('div');
    btnWrap.className = 'card-buttons';

    const orderBtn = document.createElement('button');
    orderBtn.className = 'btn-order';
    orderBtn.textContent = outOfStock ? 'Out' : 'Order Now';
    orderBtn.disabled = outOfStock;
    orderBtn.addEventListener('click', (e) => {
      e.stopPropagation();
      orderProduct(idStr);
    });
    btnWrap.appendChild(orderBtn);

    const cartBtn = document.createElement('button');
    cartBtn.className = 'btn-cart';
    cartBtn.textContent = '🛒 Cart';
    cartBtn.disabled = outOfStock;
    cartBtn.addEventListener('click', (e) => {
      e.stopPropagation();
      addToCart(idStr);
    });
    btnWrap.appendChild(cartBtn);

    const reviewsBtn = document.createElement('button');
    reviewsBtn.className = 'btn-reviews';
    reviewsBtn.textContent = '⭐ Reviews';
    reviewsBtn.addEventListener('click', (e) => {
      e.stopPropagation();
      openReviewsModal(p.id, p.name);
    });
    btnWrap.appendChild(reviewsBtn);

    info.appendChild(btnWrap);
    card.appendChild(info);
    container.appendChild(card);
  });
}

// ============================================================
// PRODUCT DETAIL MODAL (Flipkart-style)
// ============================================================
async function openProductDetail(productId) {
  const idStr = String(productId);
  let product = productsById.get(idStr);

  if (!product) {
    try {
      const data = await gql(`
        query($id: ID!) {
          getProduct(id: $id) {
            id name description price stockQuantity category imageUrl
            averageRating totalReviews
          }
        }
      `, { id: productId });
      product = data.getProduct;
      productsById.set(idStr, product);
    } catch (err) {
      showToast('Failed to load product: ' + err.message, true);
      return;
    }
  }

  currentProductDetailId = idStr;
  detailQuantity = 1;

  document.getElementById('productDetailTitle').textContent = product.name || 'Product Details';

  const body = document.getElementById('productDetailBody');
  const isFav = favorites.some(f => String(f.id) === idStr);
  const outOfStock = product.stockQuantity === 0;
  const avgRating = product.averageRating || 0;
  const totalReviews = product.totalReviews || 0;
  const starsDisplay = getStarsDisplay(avgRating);

  const ratingRowHtml = totalReviews > 0
    ? `<div class="rating-row">
         <span class="big-rating">${avgRating.toFixed(1)}</span>
         <span class="rating-stars">${starsDisplay}</span>
         <a class="reviews-count-link" onclick="viewReviewsFromDetail(${product.id}, '${escapeHtml(product.name).replace(/'/g, "\\'")}')">
           ${totalReviews} review${totalReviews > 1 ? 's' : ''} →
         </a>
       </div>`
    : `<div class="rating-row">
         <span style="color:#888;font-style:italic;">No reviews yet</span>
         <a class="reviews-count-link" onclick="viewReviewsFromDetail(${product.id}, '${escapeHtml(product.name).replace(/'/g, "\\'")}')">
           Be the first to review →
         </a>
       </div>`;

  body.innerHTML = `
    <div class="product-detail-container">
      <div class="product-detail-image">
        <img id="detailImage" src="${safeUrl(product.imageUrl) || getPlaceholderImage()}"
             alt="${escapeHtml(product.name)}"
             onerror="this.src='${getPlaceholderImage()}'" />
      </div>

      <div class="product-detail-info">
        <h2>${escapeHtml(product.name)}</h2>
        <span class="category-badge">${escapeHtml(product.category || 'General')}</span>

        ${ratingRowHtml}

        <div class="product-detail-price">₹${escapeHtml(product.price)}</div>

        <div class="product-detail-stock ${outOfStock ? 'out-of-stock' : 'in-stock'}">
          ${outOfStock ? '❌ Out of Stock' : `✅ In Stock (${product.stockQuantity} available)`}
        </div>

        ${product.description ? `
          <div class="product-detail-description">
            <h4>📝 Description</h4>
            <p>${escapeHtml(product.description)}</p>
          </div>
        ` : ''}

        ${!outOfStock ? `
          <div class="quantity-selector">
            <label>Quantity:</label>
            <button class="qty-btn" onclick="decreaseDetailQty()">−</button>
            <span class="qty-value" id="detailQtyValue">1</span>
            <button class="qty-btn" onclick="increaseDetailQty()">+</button>
          </div>
        ` : ''}

        <div class="product-detail-actions">
          <button class="btn-detail-cart" id="detailCartBtn" onclick="addToCartFromDetail()" ${outOfStock ? 'disabled' : ''}>
            🛒 Add to Cart
          </button>
          <button class="btn-detail-order" onclick="orderFromDetail()" ${outOfStock ? 'disabled' : ''}>
            ⚡ Buy Now
          </button>
          <button class="btn-detail-fav ${isFav ? 'active' : ''}" onclick="toggleFavFromDetail()" title="Add to favorites">
            ${isFav ? '❤️' : '🤍'}
          </button>
        </div>
      </div>
    </div>
  `;

  show('productDetailModal');
}

function closeProductDetail() {
  hide('productDetailModal');
  currentProductDetailId = null;
  detailQuantity = 1;
}

function increaseDetailQty() {
  if (!currentProductDetailId) return;
  const product = productsById.get(currentProductDetailId);
  if (!product) return;

  if (detailQuantity < product.stockQuantity) {
    detailQuantity++;
    document.getElementById('detailQtyValue').textContent = detailQuantity;
  } else {
    showToast('Maximum stock reached', true);
  }
}

function decreaseDetailQty() {
  if (detailQuantity > 1) {
    detailQuantity--;
    document.getElementById('detailQtyValue').textContent = detailQuantity;
  }
}

function addToCartFromDetail() {
  if (!currentProductDetailId) return;
  const product = productsById.get(currentProductDetailId);
  if (!product) return;

  const existing = cart.find(i => String(i.id) === currentProductDetailId);
  if (existing) {
    existing.quantity += detailQuantity;
  } else {
    cart.push({
      id: currentProductDetailId,
      name: product.name,
      price: product.price,
      imageUrl: product.imageUrl,
      quantity: detailQuantity
    });
  }
  saveCart();
  showToast(`🛒 Added ${detailQuantity} item(s) to cart!`);
  closeProductDetail();
}

async function orderFromDetail() {
  if (!currentProductDetailId) return;
  if (!currentUser) {
    showToast('Please login to order!', true);
    closeProductDetail();
    openLogin();
    return;
  }

  const product = productsById.get(currentProductDetailId);
  if (!product) return;

  try {
    const data = await gql(`
      mutation($input: OrderInput) {
        addOrder(orderInput: $input) { id orderNumber status totalAmount }
      }
    `, {
      input: {
        orderItems: [{ productId: currentProductDetailId, quantity: detailQuantity }]
      }
    });

    showToast('🎉 Order placed! ' + data.addOrder.orderNumber);
    closeProductDetail();
    loadProducts(document.getElementById('searchBox').value.trim());
  } catch (err) {
    showToast('❌ ' + err.message, true);
  }
}

function toggleFavFromDetail() {
  if (!currentProductDetailId) return;
  toggleFavorite(currentProductDetailId);

  // Update the heart in the detail modal
  const product = productsById.get(currentProductDetailId);
  if (product) {
    const isFav = favorites.some(f => String(f.id) === currentProductDetailId);
    const favBtn = document.querySelector('.btn-detail-fav');
    if (favBtn) {
      favBtn.classList.toggle('active', isFav);
      favBtn.textContent = isFav ? '❤️' : '🤍';
    }
  }
}

function viewReviewsFromDetail(productId, productName) {
  closeProductDetail();
  openReviewsModal(productId, productName);
}

// ============================================================
// REVIEWS & RATINGS
// ============================================================
async function openReviewsModal(productId, productName) {
  currentReviewProductId = productId;
  selectedRating = 0;

  document.getElementById('reviewsModalTitle').textContent = `⭐ Reviews: ${productName}`;
  document.getElementById('reviewsModal').classList.remove('hidden');

  resetReviewForm();

  await loadProductReviews(productId);

  if (currentUser) {
    document.getElementById('addReviewSection').classList.remove('hidden');
    document.getElementById('loginRequiredMsg').classList.add('hidden');
  } else {
    document.getElementById('addReviewSection').classList.add('hidden');
    document.getElementById('loginRequiredMsg').classList.remove('hidden');
  }
}

function closeReviewsModal() {
  document.getElementById('reviewsModal').classList.add('hidden');
  currentReviewProductId = null;
  selectedRating = 0;
}

async function loadProductReviews(productId) {
  const listContainer = document.getElementById('reviewsList');
  const summaryContainer = document.getElementById('reviewProductSummary');

  listContainer.innerHTML = '<div class="empty-msg">Loading reviews...</div>';

  try {
    const data = await gql(`
      query($productId: ID!) {
        getProductReviews(productId: $productId) {
          id
          rating
          comment
          createdAt
          user { id name email }
        }
        getProductAverageRating(productId: $productId)
        getProductReviewCount(productId: $productId)
      }
    `, { productId: productId });

    const reviews = data.getProductReviews || [];
    const avgRating = data.getProductAverageRating || 0;
    const totalReviews = data.getProductReviewCount || 0;

    renderReviewSummary(avgRating, totalReviews);
    renderReviewsList(reviews);

  } catch (err) {
    listContainer.innerHTML = `<div class="empty-msg">❌ ${escapeHtml(err.message)}</div>`;
    summaryContainer.innerHTML = '';
  }
}

function renderReviewSummary(avgRating, totalReviews) {
  const container = document.getElementById('reviewProductSummary');

  if (totalReviews === 0) {
    container.innerHTML = `
      <h3>Be the first to review!</h3>
      <div class="total-reviews">No reviews yet</div>
    `;
    return;
  }

  const starsDisplay = getStarsDisplay(avgRating);

  container.innerHTML = `
    <div class="big-rating">${avgRating.toFixed(1)}</div>
    <div class="big-stars">${starsDisplay}</div>
    <div class="total-reviews">Based on ${totalReviews} review${totalReviews > 1 ? 's' : ''}</div>
  `;
}

function renderReviewsList(reviews) {
  const container = document.getElementById('reviewsList');

  if (!reviews || reviews.length === 0) {
    container.innerHTML = '<div class="no-reviews-msg">💭 No reviews yet. Be the first!</div>';
    return;
  }

  container.innerHTML = '';

  reviews.forEach(review => {
    const item = document.createElement('div');
    item.className = 'review-item';

    const stars = getStarsDisplay(review.rating);
    const date = new Date(review.createdAt).toLocaleDateString('en-IN', {
      day: 'numeric',
      month: 'short',
      year: 'numeric'
    });

    const initials = review.user?.name
      ? review.user.name.split(' ').map(n => n[0]).join('').toUpperCase().slice(0, 2)
      : '?';

    const canDelete = currentUser && (
      currentUser.id === review.user?.id ||
      currentUser.role === 'ADMIN'
    );

    const deleteBtn = canDelete
      ? `<button class="review-delete-btn" onclick="deleteReview(${review.id})">🗑️ Delete</button>`
      : '';

    item.innerHTML = `
      <div class="review-header">
        <div class="review-user-info">
          <div class="review-avatar">${escapeHtml(initials)}</div>
          <div class="review-user-details">
            <h5>${escapeHtml(review.user?.name || 'Anonymous')}</h5>
            <div class="review-date">${escapeHtml(date)}</div>
          </div>
        </div>
        <div style="display: flex; align-items: center; gap: 10px;">
          <div class="review-rating">${stars}</div>
          ${deleteBtn}
        </div>
      </div>
      ${review.comment ? `<div class="review-comment">${escapeHtml(review.comment)}</div>` : ''}
    `;

    container.appendChild(item);
  });
}

function initStarRating() {
  const stars = document.querySelectorAll('#starInput .star');

  stars.forEach(star => {
    star.addEventListener('click', () => {
      selectedRating = parseInt(star.dataset.rating);
      updateStarsUI(selectedRating);
    });

    star.addEventListener('mouseenter', () => {
      const rating = parseInt(star.dataset.rating);
      updateStarsUI(rating);
    });
  });

  const starInput = document.getElementById('starInput');
  if (starInput) {
    starInput.addEventListener('mouseleave', () => {
      updateStarsUI(selectedRating);
    });
  }
}

function updateStarsUI(rating) {
  const stars = document.querySelectorAll('#starInput .star');
  stars.forEach((star, index) => {
    if (index < rating) {
      star.classList.add('selected');
      star.textContent = '★';
    } else {
      star.classList.remove('selected');
      star.textContent = '☆';
    }
  });
}

function resetReviewForm() {
  selectedRating = 0;
  updateStarsUI(0);
  const commentEl = document.getElementById('reviewComment');
  if (commentEl) commentEl.value = '';
}

function cancelReview() {
  resetReviewForm();
}

async function submitReview() {
  if (!currentReviewProductId) return;

  if (selectedRating === 0) {
    showToast('Please select a rating', true);
    return;
  }

  const comment = document.getElementById('reviewComment').value.trim();

  if (comment.length > 1000) {
    showToast('Comment too long (max 1000 chars)', true);
    return;
  }

  const submitBtn = document.querySelector('.btn-submit-review');
  if (submitBtn) {
    submitBtn.disabled = true;
    submitBtn.textContent = '⏳ Submitting...';
  }

  try {
    await gql(`
      mutation($input: ReviewInput!) {
        addReview(reviewInput: $input) {
          id
          rating
          comment
          createdAt
        }
      }
    `, {
      input: {
        productId: currentReviewProductId,
        rating: selectedRating,
        comment: comment || null
      }
    });

    showToast('✅ Review added successfully!');
    resetReviewForm();

    await loadProductReviews(currentReviewProductId);
    loadProducts(document.getElementById('searchBox').value.trim());

  } catch (err) {
    showToast('❌ ' + err.message, true);
  } finally {
    if (submitBtn) {
      submitBtn.disabled = false;
      submitBtn.textContent = 'Submit Review';
    }
  }
}

async function deleteReview(reviewId) {
  if (!confirm('Delete this review permanently?')) return;

  try {
    await gql(`
      mutation($id: ID!) {
        deleteReview(id: $id)
      }
    `, { id: reviewId });

    showToast('🗑️ Review deleted!');

    if (currentReviewProductId) {
      await loadProductReviews(currentReviewProductId);
      loadProducts(document.getElementById('searchBox').value.trim());
    }

  } catch (err) {
    showToast('❌ ' + err.message, true);
  }
}

// ============================================================
// ADD PRODUCT
// ============================================================
async function addProduct() {
  if (!currentUser) { showToast('Please login to add products!', true); openLogin(); return; }

  const price = parseFloat(document.getElementById('price').value);
  const stock = parseInt(document.getElementById('stock').value, 10);

  const input = {
    name: document.getElementById('name').value.trim(),
    imageUrl: document.getElementById('imageUrl').value.trim() || null,
    description: document.getElementById('description').value.trim() || null,
    price: isNaN(price) ? null : price,
    stockQuantity: isNaN(stock) ? null : stock,
    category: document.getElementById('category').value.trim() || null
  };

  if (!input.name || input.price === null) {
    showToast('Name and Price are required!', true); return;
  }
  if (input.stockQuantity === null || input.stockQuantity < 0) {
    showToast('Valid stock quantity required!', true); return;
  }
  if (input.price <= 0) {
    showToast('Price must be greater than 0!', true); return;
  }

  try {
    await gql(`
      mutation($input: ProductInput) { addProduct(productInput: $input) { id name } }
    `, { input });

    showToast('✅ Product added successfully!');
    document.getElementById('addForm').reset();
    toggleForm();
    loadProducts(document.getElementById('searchBox').value.trim());
  } catch (err) {
    showToast('Error: ' + err.message, true);
  }
}

// ============================================================
// ORDER PRODUCT (Direct)
// ============================================================
async function orderProduct(productId) {
  if (!currentUser) { showToast('Please login to order!', true); openLogin(); return; }

  try {
    const data = await gql(`
      mutation($input: OrderInput) {
        addOrder(orderInput: $input) { id orderNumber status totalAmount }
      }
    `, { input: { orderItems: [{ productId: productId, quantity: 1 }] } });

    showToast('🎉 Order placed! ' + data.addOrder.orderNumber);
    loadProducts(document.getElementById('searchBox').value.trim());
  } catch (err) {
    showToast('❌ ' + err.message, true);
  }
}

// ============================================================
// TOGGLE ADD FORM
// ============================================================
function toggleForm() {
  if (!currentUser) { showToast('Please login first!', true); openLogin(); return; }
  document.getElementById('addForm').classList.toggle('hidden');
}

// ============================================================
// CART
// ============================================================
function addToCart(productId) {
  const idStr = String(productId);
  const product = productsById.get(idStr);
  if (!product) { showToast('Product not found', true); return; }

  const existing = cart.find(i => String(i.id) === idStr);
  if (existing) {
    existing.quantity++;
  } else {
    cart.push({
      id: idStr,
      name: product.name,
      price: product.price,
      imageUrl: product.imageUrl,
      quantity: 1
    });
  }
  saveCart();
  showToast('🛒 Added to cart!');
}

function openCart() { renderCart(); show('cartModal'); }
function closeCart() { hide('cartModal'); }

function renderCart() {
  const container = document.getElementById('cartItems');
  const totalEl = document.getElementById('cartTotal');

  if (cart.length === 0) {
    container.innerHTML = '<div class="empty-msg">🛒 Your cart is empty</div>';
    totalEl.textContent = '0';
    return;
  }

  container.innerHTML = '';
  let total = 0;

  cart.forEach(item => {
    const subtotal = (item.price || 0) * item.quantity;
    total += subtotal;

    const div = document.createElement('div');
    div.className = 'cart-item';

    div.appendChild(buildImage(item.imageUrl, item.name));

    const info = document.createElement('div');
    info.className = 'item-info';
    info.innerHTML = `
      <h4>${escapeHtml(item.name)}</h4>
      <div class="price">₹${escapeHtml(item.price)} × ${escapeHtml(item.quantity)} = ₹${subtotal.toFixed(2)}</div>
    `;
    div.appendChild(info);

    const qty = document.createElement('div');
    qty.className = 'qty-controls';
    qty.innerHTML = `
      <button data-act="dec">−</button>
      <span>${escapeHtml(item.quantity)}</span>
      <button data-act="inc">+</button>
    `;
    qty.querySelector('[data-act="dec"]').addEventListener('click', () => changeQty(item.id, -1));
    qty.querySelector('[data-act="inc"]').addEventListener('click', () => changeQty(item.id, 1));
    div.appendChild(qty);

    const rm = document.createElement('button');
    rm.className = 'remove-btn';
    rm.textContent = '🗑️';
    rm.setAttribute('aria-label', 'Remove');
    rm.addEventListener('click', () => removeFromCart(item.id));
    div.appendChild(rm);

    container.appendChild(div);
  });

  totalEl.textContent = total.toFixed(2);
}

function changeQty(productId, delta) {
  const idStr = String(productId);
  const item = cart.find(i => String(i.id) === idStr);
  if (!item) return;
  item.quantity += delta;
  if (item.quantity <= 0) cart = cart.filter(i => String(i.id) !== idStr);
  saveCart();
  renderCart();
}

function removeFromCart(productId) {
  const idStr = String(productId);
  cart = cart.filter(i => String(i.id) !== idStr);
  saveCart();
  renderCart();
  showToast('🗑️ Removed from cart');
}

async function checkout() {
  if (cart.length === 0) { showToast('Cart is empty!', true); return; }
  if (!currentUser) { showToast('Please login to checkout!', true); openLogin(); return; }

  try {
    const orderItems = cart.map(item => ({
      productId: item.id,
      quantity: item.quantity
    }));

    const data = await gql(`
      mutation($input: OrderInput) {
        addOrder(orderInput: $input) { id orderNumber totalAmount }
      }
    `, { input: { orderItems } });

    showToast('🎉 Order placed! ' + data.addOrder.orderNumber);
    cart = [];
    saveCart();
    closeCart();
    loadProducts(document.getElementById('searchBox').value.trim());
  } catch (err) {
    showToast('❌ ' + err.message, true);
  }
}

// ============================================================
// FAVORITES
// ============================================================
function toggleFavorite(productId) {
  const idStr = String(productId);
  const idx = favorites.findIndex(f => String(f.id) === idStr);

  if (idx >= 0) {
    favorites.splice(idx, 1);
    showToast('💔 Removed from favorites');
  } else {
    const product = productsById.get(idStr);
    if (!product) { showToast('Product not found', true); return; }
    favorites.push({
      id: idStr,
      name: product.name,
      price: product.price,
      imageUrl: product.imageUrl,
      category: product.category
    });
    showToast('❤️ Added to favorites!');
  }
  saveFavorites();
  updateFavBadge();

  const heart = document.querySelector(`.fav-heart[data-id="${idStr}"]`);
  if (heart) {
    const active = favorites.some(f => String(f.id) === idStr);
    heart.classList.toggle('active', active);
    heart.textContent = active ? '❤️' : '🤍';
  }
}

function openFavorites() { renderFavorites(); show('favModal'); }
function closeFavorites() { hide('favModal'); }

function renderFavorites() {
  const container = document.getElementById('favItems');
  container.innerHTML = '';

  if (favorites.length === 0) {
    container.innerHTML = '<div class="empty-msg">❤️ No favorites yet</div>';
    return;
  }

  favorites.forEach(item => {
    const div = document.createElement('div');
    div.className = 'fav-item';

    div.appendChild(buildImage(item.imageUrl, item.name));

    const info = document.createElement('div');
    info.className = 'item-info';
    info.innerHTML = `
      <h4>${escapeHtml(item.name)}</h4>
      <div class="category" style="font-size:12px;color:#888;">${escapeHtml(item.category || '')}</div>
      <div class="price">₹${escapeHtml(item.price)}</div>
    `;
    div.appendChild(info);

    const cartBtn = document.createElement('button');
    cartBtn.className = 'btn-cart';
    cartBtn.textContent = '🛒';
    cartBtn.style.cssText = 'padding:8px 14px;border-radius:6px;background:#27ae60;color:#fff;border:none;cursor:pointer;';
    cartBtn.addEventListener('click', () => addToCartFromFav(item.id));
    div.appendChild(cartBtn);

    const rm = document.createElement('button');
    rm.className = 'remove-btn';
    rm.textContent = '🗑️';
    rm.setAttribute('aria-label', 'Remove');
    rm.addEventListener('click', () => toggleFavorite(item.id));
    div.appendChild(rm);

    container.appendChild(div);
  });
}

function addToCartFromFav(productId) {
  const idStr = String(productId);
  const fav = favorites.find(f => String(f.id) === idStr);
  if (!fav) return;
  const existing = cart.find(i => String(i.id) === idStr);
  if (existing) existing.quantity++;
  else cart.push({ ...fav, quantity: 1 });
  saveCart();
  showToast('🛒 Added to cart!');
}

// ============================================================
// AUTH
// ============================================================
function openLogin() {
  isRegisterMode = false;
  updateAuthModalUI();
  show('loginModal');
}

function closeLogin() {
  hide('loginModal');
  ['authEmail', 'authPassword', 'regName', 'regAddress'].forEach(id => {
    const el = document.getElementById(id);
    if (el) el.value = '';
  });
  isRegisterMode = false;
}

function switchAuthMode() { isRegisterMode = !isRegisterMode; updateAuthModalUI(); }

function updateAuthModalUI() {
  document.getElementById('authTitle').textContent = isRegisterMode ? 'Register' : 'Login';
  document.getElementById('authSubmitBtn').textContent = isRegisterMode ? 'Register' : 'Login';
  document.getElementById('switchText').textContent = isRegisterMode
    ? 'Already have an account? Login'
    : "Don't have an account? Register";

  const rf = document.getElementById('registerFields');
  const fp = document.getElementById('forgotPasswordDiv');
  if (isRegisterMode) { rf?.classList.remove('hidden'); fp?.classList.add('hidden'); }
  else { rf?.classList.add('hidden'); fp?.classList.remove('hidden'); }
}

async function submitAuth() {
  const email = document.getElementById('authEmail').value.trim();
  const password = document.getElementById('authPassword').value;

  if (!email || !password) { showToast('Email and Password are required!', true); return; }

  try {
    let data;
    if (isRegisterMode) {
      const name = document.getElementById('regName').value.trim();
      const address = document.getElementById('regAddress').value.trim();
      if (!name) { showToast('Name is required!', true); return; }
      if (password.length < 8) { showToast('Password must be at least 8 characters', true); return; }

      data = await gql(`
        mutation($userInput: UserRegisterInput!) {
          register(userRegisterInput: $userInput) { token userId name email role }
        }
      `, { userInput: { name, email, password, address } });
      data = data.register;
    } else {
      data = await gql(`
        mutation($email: String!, $password: String!) {
          login(email: $email, password: $password) { token userId name email role }
        }
      `, { email, password });
      data = data.login;
    }

    authToken = data.token;
    currentUser = { id: data.userId, name: data.name, email: data.email, role: data.role };

    sessionStorage.setItem('authToken', authToken);
    sessionStorage.setItem('currentUser', JSON.stringify(currentUser));

    updateAuthUI();
    closeLogin();
    showToast('🎉 Welcome, ' + data.name + '!');
    loadProducts(document.getElementById('searchBox').value.trim());
  } catch (err) {
    showToast('❌ ' + err.message, true);
  }
}

function logout() {
  authToken = null;
  currentUser = null;
  sessionStorage.removeItem('authToken');
  sessionStorage.removeItem('currentUser');
  cart = [];
  favorites = [];
  saveCart();
  saveFavorites();

  updateAuthUI();
  showToast('👋 Logged out');
  loadProducts(document.getElementById('searchBox').value.trim());
}

// ============================================================
// SEARCH (Debounced)
// ============================================================
let searchTimer;
const searchBox = document.getElementById('searchBox');
if (searchBox) {
  searchBox.addEventListener('input', (e) => {
    clearTimeout(searchTimer);
    searchTimer = setTimeout(() => loadProducts(e.target.value.trim()), 400);
  });
}

// ============================================================
// MY ORDERS
// ============================================================
async function openMyOrders() {
  if (!currentUser) { showToast('Please login to view orders!', true); openLogin(); return; }
  show('ordersModal');
  document.getElementById('ordersList').innerHTML =
    '<div class="empty-msg">Loading orders...</div>';

  try {
    const data = await gql(`
      query {
        myOrders {
          id orderNumber status totalAmount orderDate
          orderItems { id quantity price product { id name imageUrl } }
        }
      }
    `);
    renderOrders(data.myOrders);
  } catch (err) {
    document.getElementById('ordersList').innerHTML =
      `<div class="empty-msg">❌ ${escapeHtml(err.message)}</div>`;
  }
}

function closeMyOrders() { hide('ordersModal'); }

function renderOrders(orders) {
  const container = document.getElementById('ordersList');
  if (!orders || orders.length === 0) {
    container.innerHTML = `<div class="empty-msg">📦 No orders yet.</div>`;
    return;
  }
  container.innerHTML = '';
  orders.forEach(o => container.appendChild(createOrderCard(o)));
}

function createOrderCard(order) {
  const card = document.createElement('div');
  card.className = 'order-card';

  const date = new Date(order.orderDate).toLocaleString('en-IN', {
    day: 'numeric', month: 'short', year: 'numeric', hour: '2-digit', minute: '2-digit'
  });

  const statusClass = `status-${order.status}`;
  const steps = ['PENDING', 'PROCESSING', 'SHIPPED', 'DELIVERED'];
  const currentIdx = steps.indexOf(order.status);
  const isCancelled = order.status === 'CANCELLED';

  let timelineHTML = '';
  if (isCancelled) {
    timelineHTML = `<div style="text-align:center;padding:15px;color:#e74c3c;font-weight:bold;">❌ Order Cancelled</div>`;
  } else {
    timelineHTML = steps.map((step, i) => {
      let cls = 'timeline-step';
      if (i < currentIdx) cls += ' completed';
      else if (i === currentIdx) cls += ' active';
      const icon = i < currentIdx ? '✓' : (i === currentIdx ? '●' : i + 1);
      return `<div class="${cls}">
        <div class="timeline-line"></div>
        <div class="timeline-circle">${icon}</div>
        <div class="timeline-label">${step.charAt(0) + step.slice(1).toLowerCase()}</div>
      </div>`;
    }).join('');
  }

  card.innerHTML = `
    <div class="order-header">
      <div>
        <div class="order-number">#${escapeHtml(order.orderNumber)}</div>
        <span class="order-date">${escapeHtml(date)}</span>
      </div>
      <div style="text-align:right;">
        <div class="order-total">₹${escapeHtml(order.totalAmount)}</div>
        <span class="order-status ${statusClass}">${escapeHtml(order.status)}</span>
      </div>
    </div>
    <div class="timeline">${timelineHTML}</div>
    <div class="order-items-section">
      <button class="order-items-toggle">${order.orderItems.length} Item(s) ▼</button>
      <div class="order-items-list hidden"></div>
    </div>
  `;

  const itemsList = card.querySelector('.order-items-list');
  order.orderItems.forEach(item => {
    const row = document.createElement('div');
    row.className = 'order-item-row';
    row.appendChild(buildImage(item.product?.imageUrl, item.product?.name || 'Product'));
    const info = document.createElement('div');
    info.className = 'order-item-info';
    info.innerHTML = `
      <h5>${escapeHtml(item.product?.name || 'Unknown Product')}</h5>
      <div class="item-meta">Qty: ${escapeHtml(item.quantity)} × ₹${escapeHtml(item.price)}</div>
    `;
    row.appendChild(info);
    const price = document.createElement('div');
    price.className = 'order-item-price';
    price.textContent = `₹${(item.price * item.quantity).toFixed(2)}`;
    row.appendChild(price);
    itemsList.appendChild(row);
  });

  const canCancel = order.status === 'PENDING' || order.status === 'PROCESSING';
  if (canCancel) {
    const cancelBtn = document.createElement('button');
    cancelBtn.className = 'cancel-order-btn';
    cancelBtn.textContent = 'Cancel Order';
    cancelBtn.addEventListener('click', () => cancelOrder(order.id));
    itemsList.appendChild(cancelBtn);
  }

  const toggleBtn = card.querySelector('.order-items-toggle');
  toggleBtn.addEventListener('click', () => {
    const isHidden = itemsList.classList.toggle('hidden');
    toggleBtn.textContent = toggleBtn.textContent.replace(isHidden ? '▲' : '▼', isHidden ? '▼' : '▲');
  });

  return card;
}

async function cancelOrder(orderId) {
  if (!confirm('Are you sure you want to cancel this order?')) return;
  try {
    await gql(`mutation($id: ID!) { cancelOrder(id: $id) { id status } }`, { id: orderId });
    showToast('✅ Order cancelled');
    openMyOrders();
  } catch (err) {
    showToast('❌ ' + err.message, true);
  }
}

// ============================================================
// ADMIN PANEL
// ============================================================
let currentAdminTab = 'products';

function openAdminPanel() {
  if (!isAdmin()) { showToast('❌ Admin access only!', true); return; }
  show('adminModal');
  switchAdminTab('products', null);
}

function closeAdminPanel() { hide('adminModal'); }

function switchAdminTab(tab, btn) {
  currentAdminTab = tab;
  document.querySelectorAll('.admin-tab').forEach(b => b.classList.remove('active'));
  if (btn) btn.classList.add('active');

  if (tab === 'products') loadAdminProducts();
  else if (tab === 'orders') loadAdminOrders();
  else if (tab === 'users') loadAdminUsers();
}

// ---------- ADMIN — PRODUCTS ----------
async function loadAdminProducts() {
  const container = document.getElementById('adminContent');
  container.innerHTML = '<div class="empty-msg">Loading products...</div>';

  try {
    const data = await gql(`
      query { getAllProducts(page: 0, size: 100) {
        content { id name price stockQuantity category imageUrl }
      } }
    `);
    const products = data.getAllProducts.content;

    if (products.length === 0) {
      container.innerHTML = '<div class="empty-msg">No products</div>';
      return;
    }

    container.innerHTML = `
      <div style="margin-bottom:15px;">
        <button class="btn-sm" style="background:#27ae60;color:#fff;padding:10px 20px;" id="addProdBtn">+ Add New Product</button>
      </div>
      <table class="admin-table">
        <thead><tr><th>Image</th><th>Name</th><th>Category</th><th>Price</th><th>Stock</th><th>Actions</th></tr></thead>
        <tbody id="adminProductBody"></tbody>
      </table>
    `;
    document.getElementById('addProdBtn').addEventListener('click', showAddProductForm);

    const tbody = document.getElementById('adminProductBody');
    products.forEach(p => {
      const tr = document.createElement('tr');

      const tdImg = document.createElement('td');
      tdImg.appendChild(buildImage(p.imageUrl, p.name));
      tr.appendChild(tdImg);

      tr.insertAdjacentHTML('beforeend', `
        <td><strong>${escapeHtml(p.name)}</strong></td>
        <td>${escapeHtml(p.category || '-')}</td>
        <td>₹${escapeHtml(p.price)}</td>
        <td ${p.stockQuantity === 0 ? 'style="color:#e74c3c;font-weight:bold;"' : ''}>${escapeHtml(p.stockQuantity)}</td>
      `);

      const tdAct = document.createElement('td');
      const wrap = document.createElement('div');
      wrap.className = 'admin-actions';

      const editBtn = document.createElement('button');
      editBtn.className = 'btn-sm btn-edit';
      editBtn.textContent = 'Edit';
      editBtn.addEventListener('click', () => editProduct(p.id));
      wrap.appendChild(editBtn);

      const delBtn = document.createElement('button');
      delBtn.className = 'btn-sm btn-delete';
      delBtn.textContent = 'Delete';
      delBtn.addEventListener('click', () => deleteProductAdmin(p.id));
      wrap.appendChild(delBtn);

      tdAct.appendChild(wrap);
      tr.appendChild(tdAct);
      tbody.appendChild(tr);
    });
  } catch (err) {
    container.innerHTML = `<div class="empty-msg">❌ ${escapeHtml(err.message)}</div>`;
  }
}

async function showAddProductForm() {
  const name = prompt('Product Name:');
  if (!name) return;
  const price = parseFloat(prompt('Price (₹):'));
  if (isNaN(price) || price <= 0) { showToast('Invalid price', true); return; }
  const stock = parseInt(prompt('Stock Quantity:'), 10);
  if (isNaN(stock) || stock < 0) { showToast('Invalid stock', true); return; }
  const category = prompt('Category (optional):') || '';
  const imageUrl = prompt('Image URL (optional):') || '';
  const description = prompt('Description (optional):') || '';

  try {
    await gql(`mutation($input: ProductInput) { addProduct(productInput: $input) { id name } }`,
      { input: { name, price, stockQuantity: stock, category, imageUrl, description } });
    showToast('✅ Product added!');
    loadAdminProducts();
    loadProducts();
  } catch (err) {
    showToast('❌ ' + err.message, true);
  }
}

async function editProduct(productId) {
  const idStr = String(productId);
  let product = productsById.get(idStr);

  if (!product) {
    try {
      const data = await gql(`
        query($id: ID!) {
          getProduct(id: $id) { id name description price stockQuantity category imageUrl }
        }
      `, { id: productId });
      product = data.getProduct;
      productsById.set(idStr, product);
    } catch (err) {
      showToast('Failed to fetch product: ' + err.message, true);
      return;
    }
  }

  document.getElementById('editProductId').value = product.id;
  document.getElementById('editName').value = product.name || '';
  document.getElementById('editPrice').value = product.price || '';
  document.getElementById('editStock').value = product.stockQuantity ?? 0;
  document.getElementById('editImageUrl').value = product.imageUrl || '';
  document.getElementById('editCategory').value = product.category || '';
  document.getElementById('editDescription').value = product.description || '';

  document.getElementById('editProductModal').dataset.original = JSON.stringify({
    name: product.name || '',
    price: product.price,
    stockQuantity: product.stockQuantity,
    imageUrl: product.imageUrl || '',
    category: product.category || '',
    description: product.description || ''
  });

  show('editProductModal');
}

function closeEditProductModal() {
  hide('editProductModal');
  ['editProductId', 'editName', 'editPrice', 'editStock',
    'editImageUrl', 'editCategory', 'editDescription'].forEach(id => {
    const el = document.getElementById(id);
    if (el) el.value = '';
  });
  const modal = document.getElementById('editProductModal');
  if (modal) {
    delete modal.dataset.original;
    const saveBtn = modal.querySelector('.checkout-btn');
    if (saveBtn) {
      saveBtn.disabled = false;
      saveBtn.textContent = '💾 Save Changes';
    }
  }
}

async function saveProductEdit() {
  const modal = document.getElementById('editProductModal');
  const productId = document.getElementById('editProductId').value;

  const nameRaw = document.getElementById('editName').value.trim();
  const priceRaw = document.getElementById('editPrice').value;
  const stockRaw = document.getElementById('editStock').value;
  const imageUrlRaw = document.getElementById('editImageUrl').value.trim();
  const categoryRaw = document.getElementById('editCategory').value.trim();
  const descriptionRaw = document.getElementById('editDescription').value.trim();

  const price = parseFloat(priceRaw);
  const stockQuantity = parseInt(stockRaw, 10);

  if (!nameRaw) { showToast('Product name is required', true); return; }
  if (isNaN(price) || price <= 0) { showToast('Price must be greater than 0', true); return; }
  if (isNaN(stockQuantity) || stockQuantity < 0) { showToast('Stock cannot be negative', true); return; }

  const input = {
    name: nameRaw,
    description: descriptionRaw || null,
    price: price,
    stockQuantity: stockQuantity,
    category: categoryRaw || null,
    imageUrl: imageUrlRaw || null
  };

  const saveBtn = modal.querySelector('.checkout-btn');
  if (saveBtn) {
    saveBtn.disabled = true;
    saveBtn.textContent = '⏳ Saving...';
  }

  try {
    await gql(`
      mutation($id: ID, $input: ProductInput) {
        updateProduct(id: $id, productInput: $input) {
          id name price stockQuantity imageUrl category description
        }
      }
    `, { id: productId, input });

    showToast('✅ Product updated!');
    closeEditProductModal();
    loadAdminProducts();
    loadProducts();
  } catch (err) {
    showToast('❌ ' + err.message, true);
    if (saveBtn) {
      saveBtn.disabled = false;
      saveBtn.textContent = '💾 Save Changes';
    }
  }
}

async function deleteProductAdmin(productId) {
  if (!confirm('Delete this product permanently?')) return;
  try {
    await gql(`mutation($id: ID) { deleteProduct(id: $id) }`, { id: productId });
    showToast('🗑️ Product deleted!');
    loadAdminProducts();
    loadProducts();
  } catch (err) {
    showToast('❌ ' + err.message, true);
  }
}

// ---------- ADMIN — ORDERS ----------
async function loadAdminOrders() {
  const container = document.getElementById('adminContent');
  container.innerHTML = '<div class="empty-msg">Loading orders...</div>';

  try {
    const data = await gql(`
      query { allOrders {
        id orderNumber status totalAmount orderDate
        user { id name email }
      } }
    `);
    const orders = data.allOrders;

    if (orders.length === 0) {
      container.innerHTML = '<div class="empty-msg">No orders yet</div>';
      return;
    }

    const statuses = ['PENDING', 'PROCESSING', 'SHIPPED', 'DELIVERED', 'CANCELLED'];
    container.innerHTML = `
      <table class="admin-table">
        <thead><tr><th>Order #</th><th>Customer</th><th>Date</th><th>Total</th><th>Status</th><th>Action</th></tr></thead>
        <tbody id="adminOrderBody"></tbody>
      </table>
    `;
    const tbody = document.getElementById('adminOrderBody');

    orders.forEach(o => {
      const tr = document.createElement('tr');
      const date = new Date(o.orderDate).toLocaleDateString('en-IN');

      tr.innerHTML = `
        <td><strong>${escapeHtml(o.orderNumber)}</strong></td>
        <td>${escapeHtml(o.user?.name || 'Unknown')}<br>
            <small style="color:#888;">${escapeHtml(o.user?.email || '')}</small></td>
        <td>${escapeHtml(date)}</td>
        <td><strong>₹${escapeHtml(o.totalAmount)}</strong></td>
      `;

      const tdStatus = document.createElement('td');
      const sel = document.createElement('select');
      sel.className = 'status-select';
      statuses.forEach(s => {
        const opt = document.createElement('option');
        opt.value = s;
        opt.textContent = s;
        if (s === o.status) opt.selected = true;
        sel.appendChild(opt);
      });
      sel.addEventListener('change', () => updateOrderStatusAdmin(o.id, sel.value));
      tdStatus.appendChild(sel);
      tr.appendChild(tdStatus);

      const tdAct = document.createElement('td');
      const delBtn = document.createElement('button');
      delBtn.className = 'btn-sm btn-delete';
      delBtn.textContent = '🗑️';
      delBtn.addEventListener('click', () => deleteOrderAdmin(o.id));
      tdAct.appendChild(delBtn);
      tr.appendChild(tdAct);

      tbody.appendChild(tr);
    });
  } catch (err) {
    container.innerHTML = `<div class="empty-msg">❌ ${escapeHtml(err.message)}</div>`;
  }
}

async function updateOrderStatusAdmin(orderId, newStatus) {
  try {
    await gql(`mutation($id: ID!, $status: OrderStatus) {
      updateOrderStatus(id: $id, status: $status) { id status }
    }`, { id: orderId, status: newStatus });
    showToast('✅ Status updated to ' + newStatus);
  } catch (err) {
    showToast('❌ ' + err.message, true);
    loadAdminOrders();
  }
}

async function deleteOrderAdmin(orderId) {
  if (!confirm('Delete this order permanently?')) return;
  try {
    await gql(`mutation($id: ID!) { deleteOrder(id: $id) }`, { id: orderId });
    showToast('🗑️ Order deleted!');
    loadAdminOrders();
  } catch (err) {
    showToast('❌ ' + err.message, true);
  }
}

// ---------- ADMIN — USERS ----------
async function loadAdminUsers() {
  const container = document.getElementById('adminContent');
  container.innerHTML = '<div class="empty-msg">Loading users...</div>';

  try {
    const data = await gql(`query { getAllUsers { id name email address role createdAt } }`);
    const users = data.getAllUsers;

    const adminCount = users.filter(u => u.role === 'ADMIN').length;

    container.innerHTML = `
      <div class="admin-stats">
        <div class="stat-card"><div class="stat-value">${users.length}</div><div class="stat-label">Total Users</div></div>
        <div class="stat-card"><div class="stat-value">${adminCount}</div><div class="stat-label">Admins</div></div>
      </div>
      <table class="admin-table">
        <thead><tr><th>Name</th><th>Email</th><th>Address</th><th>Role</th><th>Actions</th></tr></thead>
        <tbody id="adminUserBody"></tbody>
      </table>
    `;

    const tbody = document.getElementById('adminUserBody');
    users.forEach(u => {
      const tr = document.createElement('tr');
      const roleBadge = u.role === 'ADMIN'
        ? '<span style="background:#f39c12;color:#fff;padding:3px 8px;border-radius:10px;font-size:11px;">👑 ADMIN</span>'
        : '<span style="background:#ddd;color:#333;padding:3px 8px;border-radius:10px;font-size:11px;">USER</span>';

      tr.innerHTML = `
        <td><strong>${escapeHtml(u.name)}</strong></td>
        <td>${escapeHtml(u.email)}</td>
        <td>${escapeHtml(u.address || '-')}</td>
        <td>${roleBadge}</td>
      `;

      const tdAct = document.createElement('td');
      const wrap = document.createElement('div');
      wrap.className = 'admin-actions';

      if (u.role !== 'ADMIN') {
        const mk = document.createElement('button');
        mk.className = 'btn-sm btn-status';
        mk.textContent = 'Make Admin';
        mk.addEventListener('click', () => makeUserAdmin(u.id));
        wrap.appendChild(mk);
      }

      const del = document.createElement('button');
      del.className = 'btn-sm btn-delete';
      del.textContent = 'Delete';
      del.addEventListener('click', () => deleteUserAdmin(u.id));
      wrap.appendChild(del);

      tdAct.appendChild(wrap);
      tr.appendChild(tdAct);
      tbody.appendChild(tr);
    });
  } catch (err) {
    container.innerHTML = `<div class="empty-msg">❌ ${escapeHtml(err.message)}</div>`;
  }
}

async function makeUserAdmin(userId) {
  if (!confirm('Make this user an Admin?')) return;
  try {
    await gql(`mutation($id: ID!) { makeAdmin(userId: $id) { message success } }`, { id: userId });
    showToast('👑 User is now Admin!');
    loadAdminUsers();
  } catch (err) {
    showToast('❌ ' + err.message, true);
  }
}

async function deleteUserAdmin(userId) {
  if (!confirm('Delete this user permanently?')) return;
  try {
    await gql(`mutation($id: ID!) { deleteUserById(id: $id) }`, { id: userId });
    showToast('🗑️ User deleted!');
    loadAdminUsers();
  } catch (err) {
    showToast('❌ ' + err.message, true);
  }
}

// ============================================================
// RESET PASSWORD
// ============================================================
function checkResetTokenInUrl() {
  const params = new URLSearchParams(window.location.search);
  const hash = window.location.hash;
  let token = params.get('token');
  if (!token && hash.startsWith('#token=')) token = hash.substring(7);

  if (token) {
    forgotToken = token;
    const modal = document.getElementById('resetModal');
    if (modal) modal.classList.remove('hidden');
    window.history.replaceState({}, document.title, window.location.pathname);
  }
}

async function submitReset() {
  const newPass = document.getElementById('resetNewPassword').value.trim();
  const confirmPass = document.getElementById('resetConfirmPassword').value.trim();
  const token = forgotToken;

  if (!newPass || newPass.length < 8) {
    showToast('Password must be at least 8 characters', true); return;
  }
  if (newPass !== confirmPass) { showToast('Passwords do not match!', true); return; }
  if (!token) { showToast('❌ No token. Please click the link from email again.', true); return; }

  try {
    const data = await gql(`
      mutation($input: ResetPasswordInput!) {
        resetPassword(resetPasswordInput: $input) { message success }
      }
    `, { input: { token, newPassword: newPass } });

    showToast('✅ ' + data.resetPassword.message);
    hide('resetModal');
    forgotToken = null;
    openLogin();
  } catch (err) {
    showToast('❌ ' + err.message, true);
  }
}

function togglePassword(inputId, btn) {
  const input = document.getElementById(inputId);
  if (!input) return;
  input.type = input.type === 'password' ? 'text' : 'password';
  btn.textContent = input.type === 'password' ? '👁️' : '🙈';
}

// ============================================================
// FORGOT PASSWORD
// ============================================================
function openForgotPassword() {
  closeLogin();
  show('forgotModal');
  document.getElementById('forgotEmail').value = '';
}

async function submitForgotPassword() {
  const email = document.getElementById('forgotEmail').value.trim();
  if (!email) { showToast('Please enter your email', true); return; }
  try {
    const data = await gql(`
      mutation($input: ForgotPasswordInput!) {
        forgotPassword(forgotPasswordInput: $input) { message success }
      }
    `, { input: { email } });
    showToast('📧 ' + data.forgotPassword.message);
    hide('forgotModal');
  } catch (err) {
    showToast('❌ ' + err.message, true);
  }
}

// ============================================================
// GLOBAL KEYBOARD — Escape closes modals
// ============================================================
document.addEventListener('keydown', e => {
  if (e.key === 'Escape') {
    ['cartModal', 'favModal', 'loginModal', 'ordersModal', 'adminModal',
      'resetModal', 'forgotModal', 'editProductModal', 'reviewsModal',
      'productDetailModal'].forEach(hide);
  }
});

// ============================================================
// INIT
// ============================================================
window.addEventListener('DOMContentLoaded', () => {
  const sb = document.getElementById('searchBox');
  if (sb) sb.value = '';
  checkResetTokenInUrl();
  updateCartBadge();
  updateFavBadge();
  updateAuthUI();
  loadProducts();

  setTimeout(initStarRating, 200);
});

// ============================================================
// WINDOW EXPORTS — Inline HTML onclick handlers
// ============================================================

// UI Helpers
window.show = show;
window.hide = hide;
window.showToast = showToast;

// Auth
window.openLogin = openLogin;
window.closeLogin = closeLogin;
window.switchAuthMode = switchAuthMode;
window.submitAuth = submitAuth;
window.logout = logout;

// Products
window.toggleForm = toggleForm;
window.addProduct = addProduct;
window.orderProduct = orderProduct;

// Cart
window.addToCart = addToCart;
window.openCart = openCart;
window.closeCart = closeCart;
window.changeQty = changeQty;
window.removeFromCart = removeFromCart;
window.checkout = checkout;

// Favorites
window.toggleFavorite = toggleFavorite;
window.openFavorites = openFavorites;
window.closeFavorites = closeFavorites;
window.addToCartFromFav = addToCartFromFav;

// Orders
window.openMyOrders = openMyOrders;
window.closeMyOrders = closeMyOrders;
window.cancelOrder = cancelOrder;

// Admin
window.openAdminPanel = openAdminPanel;
window.closeAdminPanel = closeAdminPanel;
window.switchAdminTab = switchAdminTab;
window.showAddProductForm = showAddProductForm;
window.editProduct = editProduct;
window.deleteProductAdmin = deleteProductAdmin;
window.closeEditProductModal = closeEditProductModal;
window.saveProductEdit = saveProductEdit;
window.updateOrderStatusAdmin = updateOrderStatusAdmin;
window.deleteOrderAdmin = deleteOrderAdmin;
window.makeUserAdmin = makeUserAdmin;
window.deleteUserAdmin = deleteUserAdmin;

// Password Reset & Forgot
window.openForgotPassword = openForgotPassword;
window.submitForgotPassword = submitForgotPassword;
window.submitReset = submitReset;
window.togglePassword = togglePassword;
window.checkResetTokenInUrl = checkResetTokenInUrl;

// Reviews
window.openReviewsModal = openReviewsModal;
window.closeReviewsModal = closeReviewsModal;
window.submitReview = submitReview;
window.cancelReview = cancelReview;
window.deleteReview = deleteReview;

// Product Detail
window.openProductDetail = openProductDetail;
window.closeProductDetail = closeProductDetail;
window.addToCartFromDetail = addToCartFromDetail;
window.orderFromDetail = orderFromDetail;
window.toggleFavFromDetail = toggleFavFromDetail;
window.increaseDetailQty = increaseDetailQty;
window.decreaseDetailQty = decreaseDetailQty;
window.viewReviewsFromDetail = viewReviewsFromDetail;
