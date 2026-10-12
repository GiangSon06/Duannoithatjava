// ============================================================
// SLAND — script.js  |  MOHO-style UX
// ============================================================

let cart = JSON.parse(localStorage.getItem("MY_CART")) || [];

function getCartCount() {
  return document.getElementById("cart-count");
}
function getCartItems() {
  return document.getElementById("cart-items");
}
function getCartTotal() {
  return document.getElementById("cart-total");
}
function getCartPopup() {
  return document.getElementById("cart-popup");
}

// ── INIT ──────────────────────────────────────────────────────
window.onload = function () {
  updateCartUI();
  setupSearchListener();
  initScrollReveal();
  initBannerEnhance();
  initNavScroll();
  initSmoothPageTransition();
  injectUSPBar();
  injectPromoStrip();
  injectTopbar();
  wrapProductCards();
};

// ── STICKY NAV shadow ──
function initNavScroll() {
  const header = document.querySelector("header");
  if (!header) return;
  const onScroll = () =>
    header.classList.toggle("scrolled", window.scrollY > 30);
  window.addEventListener("scroll", onScroll, { passive: true });
  onScroll();
}

// ── TOPBAR ──
function injectTopbar() {
  if (document.querySelector(".topbar")) return;
  const topbar = document.createElement("div");
  topbar.className = "topbar";
  topbar.innerHTML = `🎉 Miễn phí vận chuyển toàn quốc đơn từ 5 triệu &nbsp;|&nbsp; Bảo hành 5 năm &nbsp;|&nbsp; <a href="promotion.php">Xem khuyến mãi →</a>`;
  const body = document.body;
  const header = document.querySelector("header");
  if (header) body.insertBefore(topbar, header);
}

// ── PROMO STRIP ──
function injectPromoStrip() {
  if (document.querySelector(".promo-strip")) return;
  const banner = document.querySelector(".banner");
  if (!banner) return;
  const strip = document.createElement("div");
  strip.className = "promo-strip";
  strip.innerHTML = `<div class="promo-strip-inner"><p>🔥 SALE MÙA HÈ — Giảm đến 40% toàn bộ sản phẩm phòng khách</p><a href="promotion.php">Mua ngay →</a></div>`;
  banner.insertAdjacentElement("afterend", strip);
}

// ── USP BAR ──
function injectUSPBar() {
  if (document.querySelector(".usp-bar")) return;
  const products = document.querySelector(".products");
  if (!products) return;
  const usp = document.createElement("div");
  usp.className = "usp-bar";
  usp.innerHTML = `
    <ul class="usp-list">
      <li class="usp-item"><div class="usp-icon"><i class="fas fa-truck"></i></div><div><h4>Giao hàng toàn quốc</h4><p>Miễn phí đơn từ 5 triệu</p></div></li>
      <li class="usp-item"><div class="usp-icon"><i class="fas fa-shield-alt"></i></div><div><h4>Bảo hành 5 năm</h4><p>Cam kết chất lượng</p></div></li>
      <li class="usp-item"><div class="usp-icon"><i class="fas fa-undo"></i></div><div><h4>Đổi trả 30 ngày</h4><p>Hoàn tiền nếu không hài lòng</p></div></li>
      <li class="usp-item"><div class="usp-icon"><i class="fas fa-headset"></i></div><div><h4>Hỗ trợ 24/7</h4><p>Hotline: 0326 976 832</p></div></li>
    </ul>`;
  products.insertAdjacentElement("beforebegin", usp);
}

