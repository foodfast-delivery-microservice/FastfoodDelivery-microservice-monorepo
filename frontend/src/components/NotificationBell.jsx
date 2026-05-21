import React, { useState, useEffect, useRef } from 'react';
import { useNavigate } from 'react-router-dom';
import { FaBell, FaCheckDouble, FaCheckCircle, FaExclamationTriangle, FaInfoCircle } from 'react-icons/fa';
import { useNotifications } from '../context/NotificationContext';

export default function NotificationBell() {
  const navigate = useNavigate();
  const {
    notifications,
    unreadCount,
    markNotificationRead,
    markAllNotificationsRead,
  } = useNotifications();

  const [isOpen, setIsOpen] = useState(false);
  const [shake, setShake] = useState(false);
  const prevCountRef = useRef(unreadCount);
  const dropdownRef = useRef(null);

  // Trigger shake animation when unreadCount increases
  useEffect(() => {
    if (unreadCount > prevCountRef.current) {
      setShake(true);
      const timer = setTimeout(() => setShake(false), 500);
      return () => clearTimeout(timer);
    }
    prevCountRef.current = unreadCount;
  }, [unreadCount]);

  // Close dropdown on click outside
  useEffect(() => {
    function handleClickOutside(event) {
      if (dropdownRef.current && !dropdownRef.current.contains(event.target)) {
        setIsOpen(false);
      }
    }
    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, []);

  const handleNotificationClick = async (notif) => {
    if (!notif.isRead) {
      await markNotificationRead(notif.id);
    }
    setIsOpen(false);

    // Dynamic routing based on notification type and referenceId
    if (notif.referenceId) {
      if (notif.type.includes('ORDER')) {
        navigate(`/order/${notif.referenceId}`);
      } else if (notif.type.includes('PAYMENT')) {
        navigate(`/order/${notif.referenceId}`); // payment details are shown on order details page
      }
    }
  };

  const getIcon = (type) => {
    if (type === 'WARNING' || type === 'PAYMENT_FAILED') {
      return <FaExclamationTriangle className="notification-icon" />;
    }
    if (type === 'SUCCESS' || type === 'PAYMENT_SUCCESS') {
      return <FaCheckCircle className="notification-icon" />;
    }
    return <FaInfoCircle className="notification-icon" />;
  };

  const getIconClass = (type) => {
    if (type === 'WARNING' || type === 'PAYMENT_FAILED') return 'warning';
    if (type === 'SUCCESS' || type === 'PAYMENT_SUCCESS') return 'success';
    return 'info';
  };

  const formatTime = (dateString) => {
    if (!dateString) return '';
    const date = new Date(dateString);
    const now = new Date();
    const diffMs = now - date;
    const diffMins = Math.floor(diffMs / 60000);
    const diffHours = Math.floor(diffMins / 60);

    if (diffMins < 1) return 'Vừa xong';
    if (diffMins < 60) return `${diffMins} phút trước`;
    if (diffHours < 24) return `${diffHours} giờ trước`;
    
    return date.toLocaleDateString('vi-VN', {
      day: '2-digit',
      month: '2-digit',
      year: '2-digit',
      hour: '2-digit',
      minute: '2-digit',
    });
  };

  return (
    <div className="notification-bell-container" ref={dropdownRef}>
      <button
        className={`bell-button ${shake ? 'shake-bell' : ''}`}
        onClick={() => setIsOpen(!isOpen)}
        aria-label="Thông báo"
      >
        <FaBell size={20} />
        {unreadCount > 0 && (
          <span className="bell-badge">
            {unreadCount > 99 ? '99+' : unreadCount}
          </span>
        )}
      </button>

      {isOpen && (
        <div className="bell-dropdown">
          <div className="dropdown-header">
            <h3>Thông báo mới</h3>
            {unreadCount > 0 && (
              <button className="mark-all-btn" onClick={markAllNotificationsRead}>
                <FaCheckDouble style={{ marginRight: '4px', display: 'inline' }} size={10} />
                Đọc tất cả
              </button>
            )}
          </div>

          <div className="dropdown-list">
            {notifications.length === 0 ? (
              <div className="dropdown-empty">Không có thông báo nào</div>
            ) : (
              notifications.slice(0, 10).map((notif) => (
                <div
                  key={notif.id}
                  className={`notification-item ${!notif.isRead ? 'unread' : ''}`}
                  onClick={() => handleNotificationClick(notif)}
                >
                  <div className={`notification-icon-wrapper ${getIconClass(notif.type)}`}>
                    {getIcon(notif.type)}
                  </div>
                  <div className="notification-content">
                    <div className="notification-title-row">
                      <h4 className="notification-item-title">{notif.title}</h4>
                      <span className="notification-item-time">{formatTime(notif.createdAt)}</span>
                    </div>
                    <p className="notification-item-message">{notif.message}</p>
                  </div>
                </div>
              ))
            )}
          </div>

          <div className="dropdown-footer">
            <button
              className="view-all-link"
              onClick={() => {
                setIsOpen(false);
                navigate('/notifications');
              }}
              style={{ background: 'none', border: 'none', width: '100%', cursor: 'pointer' }}
            >
              Xem tất cả thông báo
            </button>
          </div>
        </div>
      )}
    </div>
  );
}
