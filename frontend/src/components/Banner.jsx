
// src/components/Banner.jsx
import React, { useState, useEffect, useCallback } from 'react';
import './Banner.css';
function Banner({ images, interval = 2000 }) { // Mặc định chuyển sau 3 giây
  const [currentIndex, setCurrentIndex] = useState(0);

  // Dùng useCallback để tránh re-render không cần thiết
  const goToNext = useCallback(() => {
    setCurrentIndex((prevIndex) => (prevIndex + 1) % images.length);
  }, [images.length]);

  const goToPrevious = () => {
    setCurrentIndex((prevIndex) => (prevIndex - 1 + images.length) % images.length);
  };

  // useEffect để tự động chuyển banner
  useEffect(() => {
    const timer = setInterval(goToNext, interval);
    
    // Cleanup: Xóa timer khi component bị unmount
    return () => clearInterval(timer);
  }, [goToNext, interval]); // Chạy lại effect khi các giá trị này thay đổi

  if (!images || images.length === 0) {
    return null; // Không hiển thị gì nếu không có ảnh
  }

  return (
    <div className="banner-container">
      {/* Nút chuyển về banner trước */}
      <button onClick={goToPrevious} className="banner-arrow left-arrow">
        ❮
      </button>
      
      {/* Ảnh banner chính */}
      <img 
        src={images[currentIndex]} 
        alt={`Banner ${currentIndex + 1}`} 
        className="banner-image"
      />

      {/* Nút chuyển tới banner kế tiếp */}
      <button onClick={goToNext} className="banner-arrow right-arrow">
        ❯
      </button>

      {/* Các dấu chấm chỉ vị trí banner */}
      <div className="dots-container">
        {images.map((_, index) => (
          <div
            key={index}
            className={`dot ${currentIndex === index ? 'active' : ''}`}
            onClick={() => setCurrentIndex(index)}
          ></div>
        ))}
      </div>
    </div>
  );
}

export default Banner;