// ── BANNER ENHANCE ──
function initBannerEnhance() {
  const banner = document.querySelector(".banner");
  if (!banner || document.querySelector(".banner-overlay")) return;
  banner.style.position = "relative";

  const overlay = document.createElement("div");
  overlay.className = "banner-overlay";
  overlay.innerHTML = `
    <div class="banner-text">
      <div class="banner-eyebrow">Nội Thất Cao Cấp</div>
      <h1>Không gian sống<br/>tinh tế & bền vững</h1>
      <p>Sản phẩm an toàn, thiết kế hiện đại — bảo hành 5 năm chính hãng.</p>
      <div class="banner-actions">
        <a href="products.php" class="btn-banner primary">Mua ngay</a>
        <a href="about.php" class="btn-banner outline">Về SLAND</a>
      </div>
    </div>`;
  banner.appendChild(overlay);

  // Dots
  const slidesImgs = banner.querySelectorAll("img");
  if (slidesImgs.length > 1) {
    const dots = document.createElement("div");
    dots.className = "banner-dots";
    slidesImgs.forEach((_, i) => {
      const d = document.createElement("button");
      d.className = "banner-dot" + (i === 0 ? " active" : "");
      d.onclick = () => goToSlide(i);
      dots.appendChild(d);
    });
    banner.appendChild(dots);
  }
}

// ── WRAP PRODUCT CARDS (add img-wrap + card-body + overlay) ──
function wrapProductCards() {
  document.querySelectorAll(".product-card").forEach((card) => {
    if (card.querySelector(".card-body")) return;

    const img = card.querySelector("img");
    const link = card.querySelector(".product-link");

    // wrap image
    if (img && !img.closest(".img-wrap")) {
      const wrap = document.createElement("div");
      wrap.className = "img-wrap";
      const parent = img.parentNode;
      parent.insertBefore(wrap, img);
      wrap.appendChild(img);

      // overlay
      const overlay = document.createElement("div");
      overlay.className = "card-overlay";
      overlay.innerHTML = `<button class="btn-quick-add" onclick="addToCart(this.closest('.product-card').querySelector('button[onclick]') || this)">+ Thêm vào giỏ</button>`;
      wrap.appendChild(overlay);
    }

    // card-body wrap
    const body = document.createElement("div");
    body.className = "card-body";
    const children = [...card.children];
    children.forEach((child) => {
      if (
        !child.classList.contains("img-wrap") &&
        !child.classList.contains("product-badge")
      ) {
        body.appendChild(child);
      }
    });
    if (body.children.length) card.appendChild(body);
  });
}

// ── SCROLL REVEAL ──
function initScrollReveal() {
  const els = document.querySelectorAll(
    ".product-card, .review-card, .usp-item, .cat-item, .footer-col",
  );
  els.forEach((el, i) => {
    el.classList.add("reveal");
    el.style.transitionDelay = `${(i % 5) * 70}ms`;
  });
  const obs = new IntersectionObserver(
    (entries) => {
      entries.forEach((e) => {
        if (e.isIntersecting) {
          e.target.classList.add("visible");
          obs.unobserve(e.target);
        }
      });
    },
    { threshold: 0.08, rootMargin: "0px 0px -32px 0px" },
  );
  els.forEach((el) => obs.observe(el));
}

// ── SMOOTH PAGE TRANSITION ──
function initSmoothPageTransition() {
  document.body.style.transition = "opacity 0.28s ease";
  document.body.style.opacity = "0";
  requestAnimationFrame(() =>
    requestAnimationFrame(() => (document.body.style.opacity = "1")),
  );

  document.addEventListener("click", (e) => {
    const link = e.target.closest("a[href]");
    if (!link) return;
    const href = link.getAttribute("href");
    if (
      !href ||
      href.startsWith("#") ||
      href.startsWith("javascript") ||
      href.startsWith("mailto") ||
      e.ctrlKey ||
      e.metaKey
    )
      return;
    e.preventDefault();
    document.body.style.opacity = "0";
    setTimeout(() => (window.location.href = href), 260);
  });
}

// ── SEARCH ──
// ── Chuẩn hoá chuỗi: bỏ dấu, viết thường ──
// Bỏ dấu tiếng Việt → chữ thường không dấu
function normalizeText(str) {
  return (str || "")
    .normalize("NFD")
    .replace(/[\u0300-\u036f]/g, "")
    .replace(/[đĐ]/g, "d")
    .toLowerCase()
    .trim();
}

