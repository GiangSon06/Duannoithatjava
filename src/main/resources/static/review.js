// js/reviews.js

document.addEventListener("DOMContentLoaded", function () {
  const reviewForm = document.getElementById("reviewForm");

  if (reviewForm) {
    reviewForm.addEventListener("submit", function (e) {
      e.preventDefault(); // Chặn load lại trang

      // Lấy dữ liệu form
      const formData = new FormData(reviewForm);

      // Gửi AJAX (Fetch API)
      fetch("submit_review.php", {
        method: "POST",
        body: formData,
      })
        .then((response) => response.text())
        .then((data) => {
          if (data.trim() === "success") {
            alert("Cảm ơn bạn đã đánh giá!");
            location.reload(); // Tải lại trang để hiện đánh giá mới
          } else {
            alert(data); // Hiện lỗi nếu có
          }
        })
        .catch((error) => console.error("Error:", error));
    });
  }
});