// Kiểm tra từ khoá có khớp với tên không (cả 2 chiều có dấu ↔ không dấu)
function isMatch(keyword, productName) {
  if (!keyword) return true;
  const kwNorm = normalizeText(keyword); // từ khoá bỏ dấu
  const nameNorm = normalizeText(productName); // tên bỏ dấu
  const kwOrig = keyword.toLowerCase().trim();
  const nameOrig = productName.toLowerCase().trim();
  // Khớp nếu: (kw không dấu ⊂ tên không dấu) HOẶC (kw gốc ⊂ tên gốc)
  return nameNorm.includes(kwNorm) || nameOrig.includes(kwOrig);
}

function setupSearchListener() {
  const box = document.getElementById("search-box");
  if (!box) return;

  box.addEventListener("input", function () {
    const kw = this.value.trim();
    const cards = Array.from(document.querySelectorAll(".product-card"));
    if (!cards.length) return;

    const parent = cards[0].parentElement;
    const matched = [];
    const unmatched = [];

    cards.forEach((card) => {
      const name = card.getAttribute("data-name") || "";
      if (isMatch(kw, name)) matched.push(card);
      else unmatched.push(card);
    });

    // Dồn matched lên đầu, unmatched xuống sau
    [...matched, ...unmatched].forEach((card, i) => {
      card.style.transition = `opacity 0.28s ${i * 20}ms, transform 0.28s ${i * 20}ms`;
      parent.appendChild(card);
    });

    matched.forEach((card) => {
      card.style.opacity = "1";
      card.style.transform = "";
      card.style.pointerEvents = "";
    });
    unmatched.forEach((card) => {
      card.style.opacity = kw === "" ? "1" : "0.2";
      card.style.transform = kw === "" ? "" : "scale(0.96)";
      card.style.pointerEvents = kw === "" ? "" : "none";
    });
  });
}

// ── SLIDER ──
let slideIndex = 0;
const allSlides = document.querySelectorAll(".banner img, .slides img");
function goToSlide(n) {
  slideIndex = n;
  allSlides.forEach((s, i) => (s.style.display = i === n ? "block" : "none"));
  document
    .querySelectorAll(".banner-dot")
    .forEach((d, i) => d.classList.toggle("active", i === n));
}
function showSlide() {
  slideIndex = (slideIndex + 1) % Math.max(allSlides.length, 1);
  goToSlide(slideIndex);
}
if (allSlides.length > 0) {
  goToSlide(0);
  setInterval(showSlide, 4500);
}

// ── CART ─────────────────────────────────────────────────────
function saveCart() {
  localStorage.setItem("MY_CART", JSON.stringify(cart));
}

async function addToCart(button, isBuyNow = false) {
  const container = button.closest(".product-card") || button;
  const id =
    container.getAttribute("data-id") || button.getAttribute("data-id");
  const name =
    container.getAttribute("data-name") || button.getAttribute("data-name");
  const price = parseInt(
    container.getAttribute("data-price") || button.getAttribute("data-price"),
  );
  let image =
    container.getAttribute("data-img") ||
    button.getAttribute("data-img") ||
    container.getAttribute("data-image") ||
    button.getAttribute("data-image");
  if (!image) {
    const img = container.querySelector("img");
    if (img) image = img.src;
  }
  if (!id || isNaN(price)) {
    showToast("Không tìm thấy thông tin sản phẩm", "error");
    return;
  }

  const origText = button.textContent;
  button.textContent = "Đang thêm...";
  button.disabled = true;

  const fd = new FormData();
  fd.append("id", id);
  fd.append("name", name);
  fd.append("price", price);
  fd.append("image", image);
  fd.append("quantity_add", 1);

  try {
    const res = await fetch("check_inventory.php", {
      method: "POST",
      body: fd,
    });
    const data = await res.json();
    if (data.status === "error") {
      showToast("Hết hàng: " + data.message, "error");
      button.textContent = origText;
      button.disabled = false;
      return;
    }
  } catch {
    showToast("Lỗi kết nối máy chủ", "error");
    button.textContent = origText;
    button.disabled = false;
    return;
  }

  fetch("add_to_cart.php", { method: "POST", body: fd })
    .then((r) => r.text())
    .then((totalQty) => {
      const ex = cart.find((i) => i.id === id);
      if (ex) ex.quantity++;
      else cart.push({ id, name, price, image, quantity: 1 });
      saveCart();
      updateCartUI();
      const cc = getCartCount();
      if (cc) {
        cc.innerText = totalQty;
        cc.style.display = totalQty > 0 ? "flex" : "none";
      }
      button.textContent = "✓ Đã thêm";
      button.style.background = "#E8352A";
      setTimeout(() => {
        button.textContent = origText;
        button.style.background = "";
        button.disabled = false;
      }, 1600);
      if (isBuyNow) {
        window.location.href = "checkout.php";
      } else {
        showToast(`Đã thêm "${name}" vào giỏ hàng`, "success");
        showCartPopup();
      }
    })
    .catch(() => {
      showToast("Lỗi đồng bộ giỏ hàng", "error");
      button.textContent = origText;
      button.disabled = false;
    });
}

function buyNow(button) {
  addToCart(button, true);
}

function updateCartUI() {
  const cartItems = getCartItems();
  const cartTotal = getCartTotal();
  if (!cartItems || !cartTotal) return;
  cartItems.innerHTML = "";
  let total = 0,
    totalItems = 0;
  cart.forEach((item, index) => {
    total += item.price * item.quantity;
    totalItems += item.quantity;
    const li = document.createElement("li");
    li.innerHTML = `
      <img src="${item.image}" alt="${item.name}" />
      <div class="cart-item-info">
        <strong>${item.name}</strong>
        <span>${item.price.toLocaleString("vi-VN")}đ × ${item.quantity}</span>
      </div>
      <button onclick="removeFromCart(${index})" style="
        background:transparent; border:1px solid #e5e5e5; color:#999;
        width:22px; height:22px; border-radius:50%; flex-shrink:0;
        font-size:11px; font-weight:700; display:flex; align-items:center; justify-content:center;
        transition:all 0.2s; cursor:pointer;
      " onmouseover="this.style.background='#FFF5F5';this.style.color='#E8352A';this.style.borderColor='#E8352A'"
         onmouseout="this.style.background='transparent';this.style.color='#999';this.style.borderColor='#e5e5e5'">✕</button>`;
    cartItems.appendChild(li);
  });
  cartTotal.textContent = total.toLocaleString("vi-VN");
  const cc = getCartCount();
  if (cc) {
    cc.innerText = totalItems;
    cc.style.display = totalItems > 0 ? "flex" : "none";
  }
}

function removeFromCart(index) {
  fetch("remove_from_cart.php", {
    method: "POST",
    headers: { "Content-Type": "application/x-www-form-urlencoded" },
    body: `id=${cart[index].id}`,
  })
    .then((r) => r.text())
    .then((totalQty) => {
      cart.splice(index, 1);
      saveCart();
      updateCartUI();
      const cc = getCartCount();
      if (cc) {
        cc.innerText = totalQty;
        cc.style.display = totalQty > 0 ? "flex" : "none";
      }
      showToast("Đã xóa sản phẩm khỏi giỏ", "info");
    })
    .catch(() => showToast("Lỗi xóa sản phẩm", "error"));
}

function showCartPopup() {
  const popup = getCartPopup();
  if (!popup) return;
  // wrap footer if not done
  wrapCartPopupFooter(popup);
  popup.style.display = "block";
  popup.style.animation = "slideDown 0.28s ease";
}

function wrapCartPopupFooter(popup) {
  if (popup.querySelector(".cart-popup-footer")) return;
  const totalP = popup.querySelector("p");
  const actions = popup.querySelector(".cart-actions");
  if (!totalP || !actions) return;
  const footer = document.createElement("div");
  footer.className = "cart-popup-footer";
  totalP.parentNode.insertBefore(footer, totalP);
  footer.appendChild(totalP);
  footer.appendChild(actions);
}

function toggleCart() {
  const popup = getCartPopup();
  if (!popup) return;
  if (popup.style.display === "block") {
    popup.style.opacity = "0";
    popup.style.transform = "translateY(-8px)";
    popup.style.transition = "opacity 0.2s, transform 0.2s";
    setTimeout(() => {
      popup.style.display = "none";
      popup.style.opacity = "";
      popup.style.transform = "";
    }, 200);
  } else {
    showCartPopup();
  }
}

document.addEventListener("click", (e) => {
  const popup = getCartPopup();
  const cartIcon = document.querySelector(".cart-icon");
  if (
    popup &&
    popup.style.display === "block" &&
    !popup.contains(e.target) &&
    !cartIcon?.contains(e.target)
  ) {
    popup.style.opacity = "0";
    popup.style.transition = "opacity 0.2s";
    setTimeout(() => {
      popup.style.display = "none";
      popup.style.opacity = "";
    }, 200);
  }
});

function clearCart() {
  if (!confirm("Bạn có chắc muốn xóa toàn bộ giỏ hàng?")) return;
  fetch("clear_cart.php", { method: "POST" })
    .then(() => {
      cart = [];
      saveCart();
      updateCartUI();
      const cc = getCartCount();
      if (cc) {
        cc.innerText = 0;
        cc.style.display = "none";
      }
      showToast("Đã xóa giỏ hàng", "info");
    })
    .catch(() => console.error("Lỗi xóa giỏ hàng"));
}

function checkout() {
  window.location.href = "checkout.php";
}

function filterCategory(category) {
  document.querySelectorAll(".product-card").forEach((card, i) => {
    const cat = card.getAttribute("data-category");
    const show = category === "all" || cat === category;
    card.style.transition = `opacity 0.3s ${i * 35}ms, transform 0.3s ${i * 35}ms`;
    card.style.opacity = show ? "1" : "0";
    card.style.transform = show ? "" : "translateY(10px)";
    card.style.pointerEvents = show ? "" : "none";
  });
  document
    .querySelectorAll(".cat-item")
    .forEach((b) => b.classList.remove("active"));
  if (event && event.target) event.target.classList.add("active");
}

// ── TOAST ─────────────────────────────────────────────────────
function showToast(message, type = "info") {
  let container = document.getElementById("toast-container");
  if (!container) {
    container = document.createElement("div");
    container.id = "toast-container";
    container.style.cssText =
      "position:fixed;bottom:24px;right:22px;z-index:9999;display:flex;flex-direction:column;gap:8px;pointer-events:none;";
    document.body.appendChild(container);
  }
  const cfg = {
    success: { bg: "#E8352A", icon: "✓" },
    error: { bg: "#1A1A1A", icon: "✕" },
    info: { bg: "#666666", icon: "ℹ" },
  };
  const c = cfg[type] || cfg.info;
  const toast = document.createElement("div");
  toast.style.cssText = `
    background:${c.bg}; color:white;
    padding:11px 18px; border-radius:6px;
    font-family:'Be Vietnam Pro',sans-serif; font-size:13px; font-weight:600;
    display:flex; align-items:center; gap:9px;
    box-shadow:0 8px 24px rgba(0,0,0,0.18);
    transform:translateX(110%); transition:transform 0.3s cubic-bezier(0.4,0,0.2,1),opacity 0.3s;
    max-width:300px; pointer-events:auto;
  `;
  toast.innerHTML = `<span style="font-size:15px">${c.icon}</span> ${message}`;
  container.appendChild(toast);
  requestAnimationFrame(() =>
    requestAnimationFrame(() => (toast.style.transform = "translateX(0)")),
  );
  setTimeout(() => {
    toast.style.opacity = "0";
    toast.style.transform = "translateX(110%)";
    setTimeout(() => toast.remove(), 320);
  }, 3000);
}

// inject slide-down keyframe
if (!document.getElementById("sland-kf")) {
  const st = document.createElement("style");
  st.id = "sland-kf";
  st.textContent = `@keyframes slideDown { from { opacity:0; transform:translateY(-10px); } to { opacity:1; transform:translateY(0); } }`;
  document.head.appendChild(st);
}
